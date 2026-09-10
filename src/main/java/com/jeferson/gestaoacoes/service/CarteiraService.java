package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.HistoricoResponseDTO;
import com.jeferson.gestaoacoes.dto.PosicaoResponseDTO;
import com.jeferson.gestaoacoes.dto.ResumoCarteiraResponseDTO;
import com.jeferson.gestaoacoes.dto.TransacaoRequestDTO;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.model.*;
import com.jeferson.gestaoacoes.repository.AcaoRepository;
import com.jeferson.gestaoacoes.repository.CorretoraRepository;
import com.jeferson.gestaoacoes.repository.PosicaoRepository;
import com.jeferson.gestaoacoes.repository.ResultadoRealizadoPorMoeda;
import com.jeferson.gestaoacoes.repository.TransacaoRepository;
import com.jeferson.gestaoacoes.security.UsuarioAtualService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
public class CarteiraService {

    private static final int ESCALA_MONETARIA = 4;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal ZERO_MONETARIO = BigDecimal.ZERO.setScale(ESCALA_MONETARIA);
    private static final BigDecimal VALOR_UNITARIO_MAXIMO = new BigDecimal("999999999999999.9999");
    private static final ZoneId FUSO_TRANSACAO = ZoneId.of("America/Sao_Paulo");

    private final PosicaoRepository posicaoRepository;
    private final TransacaoRepository transacaoRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final UsuarioAtualService usuarioAtualService;

    public CarteiraService(PosicaoRepository posicaoRepository, TransacaoRepository transacaoRepository,
                           AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
                           UsuarioAtualService usuarioAtualService) {
        this.posicaoRepository = posicaoRepository;
        this.transacaoRepository = transacaoRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.usuarioAtualService = usuarioAtualService;
    }

    @Transactional
    public void registrarCompra(TransacaoRequestDTO dto) {
        registrarCompra(dto, null);
    }

    @Transactional
    public void registrarCompra(TransacaoRequestDTO dto, String idempotencyKey) {
        validarDadosTransacao(dto);
        String chaveNormalizada = validarChaveIdempotencia(idempotencyKey);
        Usuario usuario = usuarioAtualService.obterReferencia();

        Acao acao = buscarAcaoComBloqueio(dto.acaoId());
        Corretora corretora = buscarCorretora(dto.corretoraId());
        if (requisicaoJaProcessada(usuario.getId(), chaveNormalizada, TipoTransacao.COMPRA, dto, acao, corretora)) {
            return;
        }

        // Pega o preço exato que o usuário digitou na boleta (Front-end)
        BigDecimal precoCompra = dto.valorUnitario();

        Posicao posicao = posicaoRepository.findByUsuarioIdAndAcaoId(usuario.getId(), acao.getId())
                .orElseGet(() -> novaPosicao(usuario, acao));

        // REGRA DA RECEITA FEDERAL: Média Ponderada nas Compras
        if (posicao.getId() == null || posicao.getQuantidade() == 0) {
            posicao.setQuantidade(dto.quantidade());
            posicao.setPrecoMedio(valorMonetario(precoCompra));
        } else {
            BigDecimal financeiroAntigo = posicao.getPrecoMedio().multiply(BigDecimal.valueOf(posicao.getQuantidade()));
            BigDecimal financeiroNovo = precoCompra.multiply(BigDecimal.valueOf(dto.quantidade()));

            int quantidadeTotal;
            try {
                quantidadeTotal = Math.addExact(posicao.getQuantidade(), dto.quantidade());
            } catch (ArithmeticException e) {
                throw new RegraNegocioException("A quantidade total da posição excede o limite permitido.");
            }

            // Calcula o novo Preço Médio (Financeiro Total / Quantidade Total)
            BigDecimal novoPrecoMedio = (financeiroAntigo.add(financeiroNovo))
                    .divide(BigDecimal.valueOf(quantidadeTotal), 4, RoundingMode.HALF_UP);

            posicao.setQuantidade(quantidadeTotal);
            posicao.setPrecoMedio(novoPrecoMedio);
        }

        salvarTransacao(usuario, acao, corretora, TipoTransacao.COMPRA, dto.quantidade(), precoCompra,
                dataHoraTransacao(dto.data()), chaveNormalizada);
        posicaoRepository.save(posicao);
    }

    @Transactional
    public void registrarVenda(TransacaoRequestDTO dto) {
        registrarVenda(dto, null);
    }

