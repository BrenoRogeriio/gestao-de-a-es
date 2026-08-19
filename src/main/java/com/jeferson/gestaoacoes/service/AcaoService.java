package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.AcaoRequestDTO;
import com.jeferson.gestaoacoes.dto.AcaoResponseDTO;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.infrastructure.client.BrapiClient;
import com.jeferson.gestaoacoes.infrastructure.client.BrapiResponse;
import com.jeferson.gestaoacoes.infrastructure.client.TwelveDataClient;
import com.jeferson.gestaoacoes.infrastructure.client.TwelveDataResponse;
import com.jeferson.gestaoacoes.mapper.AcaoMapper;
import com.jeferson.gestaoacoes.model.Acao;
import com.jeferson.gestaoacoes.model.Mercado;
import com.jeferson.gestaoacoes.model.Moeda;
import com.jeferson.gestaoacoes.repository.AcaoRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class AcaoService {

    private final AcaoRepository repository;
    private final AcaoMapper mapper;
    private final BrapiClient brapiClient;
    private final TwelveDataClient twelveDataClient;

    // Lendo os tokens do application.yml (vazios por padrão no ambiente local)
    @Value("${app.market-data.brapi.token:}")
    private String brapiToken;

    @Value("${app.market-data.twelve-data.apikey:}")
    private String twelveDataApiKey;

    public AcaoService(AcaoRepository repository, AcaoMapper mapper,
                       BrapiClient brapiClient, TwelveDataClient twelveDataClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.brapiClient = brapiClient;
        this.twelveDataClient = twelveDataClient;
    }

    @Transactional
    public AcaoResponseDTO cadastrar(AcaoRequestDTO dto) {
        // Normalização do ticker (ex: de "petr4" para "PETR4")
        String tickerNormalizado = dto.ticker().trim().toUpperCase();

        // RN07: Validar duplicidade lógica (Ticker + Mercado)
        if (repository.existsByTickerAndMercado(tickerNormalizado, dto.mercado())) {
            throw new RegraNegocioException("Ação já cadastrada para este mercado.");
        }

        Acao acao = new Acao();
        acao.setTicker(tickerNormalizado);
        acao.setMercado(dto.mercado());

        // Busca e preenche os dados financeiros no provedor externo correspondente
        buscarEPreencherCotacao(acao);

        Acao acaoSalva = repository.save(acao);
        return mapper.toResponseDTO(acaoSalva);
    }

    private void buscarEPreencherCotacao(Acao acao) {
        try {
            // RN06: Roteamento dinâmico pelo mercado
            if (acao.getMercado() == Mercado.BRASIL) {
                BrapiResponse response = brapiClient.consultarCotacao(acao.getTicker(), brapiToken);

                if (response.results() == null || response.results().isEmpty()) {
                    throw new RegraNegocioException("Ticker não encontrado no mercado brasileiro (Brapi).");
                }

                var dados = response.results().get(0);
                acao.setNomeEmpresa(dados.shortName());
                acao.setMoeda(Moeda.BRL);
                acao.setCotacaoAtual(dados.regularMarketPrice());
                acao.setDataHoraCotacao(OffsetDateTime.now(ZoneOffset.UTC));
                acao.setProvedorOrigem("brapi.dev");

            } else if (acao.getMercado() == Mercado.ESTADOS_UNIDOS) {
                TwelveDataResponse response = twelveDataClient.consultarCotacao(acao.getTicker(), twelveDataApiKey);

                // Twelve Data retorna os dados direto no objeto principal, mas sem 'close' se o ticker for inválido
                if (response.close() == null) {
                    throw new RegraNegocioException("Ticker não encontrado no mercado americano (Twelve Data).");
                }

                acao.setNomeEmpresa(response.name());
                acao.setMoeda(Moeda.USD);
                acao.setCotacaoAtual(response.close());
                acao.setDataHoraCotacao(OffsetDateTime.now(ZoneOffset.UTC));
                acao.setProvedorOrigem("Twelve Data");
            }
        } catch (FeignException e) {
            throw new RegraNegocioException("O provedor de dados de mercado está indisponível no momento.");
        }
    }
    @Transactional
    public AcaoResponseDTO atualizarCotacao(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Ação não encontrada pelo ID."));

        // A RN11 diz que devemos manter a cotação antiga se falhar, mas o ideal é
        // tentar buscar e se falhar o buscarEPreencherCotacao lança exceção e faz rollback.
        buscarEPreencherCotacao(acao);

        return mapper.toResponseDTO(repository.save(acao));
    }

    public java.util.List<AcaoResponseDTO> buscarPorTicker(String ticker, Mercado mercado) {
        String tickerNormalizado = ticker.trim().toUpperCase();

        if (mercado != null) {
            return repository.findByTickerAndMercado(tickerNormalizado, mercado)
                    .map(mapper::toResponseDTO)
                    .stream().toList();
        }

        return repository.findByTicker(tickerNormalizado)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }
}