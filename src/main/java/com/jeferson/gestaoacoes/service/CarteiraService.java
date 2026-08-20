package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.HistoricoResponseDTO;
import com.jeferson.gestaoacoes.dto.PosicaoResponseDTO;
import com.jeferson.gestaoacoes.dto.TransacaoRequestDTO;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.model.*;
import com.jeferson.gestaoacoes.repository.AcaoRepository;
import com.jeferson.gestaoacoes.repository.CorretoraRepository;
import com.jeferson.gestaoacoes.repository.PosicaoRepository;
import com.jeferson.gestaoacoes.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class CarteiraService {

    private final PosicaoRepository posicaoRepository;
    private final TransacaoRepository transacaoRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;

    public CarteiraService(PosicaoRepository posicaoRepository, TransacaoRepository transacaoRepository,
                           AcaoRepository acaoRepository, CorretoraRepository corretoraRepository) {
        this.posicaoRepository = posicaoRepository;
        this.transacaoRepository = transacaoRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
    }

    @Transactional
    public void registrarCompra(TransacaoRequestDTO dto) {
        Acao acao = buscarAcao(dto.acaoId());
        Corretora corretora = buscarCorretora(dto.corretoraId());

        // Pega o preço exato que o usuário digitou na boleta (Front-end)
        BigDecimal precoCompra = dto.valorUnitario();

        salvarTransacao(acao, corretora, TipoTransacao.COMPRA, dto.quantidade(), precoCompra);

        Posicao posicao = posicaoRepository.findByAcaoId(acao.getId()).orElse(new Posicao());

        // REGRA DA RECEITA FEDERAL: Média Ponderada nas Compras
        if (posicao.getId() == null || posicao.getQuantidade() == 0) {
            posicao.setAcao(acao);
            posicao.setQuantidade(dto.quantidade());
            posicao.setPrecoMedio(precoCompra);
        } else {
            BigDecimal financeiroAntigo = posicao.getPrecoMedio().multiply(new BigDecimal(posicao.getQuantidade()));
            BigDecimal financeiroNovo = precoCompra.multiply(new BigDecimal(dto.quantidade()));

            Integer quantidadeTotal = posicao.getQuantidade() + dto.quantidade();

            // Calcula o novo Preço Médio (Financeiro Total / Quantidade Total)
            BigDecimal novoPrecoMedio = (financeiroAntigo.add(financeiroNovo))
                    .divide(new BigDecimal(quantidadeTotal), 4, RoundingMode.HALF_UP);

            posicao.setQuantidade(quantidadeTotal);
            posicao.setPrecoMedio(novoPrecoMedio);
        }
        posicaoRepository.save(posicao);
    }

    @Transactional
    public void registrarVenda(TransacaoRequestDTO dto) {
        Acao acao = buscarAcao(dto.acaoId());
        Corretora corretora = buscarCorretora(dto.corretoraId());
        BigDecimal precoVenda = dto.valorUnitario();

        Posicao posicao = posicaoRepository.findByAcaoId(acao.getId())
                .orElseThrow(() -> new RegraNegocioException("Você não possui posição nesta ação para vender."));

        if (posicao.getQuantidade() < dto.quantidade()) {
            throw new RegraNegocioException("Quantidade insuficiente. Você possui apenas " + posicao.getQuantidade() + " ações.");
        }

        // REGRA DA RECEITA FEDERAL: Venda gera Apuração de Resultado (Lucro/Prejuízo Realizado)
        // O Preço Médio da posição NÃO SE ALTERA durante a venda.
        BigDecimal precoMedioDeCusto = posicao.getPrecoMedio();

        // Exemplo para falar na apresentação: "Calculamos o lucro subtraindo o custo médio do preço de venda"
        BigDecimal lucroOuPrejuizoPorAcao = precoVenda.subtract(precoMedioDeCusto);
        BigDecimal resultadoFinanceiroDaOperacao = lucroOuPrejuizoPorAcao.multiply(new BigDecimal(dto.quantidade()));

        salvarTransacao(acao, corretora, TipoTransacao.VENDA, dto.quantidade(), precoVenda);

        // Atualiza o saldo de ações na carteira
        posicao.setQuantidade(posicao.getQuantidade() - dto.quantidade());

        // Se vendeu tudo, o preço médio zera para não impactar recompras futuras
        if (posicao.getQuantidade() == 0) {
            posicao.setPrecoMedio(BigDecimal.ZERO);
        }

        posicaoRepository.save(posicao);

        // Nota: O 'resultadoFinanceiroDaOperacao' poderia ser salvo no banco para o relatório de DARF (Imposto de Renda).
    }

    public List<PosicaoResponseDTO> listarPosicoes() {
        return posicaoRepository.findAll().stream()
                .filter(p -> p.getQuantidade() > 0)
                .map(p -> {
                    BigDecimal cotacaoAtual = p.getAcao().getCotacaoAtual();
                    BigDecimal precoMedio = p.getPrecoMedio();

                    BigDecimal rentabilidade = BigDecimal.ZERO;
                    if (precoMedio.compareTo(BigDecimal.ZERO) > 0) {
                        rentabilidade = cotacaoAtual.divide(precoMedio, 4, RoundingMode.HALF_UP)
                                .subtract(BigDecimal.ONE)
                                .multiply(new BigDecimal("100"));
                    }

                    BigDecimal saldoTotalAtual = cotacaoAtual.multiply(new BigDecimal(p.getQuantidade()));

                    return new PosicaoResponseDTO(
                            p.getAcao().getTicker(),
                            p.getAcao().getNomeEmpresa(),
                            p.getQuantidade(),
                            precoMedio,
                            cotacaoAtual,
                            rentabilidade,
                            saldoTotalAtual
                    );
                }).toList();
    }

    private void salvarTransacao(Acao acao, Corretora corretora, TipoTransacao tipo, Integer quantidade, BigDecimal valorUnitario) {
        Transacao transacao = new Transacao();
        transacao.setAcao(acao);
        transacao.setCorretora(corretora);
        transacao.setTipoTransacao(tipo);
        transacao.setQuantidade(quantidade);
        transacao.setValorUnitario(valorUnitario);
        transacao.setDataHoraTransacao(OffsetDateTime.now(ZoneOffset.UTC));
        transacaoRepository.save(transacao);
    }

    private Acao buscarAcao(Long id) {
        return acaoRepository.findById(id).orElseThrow(() -> new RegraNegocioException("Ação não encontrada."));
    }

    private Corretora buscarCorretora(Long id) {
        return corretoraRepository.findById(id).orElseThrow(() -> new RegraNegocioException("Corretora não encontrada."));
    }
    // Importe java.util.Comparator se pedir!
    public List<HistoricoResponseDTO> listarHistorico() {
        return transacaoRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Transacao::getDataHoraTransacao).reversed())
                .map(t -> new HistoricoResponseDTO(
                        t.getTipoTransacao().name(),
                        t.getAcao().getTicker(),
                        t.getCorretora().getCnpj(),
                        t.getQuantidade(),
                        t.getValorUnitario(),
                        t.getValorUnitario().multiply(new BigDecimal(t.getQuantidade())),
                        t.getDataHoraTransacao()
                )).toList();
    }
}