    @Transactional
    public void registrarVenda(TransacaoRequestDTO dto, String idempotencyKey) {
        validarDadosTransacao(dto);
        String chaveNormalizada = validarChaveIdempotencia(idempotencyKey);
        Usuario usuario = usuarioAtualService.obterReferencia();

        Acao acao = buscarAcaoComBloqueio(dto.acaoId());
        Corretora corretora = buscarCorretora(dto.corretoraId());
        if (requisicaoJaProcessada(usuario.getId(), chaveNormalizada, TipoTransacao.VENDA, dto, acao, corretora)) {
            return;
        }
        BigDecimal precoVenda = dto.valorUnitario();

        Posicao posicao = posicaoRepository.findByUsuarioIdAndAcaoId(usuario.getId(), acao.getId())
                .orElseThrow(() -> new RegraNegocioException("Você não possui posição nesta ação para vender."));

        if (posicao.getQuantidade() < dto.quantidade()) {
            throw new RegraNegocioException("Quantidade insuficiente. Você possui apenas " + posicao.getQuantidade() + " ações.");
        }

        // REGRA DA RECEITA FEDERAL: Venda gera Apuração de Resultado (Lucro/Prejuízo Realizado)
        // O Preço Médio da posição NÃO SE ALTERA durante a venda.
        BigDecimal precoMedioDeCusto = posicao.getPrecoMedio();

        // Exemplo para falar na apresentação: "Calculamos o lucro subtraindo o custo médio do preço de venda"
        BigDecimal lucroOuPrejuizoPorAcao = precoVenda.subtract(precoMedioDeCusto);
        BigDecimal resultadoFinanceiroDaOperacao = valorMonetario(
                lucroOuPrejuizoPorAcao.multiply(BigDecimal.valueOf(dto.quantidade())));

        salvarTransacao(usuario, acao, corretora, TipoTransacao.VENDA, dto.quantidade(), precoVenda,
                dataHoraTransacao(dto.data()), chaveNormalizada,
                valorMonetario(precoMedioDeCusto), resultadoFinanceiroDaOperacao);

        // Atualiza o saldo de ações na carteira
        posicao.setQuantidade(Math.subtractExact(posicao.getQuantidade(), dto.quantidade()));

        // Se vendeu tudo, o preço médio zera para não impactar recompras futuras
        if (posicao.getQuantidade() == 0) {
            posicao.setPrecoMedio(ZERO_MONETARIO);
        }

        posicaoRepository.save(posicao);
    }

    @Transactional(readOnly = true)
    public List<PosicaoResponseDTO> listarPosicoes() {
        return listarPosicoesDoUsuario(usuarioAtualService.obterId());
    }

    @Transactional(readOnly = true)
    public List<ResumoCarteiraResponseDTO> resumirCarteiraPorMoeda() {
        Long usuarioId = usuarioAtualService.obterId();
        Map<Moeda, List<PosicaoResponseDTO>> posicoesPorMoeda = new EnumMap<>(Moeda.class);
        listarPosicoesDoUsuario(usuarioId).forEach(posicao -> posicoesPorMoeda
                .computeIfAbsent(posicao.moeda(), moeda -> new java.util.ArrayList<>())
                .add(posicao));

        Map<Moeda, BigDecimal> resultadoRealizadoPorMoeda = new EnumMap<>(Moeda.class);
        transacaoRepository.somarResultadoRealizadoPorMoeda(usuarioId).forEach(resultado ->
                resultadoRealizadoPorMoeda.put(resultado.moeda(), valorMonetario(resultado.valor())));

        EnumSet<Moeda> moedas = EnumSet.noneOf(Moeda.class);
        moedas.addAll(posicoesPorMoeda.keySet());
        moedas.addAll(resultadoRealizadoPorMoeda.keySet());

        return moedas.stream()
                .map(moeda -> resumirMoeda(
                        moeda,
                        posicoesPorMoeda.getOrDefault(moeda, List.of()),
                        resultadoRealizadoPorMoeda.getOrDefault(moeda, ZERO_MONETARIO)))
                .toList();
    }

