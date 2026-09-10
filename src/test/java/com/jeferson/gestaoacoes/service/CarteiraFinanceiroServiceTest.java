package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.TransacaoRequestDTO;
import com.jeferson.gestaoacoes.model.Acao;
import com.jeferson.gestaoacoes.model.Corretora;
import com.jeferson.gestaoacoes.model.Mercado;
import com.jeferson.gestaoacoes.model.Moeda;
import com.jeferson.gestaoacoes.model.Posicao;
import com.jeferson.gestaoacoes.model.TipoTransacao;
import com.jeferson.gestaoacoes.model.Transacao;
import com.jeferson.gestaoacoes.repository.AcaoRepository;
import com.jeferson.gestaoacoes.repository.CorretoraRepository;
import com.jeferson.gestaoacoes.repository.PosicaoRepository;
import com.jeferson.gestaoacoes.repository.ResultadoRealizadoPorMoeda;
import com.jeferson.gestaoacoes.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarteiraFinanceiroServiceTest {

    @Mock
    private PosicaoRepository posicaoRepository;
    @Mock
    private TransacaoRepository transacaoRepository;
    @Mock
    private AcaoRepository acaoRepository;
    @Mock
    private CorretoraRepository corretoraRepository;

    @InjectMocks
    private CarteiraService service;

    private Acao acao;
    private Corretora corretora;

    @BeforeEach
    void prepararEntidades() {
        acao = acao(1L, "PETR4", Mercado.BRASIL, Moeda.BRL, "120.0000");
        corretora = new Corretora();
        corretora.setId(2L);
        corretora.setCnpj("02332886000104");
    }

    @Test
    void deveCalcularMediaPonderadaNaSegundaCompra() {
        prepararReferencias();
        Posicao posicao = posicao(acao, 10, "100.0000");
        when(posicaoRepository.findByAcaoId(acao.getId())).thenReturn(Optional.of(posicao));

        service.registrarCompra(dto(5, "120.0000"));

        assertEquals(15, posicao.getQuantidade());
        assertDecimal("106.6667", posicao.getPrecoMedio());
    }

    @Test
    void deveCalcularMediaPonderadaEmVariasCompras() {
        prepararReferencias();
        AtomicReference<Posicao> estado = simularPersistenciaDaPosicao();

        service.registrarCompra(dto(10, "100.0000"));
        service.registrarCompra(dto(5, "120.0000"));
        service.registrarCompra(dto(15, "110.0000"));

        assertEquals(30, estado.get().getQuantidade());
        assertDecimal("108.3334", estado.get().getPrecoMedio());
    }

    @Test
    void devePreservarQuatroCasasComQuantidadeGrande() {
        prepararReferencias();
        Posicao posicao = posicao(acao, 1_000_000_000, "10.1234");
        when(posicaoRepository.findByAcaoId(acao.getId())).thenReturn(Optional.of(posicao));

        service.registrarCompra(dto(1_000_000_000, "10.1235"));

        assertEquals(2_000_000_000, posicao.getQuantidade());
        assertDecimal("10.1235", posicao.getPrecoMedio());
        assertEquals(4, posicao.getPrecoMedio().scale());
    }

    @Test
    void deveManterPrecoMedioERegistrarLucroNaVendaParcial() {
        prepararReferencias();
        Posicao posicao = posicao(acao, 15, "106.6667");
        when(posicaoRepository.findByAcaoId(acao.getId())).thenReturn(Optional.of(posicao));

        service.registrarVenda(dto(4, "130.0000"));

        ArgumentCaptor<Transacao> captor = ArgumentCaptor.forClass(Transacao.class);
        verify(transacaoRepository).saveAndFlush(captor.capture());
        assertEquals(11, posicao.getQuantidade());
        assertDecimal("106.6667", posicao.getPrecoMedio());
        assertDecimal("106.6667", captor.getValue().getPrecoMedioOperacao());
        assertDecimal("93.3332", captor.getValue().getResultadoRealizado());
    }

    @Test
    void deveRegistrarPrejuizoRealizado() {
        prepararReferencias();
        Posicao posicao = posicao(acao, 10, "100.0000");
        when(posicaoRepository.findByAcaoId(acao.getId())).thenReturn(Optional.of(posicao));

        service.registrarVenda(dto(3, "80.0000"));

        ArgumentCaptor<Transacao> captor = ArgumentCaptor.forClass(Transacao.class);
        verify(transacaoRepository).saveAndFlush(captor.capture());
        assertDecimal("-60.0000", captor.getValue().getResultadoRealizado());
    }

    @Test
    void deveZerarQuantidadeEPrecoMedioNaVendaTotal() {
        prepararReferencias();
        Posicao posicao = posicao(acao, 10, "100.0000");
        when(posicaoRepository.findByAcaoId(acao.getId())).thenReturn(Optional.of(posicao));

        service.registrarVenda(dto(10, "120.0000"));

        assertEquals(0, posicao.getQuantidade());
        assertDecimal("0.0000", posicao.getPrecoMedio());
    }

    @Test
    void deveCalcularResultadoNaoRealizadoPositivoERentabilidade() {
        when(posicaoRepository.findAll()).thenReturn(List.of(posicao(acao, 15, "106.6667")));

        var resposta = service.listarPosicoes().getFirst();

        assertDecimal("1600.0005", resposta.valorInvestido());
        assertDecimal("1800.0000", resposta.valorAtual());
        assertDecimal("199.9995", resposta.resultadoNaoRealizado());
        assertDecimal("12.5000", resposta.rentabilidadePercentual());
        assertDecimal("1800.0000", resposta.saldoTotalAtual());
    }

    @Test
    void deveCalcularResultadoNaoRealizadoNegativo() {
        acao.setCotacaoAtual(new BigDecimal("80.0000"));
        when(posicaoRepository.findAll()).thenReturn(List.of(posicao(acao, 10, "100.0000")));

        var resposta = service.listarPosicoes().getFirst();

        assertDecimal("-200.0000", resposta.resultadoNaoRealizado());
        assertDecimal("-20.0000", resposta.rentabilidadePercentual());
    }

    @Test
    void deveIgnorarPosicaoZeradaETratarCotacaoAusente() {
        Posicao zerada = posicao(acao, 0, "0.0000");
        Acao semCotacao = acao(2L, "VALE3", Mercado.BRASIL, Moeda.BRL, null);
        Posicao abertaSemCotacao = posicao(semCotacao, 2, "50.0000");
        when(posicaoRepository.findAll()).thenReturn(List.of(zerada, abertaSemCotacao));

        var resposta = service.listarPosicoes();

        assertEquals(1, resposta.size());
        assertDecimal("100.0000", resposta.getFirst().valorInvestido());
        assertNull(resposta.getFirst().valorAtual());
        assertNull(resposta.getFirst().resultadoNaoRealizado());
        assertNull(resposta.getFirst().rentabilidadePercentual());
    }

    @Test
    void deveConsolidarBrlEUsdSeparadamente() {
        Acao acaoBrl = acao(1L, "PETR4", Mercado.BRASIL, Moeda.BRL, "120.0000");
        Acao acaoUsd = acao(2L, "AAPL", Mercado.ESTADOS_UNIDOS, Moeda.USD, "180.0000");
        when(posicaoRepository.findAll()).thenReturn(List.of(
                posicao(acaoBrl, 10, "100.0000"),
                posicao(acaoUsd, 2, "150.0000")));
        when(transacaoRepository.somarResultadoRealizadoPorMoeda()).thenReturn(List.of(
                resultadoRealizado(Moeda.BRL, "50.0000"),
                resultadoRealizado(Moeda.USD, "-10.0000")));

        var resumos = service.resumirCarteiraPorMoeda();

        assertEquals(2, resumos.size());
        assertEquals(Moeda.BRL, resumos.get(0).moeda());
        assertDecimal("1000.0000", resumos.get(0).valorInvestidoTotal());
        assertDecimal("1200.0000", resumos.get(0).valorAtualTotal());
        assertDecimal("200.0000", resumos.get(0).resultadoNaoRealizadoTotal());
        assertDecimal("20.0000", resumos.get(0).rentabilidadePercentual());
        assertDecimal("50.0000", resumos.get(0).resultadoRealizadoTotal());
        assertDecimal("250.0000", resumos.get(0).resultadoTotal());
        assertEquals(Moeda.USD, resumos.get(1).moeda());
        assertDecimal("300.0000", resumos.get(1).valorInvestidoTotal());
        assertDecimal("360.0000", resumos.get(1).valorAtualTotal());
        assertDecimal("-10.0000", resumos.get(1).resultadoRealizadoTotal());
        assertDecimal("50.0000", resumos.get(1).resultadoTotal());
    }

    @Test
    void deveManterResultadoRealizadoZeroQuandoNaoHaVenda() {
        when(posicaoRepository.findAll()).thenReturn(List.of(posicao(acao, 10, "100.0000")));
        when(transacaoRepository.somarResultadoRealizadoPorMoeda()).thenReturn(List.of());

        var resumo = service.resumirCarteiraPorMoeda().getFirst();

        assertDecimal("0.0000", resumo.resultadoRealizadoTotal());
        assertDecimal("200.0000", resumo.resultadoTotal());
    }

    @Test
    void deveSomarLucroRealizadoAoNaoRealizadoSemAlterarRentabilidadeExistente() {
        when(posicaoRepository.findAll()).thenReturn(List.of(posicao(acao, 10, "100.0000")));
        when(transacaoRepository.somarResultadoRealizadoPorMoeda()).thenReturn(
                List.of(resultadoRealizado(Moeda.BRL, "350.0000")));

        var resumo = service.resumirCarteiraPorMoeda().getFirst();

        assertDecimal("200.0000", resumo.resultadoNaoRealizadoTotal());
        assertDecimal("350.0000", resumo.resultadoRealizadoTotal());
        assertDecimal("550.0000", resumo.resultadoTotal());
        assertDecimal("20.0000", resumo.rentabilidadePercentual());
    }

    @Test
    void deveSomarPrejuizoRealizadoAoResultadoTotal() {
        when(posicaoRepository.findAll()).thenReturn(List.of(posicao(acao, 10, "100.0000")));
        when(transacaoRepository.somarResultadoRealizadoPorMoeda()).thenReturn(
                List.of(resultadoRealizado(Moeda.BRL, "-300.0000")));

        var resumo = service.resumirCarteiraPorMoeda().getFirst();

        assertDecimal("-300.0000", resumo.resultadoRealizadoTotal());
        assertDecimal("-100.0000", resumo.resultadoTotal());
        assertDecimal("20.0000", resumo.rentabilidadePercentual());
    }

    @Test
    void deveExibirResultadoRealizadoMesmoSemPosicaoAberta() {
        when(posicaoRepository.findAll()).thenReturn(List.of());
        when(transacaoRepository.somarResultadoRealizadoPorMoeda()).thenReturn(
                List.of(resultadoRealizado(Moeda.BRL, "125.0000")));

        var resumo = service.resumirCarteiraPorMoeda().getFirst();

        assertEquals(Moeda.BRL, resumo.moeda());
        assertDecimal("0.0000", resumo.valorInvestidoTotal());
        assertDecimal("0.0000", resumo.valorAtualTotal());
        assertDecimal("0.0000", resumo.resultadoNaoRealizadoTotal());
        assertDecimal("125.0000", resumo.resultadoRealizadoTotal());
        assertDecimal("125.0000", resumo.resultadoTotal());
        assertDecimal("0.0000", resumo.rentabilidadePercentual());
    }

    @Test
    void deveExporResultadoSomenteParaVendaNoHistorico() {
        Transacao compra = transacao(TipoTransacao.COMPRA, "100.0000", null, null);
        Transacao venda = transacao(TipoTransacao.VENDA, "130.0000", "106.6667", "93.3332");
        when(transacaoRepository.findAll()).thenReturn(List.of(compra, venda));

        var historico = service.listarHistorico();
        var vendaDto = historico.getFirst();
        var compraDto = historico.getLast();

        assertDecimal("106.6667", vendaDto.precoMedioOperacao());
        assertDecimal("93.3332", vendaDto.resultadoRealizado());
        assertDecimal("21.8750", vendaDto.rentabilidadeRealizada());
        assertNull(compraDto.precoMedioOperacao());
        assertNull(compraDto.resultadoRealizado());
        assertNull(compraDto.rentabilidadeRealizada());
    }

    private void prepararReferencias() {
        when(acaoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(acao));
        when(corretoraRepository.findById(2L)).thenReturn(Optional.of(corretora));
    }

    private AtomicReference<Posicao> simularPersistenciaDaPosicao() {
        AtomicReference<Posicao> estado = new AtomicReference<>();
        when(posicaoRepository.findByAcaoId(acao.getId()))
                .thenAnswer(invocation -> Optional.ofNullable(estado.get()));
        when(posicaoRepository.save(any(Posicao.class))).thenAnswer(invocation -> {
            Posicao posicao = invocation.getArgument(0);
            if (posicao.getId() == null) {
                posicao.setId(3L);
            }
            estado.set(posicao);
            return posicao;
        });
        return estado;
    }

    private TransacaoRequestDTO dto(int quantidade, String valor) {
        return new TransacaoRequestDTO(1L, 2L, quantidade, new BigDecimal(valor));
    }

    private Acao acao(Long id, String ticker, Mercado mercado, Moeda moeda, String cotacao) {
        Acao novaAcao = new Acao();
        novaAcao.setId(id);
        novaAcao.setTicker(ticker);
        novaAcao.setNomeEmpresa(ticker);
        novaAcao.setMercado(mercado);
        novaAcao.setMoeda(moeda);
        novaAcao.setCotacaoAtual(cotacao == null ? null : new BigDecimal(cotacao));
        return novaAcao;
    }

    private Posicao posicao(Acao acaoDaPosicao, int quantidade, String precoMedio) {
        Posicao posicao = new Posicao();
        posicao.setId(3L);
        posicao.setAcao(acaoDaPosicao);
        posicao.setQuantidade(quantidade);
        posicao.setPrecoMedio(new BigDecimal(precoMedio));
        return posicao;
    }

    private Transacao transacao(TipoTransacao tipo, String valor, String precoMedio, String resultado) {
        Transacao transacao = new Transacao();
        transacao.setAcao(acao);
        transacao.setCorretora(corretora);
        transacao.setTipoTransacao(tipo);
        transacao.setQuantidade(4);
        transacao.setValorUnitario(new BigDecimal(valor));
        transacao.setPrecoMedioOperacao(precoMedio == null ? null : new BigDecimal(precoMedio));
        transacao.setResultadoRealizado(resultado == null ? null : new BigDecimal(resultado));
        transacao.setDataHoraTransacao(OffsetDateTime.of(
                tipo == TipoTransacao.VENDA ? 2026 : 2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        return transacao;
    }

    private ResultadoRealizadoPorMoeda resultadoRealizado(Moeda moeda, String valor) {
        return new ResultadoRealizadoPorMoeda(moeda, new BigDecimal(valor));
    }

    private void assertDecimal(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual));
    }
}
