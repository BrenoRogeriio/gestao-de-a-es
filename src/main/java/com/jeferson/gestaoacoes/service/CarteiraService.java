package com.jeferson.gestaoacoes.service;

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

        // Pega o preço REAL da bolsa neste exato momento para executar a ordem
        BigDecimal precoExecucao = acao.getCotacaoAtual();

        // 1. Salva o Histórico
        salvarTransacao(acao, corretora, TipoTransacao.COMPRA, dto.quantidade(), precoExecucao);

        // 2. Atualiza a Posição (Preço Médio)
        Posicao posicao = posicaoRepository.findByAcaoId(acao.getId()).orElse(new Posicao());
        if (posicao.getId() == null) {
            posicao.setAcao(acao);
            posicao.setQuantidade(dto.quantidade());
            posicao.setPrecoMedio(precoExecucao);
        } else {
            BigDecimal totalAntigo = posicao.getPrecoMedio().multiply(new BigDecimal(posicao.getQuantidade()));
            BigDecimal totalNovo = precoExecucao.multiply(new BigDecimal(dto.quantidade()));

            Integer novaQuantidade = posicao.getQuantidade() + dto.quantidade();
            BigDecimal novoPrecoMedio = (totalAntigo.add(totalNovo)).divide(new BigDecimal(novaQuantidade), 4, RoundingMode.HALF_UP);

            posicao.setQuantidade(novaQuantidade);
            posicao.setPrecoMedio(novoPrecoMedio);
        }
        posicaoRepository.save(posicao);
    }

    @Transactional
    public void registrarVenda(TransacaoRequestDTO dto) {
        Acao acao = buscarAcao(dto.acaoId());
        Corretora corretora = buscarCorretora(dto.corretoraId());

        // Preço REAL da bolsa para a venda
        BigDecimal precoExecucao = acao.getCotacaoAtual();

        Posicao posicao = posicaoRepository.findByAcaoId(acao.getId())
                .orElseThrow(() -> new RegraNegocioException("Você não possui posição nesta ação para vender."));

        if (posicao.getQuantidade() < dto.quantidade()) {
            throw new RegraNegocioException("Quantidade insuficiente. Você possui apenas " + posicao.getQuantidade() + " ações de " + acao.getTicker());
        }

        // 1. Salva o Histórico
        salvarTransacao(acao, corretora, TipoTransacao.VENDA, dto.quantidade(), precoExecucao);

        // 2. Atualiza a Posição
        posicao.setQuantidade(posicao.getQuantidade() - dto.quantidade());

        if (posicao.getQuantidade() == 0) {
            posicao.setPrecoMedio(BigDecimal.ZERO);
        }
        posicaoRepository.save(posicao);
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
}