    private ResumoCarteiraResponseDTO resumirMoeda(Moeda moeda, List<PosicaoResponseDTO> posicoes,
                                                    BigDecimal resultadoRealizadoTotal) {
        BigDecimal valorInvestidoTotal = posicoes.stream()
                .map(PosicaoResponseDTO::valorInvestido)
                .reduce(ZERO_MONETARIO, BigDecimal::add);
        boolean todasCotacoesDisponiveis = posicoes.stream()
                .allMatch(posicao -> posicao.valorAtual() != null);

        if (!todasCotacoesDisponiveis) {
            return new ResumoCarteiraResponseDTO(
                    moeda, valorMonetario(valorInvestidoTotal), null, null, null,
                    resultadoRealizadoTotal, null);
        }

        BigDecimal valorAtualTotal = posicoes.stream()
                .map(PosicaoResponseDTO::valorAtual)
                .reduce(ZERO_MONETARIO, BigDecimal::add);
        BigDecimal resultadoNaoRealizadoTotal = valorMonetario(valorAtualTotal.subtract(valorInvestidoTotal));
        return new ResumoCarteiraResponseDTO(
                moeda,
                valorMonetario(valorInvestidoTotal),
                valorMonetario(valorAtualTotal),
                resultadoNaoRealizadoTotal,
                calcularRentabilidade(resultadoNaoRealizadoTotal, valorInvestidoTotal),
                resultadoRealizadoTotal,
                valorMonetario(resultadoRealizadoTotal.add(resultadoNaoRealizadoTotal))
        );
    }

    private void salvarTransacao(Usuario usuario, Acao acao, Corretora corretora, TipoTransacao tipo, Integer quantidade,
                                 BigDecimal valorUnitario, OffsetDateTime dataHoraTransacao,
                                 String idempotencyKey) {
        salvarTransacao(usuario, acao, corretora, tipo, quantidade, valorUnitario, dataHoraTransacao,
                idempotencyKey, null, null);
    }

