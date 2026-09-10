package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.AcaoRequestDTO;
import com.jeferson.gestaoacoes.dto.AcaoResponseDTO;
import com.jeferson.gestaoacoes.exception.ProvedorExternoIndisponivelException;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.exception.RespostaExternaInvalidaException;
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

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
public class AcaoService {

    private static final String PAIS_ESTADOS_UNIDOS = "United States";

    private final AcaoRepository repository;
    private final AcaoMapper mapper;
    private final BrapiClient brapiClient;
    private final TwelveDataClient twelveDataClient;

    // Lendo os tokens do application.yml (vazios por padrão no ambiente local)
    @Value("${app.market-data.brapi.token}")
    private String brapiToken;

    @Value("${app.market-data.twelve-data.apikey}")
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
        OffsetDateTime instanteConsulta = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            if (acao.getMercado() == Mercado.BRASIL) {
                BrapiResponse response = brapiClient.consultarCotacao(acao.getTicker(), brapiToken);
                preencherCotacaoBrapi(acao, response, instanteConsulta);
            } else if (acao.getMercado() == Mercado.ESTADOS_UNIDOS) {
                TwelveDataResponse response = twelveDataClient.consultarCotacao(
                        acao.getTicker(), PAIS_ESTADOS_UNIDOS, twelveDataApiKey);
                preencherCotacaoTwelveData(acao, response, instanteConsulta);
            }
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new RegraNegocioException("Ticker não encontrado no mercado informado.");
            }
            throw new ProvedorExternoIndisponivelException(
                    "O provedor de dados de mercado está indisponível no momento.", e);
        }
    }

    private void preencherCotacaoBrapi(Acao acao, BrapiResponse response, OffsetDateTime instanteConsulta) {
        if (response == null) {
            throw new RespostaExternaInvalidaException("A Brapi retornou uma resposta vazia.");
        }
        if (response.results() == null || response.results().isEmpty()) {
            throw new RegraNegocioException("Ticker não encontrado no mercado brasileiro (Brapi).");
        }

        var dados = response.results().get(0);
        if (dados == null) {
            throw new RespostaExternaInvalidaException("A Brapi retornou um resultado vazio.");
        }
        validarIdentificacaoCotacao(
                acao.getTicker(), dados.symbol(), dados.currency(), "BRL", dados.shortName(), "Brapi");
        validarCotacaoPositiva(dados.regularMarketPrice(), "Brapi");

        acao.setNomeEmpresa(dados.shortName().trim());
        acao.setMoeda(Moeda.BRL);
        acao.setCotacaoAtual(dados.regularMarketPrice());
        acao.setDataHoraCotacao(converterTimestampBrapi(dados.regularMarketTime(), instanteConsulta));
        acao.setProvedorOrigem("brapi.dev");
    }

    private void preencherCotacaoTwelveData(
            Acao acao, TwelveDataResponse response, OffsetDateTime instanteConsulta) {
        if (response == null) {
            throw new RespostaExternaInvalidaException("A Twelve Data retornou uma resposta vazia.");
        }
        if (respostaDeErro(response)) {
            if (erroIndicaTickerInexistente(response)) {
                throw new RegraNegocioException("Ticker não encontrado no mercado americano (Twelve Data).");
            }
            throw new RespostaExternaInvalidaException("A Twelve Data rejeitou a consulta: "
                    + mensagemExterna(response.message()));
        }

        validarIdentificacaoCotacao(
                acao.getTicker(), response.symbol(), response.currency(), "USD", response.name(), "Twelve Data");
        validarCotacaoPositiva(response.close(), "Twelve Data");

        acao.setNomeEmpresa(response.name().trim());
        acao.setMoeda(Moeda.USD);
        acao.setCotacaoAtual(response.close());
        acao.setDataHoraCotacao(converterTimestampTwelveData(response.timestamp(), instanteConsulta));
        acao.setProvedorOrigem("Twelve Data");
    }

    private static void validarIdentificacaoCotacao(
            String tickerSolicitado,
            String tickerRetornado,
            String moedaRetornada,
            String moedaEsperada,
            String nomeEmpresa,
            String provedor) {
        if (!textoObrigatorio(tickerRetornado)
                || !tickerSolicitado.equals(tickerRetornado.trim().toUpperCase(Locale.ROOT))) {
            throw new RespostaExternaInvalidaException(
                    "A " + provedor + " retornou um ticker diferente do solicitado.");
        }
        if (!textoObrigatorio(moedaRetornada)
                || !moedaEsperada.equals(moedaRetornada.trim().toUpperCase(Locale.ROOT))) {
            throw new RespostaExternaInvalidaException(
                    "A " + provedor + " retornou uma moeda incompatível com o mercado.");
        }
        if (!textoObrigatorio(nomeEmpresa)) {
            throw new RespostaExternaInvalidaException(
                    "A " + provedor + " não retornou o nome da empresa.");
        }
    }

    private static void validarCotacaoPositiva(BigDecimal cotacao, String provedor) {
        if (cotacao == null || cotacao.signum() <= 0) {
            throw new RespostaExternaInvalidaException(
                    "A " + provedor + " retornou uma cotação ausente, zero ou negativa.");
        }
    }

    private static OffsetDateTime converterTimestampBrapi(
            String timestamp, OffsetDateTime instanteConsulta) {
        if (!textoObrigatorio(timestamp)) {
            return instanteConsulta;
        }
        try {
            return OffsetDateTime.parse(timestamp).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new RespostaExternaInvalidaException("A Brapi retornou um timestamp inválido.");
        }
    }

    private static OffsetDateTime converterTimestampTwelveData(
            Long timestamp, OffsetDateTime instanteConsulta) {
        if (timestamp == null) {
            return instanteConsulta;
        }
        if (timestamp <= 0) {
            throw new RespostaExternaInvalidaException("A Twelve Data retornou um timestamp inválido.");
        }
        try {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC);
        } catch (RuntimeException e) {
            throw new RespostaExternaInvalidaException("A Twelve Data retornou um timestamp inválido.");
        }
    }

    private static boolean respostaDeErro(TwelveDataResponse response) {
        return "ERROR".equals(normalizarTexto(response.status()))
                || response.code() != null && response.code() >= 400;
    }

    private static boolean erroIndicaTickerInexistente(TwelveDataResponse response) {
        String mensagem = normalizarTexto(response.message());
        return Integer.valueOf(404).equals(response.code())
                || mensagem.contains("INVALID SYMBOL")
                || mensagem.contains("SYMBOL NOT FOUND")
                || mensagem.contains("NO DATA");
    }

    private static String mensagemExterna(String mensagem) {
        return textoObrigatorio(mensagem) ? mensagem.trim() : "erro sem mensagem";
    }

    private static boolean textoObrigatorio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
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