    private void salvarTransacao(Usuario usuario, Acao acao, Corretora corretora, TipoTransacao tipo, Integer quantidade,
                                 BigDecimal valorUnitario, OffsetDateTime dataHoraTransacao,
                                 String idempotencyKey, BigDecimal precoMedioOperacao,
                                 BigDecimal resultadoRealizado) {
        Transacao transacao = new Transacao();
        transacao.setUsuario(usuario);
        transacao.setAcao(acao);
        transacao.setCorretora(corretora);
        transacao.setTipoTransacao(tipo);
        transacao.setQuantidade(quantidade);
        transacao.setValorUnitario(valorUnitario);
        transacao.setPrecoMedioOperacao(precoMedioOperacao);
        transacao.setResultadoRealizado(resultadoRealizado);
        transacao.setDataHoraTransacao(dataHoraTransacao);
        transacao.setIdempotencyKey(idempotencyKey);
        try {
            transacaoRepository.saveAndFlush(transacao);
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey != null) {
                throw new RegraNegocioException("A chave de idempotência já foi utilizada em outra operação.");
            }
            throw e;
        }
    }

    private BigDecimal valorMonetario(BigDecimal valor) {
        return valor.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularRentabilidade(BigDecimal resultado, BigDecimal valorInvestido) {
        if (valorInvestido == null || valorInvestido.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO_MONETARIO;
        }
        return resultado.multiply(CEM)
                .divide(valorInvestido, ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularRentabilidadeRealizada(Transacao transacao) {
        BigDecimal precoMedio = transacao.getPrecoMedioOperacao();
        if (transacao.getTipoTransacao() != TipoTransacao.VENDA
                || precoMedio == null
                || precoMedio.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return transacao.getValorUnitario().subtract(precoMedio)
                .multiply(CEM)
                .divide(precoMedio, ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private OffsetDateTime dataHoraTransacao(LocalDate data) {
        if (data == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return data.atStartOfDay(FUSO_TRANSACAO).toOffsetDateTime();
    }

    private LocalDate dataOperacao(Transacao transacao) {
        return transacao.getDataHoraTransacao().atZoneSameInstant(FUSO_TRANSACAO).toLocalDate();
    }

    private String validarChaveIdempotencia(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }

        String chaveNormalizada = idempotencyKey.trim();
        if (chaveNormalizada.isEmpty()) {
            throw new RegraNegocioException("A chave de idempotência não pode estar vazia.");
        }
        if (chaveNormalizada.length() > 100) {
            throw new RegraNegocioException("A chave de idempotência deve ter no máximo 100 caracteres.");
        }
        return chaveNormalizada;
    }

    private boolean requisicaoJaProcessada(Long usuarioId, String idempotencyKey, TipoTransacao tipo,
                                            TransacaoRequestDTO dto, Acao acao, Corretora corretora) {
        if (idempotencyKey == null) {
            return false;
        }

        return transacaoRepository.findByUsuarioIdAndIdempotencyKey(usuarioId, idempotencyKey)
                .map(transacao -> {
                    boolean mesmaOperacao = transacao.getTipoTransacao() == tipo
                            && transacao.getAcao().getId().equals(acao.getId())
                            && transacao.getCorretora().getId().equals(corretora.getId())
                            && transacao.getQuantidade().equals(dto.quantidade())
                            && transacao.getValorUnitario().compareTo(dto.valorUnitario()) == 0
                            && (dto.data() == null || dataOperacao(transacao).equals(dto.data()));
                    if (!mesmaOperacao) {
                        throw new RegraNegocioException("A chave de idempotência já foi utilizada em outra operação.");
                    }
                    return true;
                })
                .orElse(false);
    }

    private void validarDadosTransacao(TransacaoRequestDTO dto) {
        if (dto.quantidade() == null) {
            throw new RegraNegocioException("A quantidade é obrigatória.");
        }
        if (dto.quantidade() <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }

        BigDecimal valorUnitario = dto.valorUnitario();
        if (valorUnitario == null) {
            throw new RegraNegocioException("O preço de execução é obrigatório.");
        }
        if (valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O preço de execução deve ser maior que zero.");
        }

        if (valorUnitario.scale() > ESCALA_MONETARIA) {
            throw new RegraNegocioException("O preço de execução deve ter no máximo 4 casas decimais.");
        }
        if (valorUnitario.compareTo(VALOR_UNITARIO_MAXIMO) > 0) {
            throw new RegraNegocioException("O preço de execução deve ter no máximo 15 dígitos inteiros.");
        }
    }

    private Acao buscarAcaoComBloqueio(Long id) {
        return acaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RegraNegocioException("Ação não encontrada."));
    }

    private Corretora buscarCorretora(Long id) {
        return corretoraRepository.findById(id).orElseThrow(() -> new RegraNegocioException("Corretora não encontrada."));
    }
    @Transactional(readOnly = true)
    public List<HistoricoResponseDTO> listarHistorico() {
        Long usuarioId = usuarioAtualService.obterId();
        return transacaoRepository.findAllByUsuarioIdOrderByDataHoraTransacaoDesc(usuarioId).stream()
                .sorted(java.util.Comparator.comparing(Transacao::getDataHoraTransacao).reversed())
                .map(t -> new HistoricoResponseDTO(
                        t.getAcao().getId(),
                        t.getTipoTransacao().name(),
                        t.getAcao().getTicker(),
                        t.getAcao().getMercado(),
                        t.getAcao().getMoeda(),
                        t.getCorretora().getCnpj(),
                        t.getQuantidade(),
                        t.getValorUnitario(),
                        t.getValorUnitario().multiply(BigDecimal.valueOf(t.getQuantidade())),
                        t.getPrecoMedioOperacao(),
                        t.getResultadoRealizado(),
                        calcularRentabilidadeRealizada(t),
                        t.getDataHoraTransacao(),
                        dataOperacao(t)
                )).toList();
    }

    private Posicao novaPosicao(Usuario usuario, Acao acao) {
        Posicao posicao = new Posicao();
        posicao.setUsuario(usuario);
        posicao.setAcao(acao);
        return posicao;
    }

    private List<PosicaoResponseDTO> listarPosicoesDoUsuario(Long usuarioId) {
        return posicaoRepository.findAllByUsuarioIdAndQuantidadeGreaterThan(usuarioId, 0).stream()
                .filter(posicao -> posicao.getQuantidade() > 0)
                .map(this::mapearPosicao)
                .toList();
    }

    private PosicaoResponseDTO mapearPosicao(Posicao p) {
        BigDecimal cotacaoAtual = p.getAcao().getCotacaoAtual();
        BigDecimal precoMedio = p.getPrecoMedio();
        BigDecimal valorInvestido = valorMonetario(
                precoMedio.multiply(BigDecimal.valueOf(p.getQuantidade())));
        BigDecimal valorAtual = cotacaoAtual == null ? null : valorMonetario(
                cotacaoAtual.multiply(BigDecimal.valueOf(p.getQuantidade())));
        BigDecimal resultadoNaoRealizado = valorAtual == null ? null
                : valorMonetario(valorAtual.subtract(valorInvestido));
        BigDecimal rentabilidade = valorAtual == null ? null
                : calcularRentabilidade(resultadoNaoRealizado, valorInvestido);

        return new PosicaoResponseDTO(
                p.getAcao().getId(), p.getAcao().getTicker(), p.getAcao().getNomeEmpresa(),
                p.getAcao().getMercado(), p.getAcao().getMoeda(), p.getQuantidade(), precoMedio,
                cotacaoAtual, valorInvestido, valorAtual, resultadoNaoRealizado, rentabilidade, valorAtual);
    }
}
