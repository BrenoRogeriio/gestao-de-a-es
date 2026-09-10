package com.jeferson.gestaoacoes.web;

import com.jeferson.gestaoacoes.config.HttpConfiguration;
import com.jeferson.gestaoacoes.config.SecurityHeadersFilter;
import com.jeferson.gestaoacoes.config.RequestObservabilityFilter;
import com.jeferson.gestaoacoes.controller.CarteiraController;
import com.jeferson.gestaoacoes.exception.GlobalExceptionHandler;
import com.jeferson.gestaoacoes.exception.ProvedorExternoIndisponivelException;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.exception.RespostaExternaInvalidaException;
import com.jeferson.gestaoacoes.service.CarteiraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.jeferson.gestaoacoes.security.JwtAuthenticationFilter;

import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CarteiraController.class,
        properties = "app.http.cors.allowed-origins=http://localhost:5173",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
)
@Import({HttpConfiguration.class, SecurityHeadersFilter.class, RequestObservabilityFilter.class,
        GlobalExceptionHandler.class})
@WithMockUser
class HttpConfigurationIntegrationTest {

    private static final String ORIGEM_DEV = "http://localhost:5173";
    private static final String TRANSACAO_VALIDA = """
            {
              "acaoId": 1,
              "corretoraId": 2,
              "quantidade": 3,
              "valorUnitario": 10.1234
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarteiraService carteiraService;

    @Test
    void devePermitirOrigemDevEAdicionarHeadersDeSeguranca() throws Exception {
        when(carteiraService.listarPosicoes()).thenReturn(List.of());

        mockMvc.perform(get("/carteira/posicao").header(HttpHeaders.ORIGIN, ORIGEM_DEV))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void deveAceitarPreflightComHeadersNecessarios() throws Exception {
        mockMvc.perform(options("/carteira/comprar")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Content-Type, Authorization, Idempotency-Key, X-Request-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Idempotency-Key")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("X-Request-Id")));
    }

    @Test
    void deveRejeitarOrigemNaoPermitida() throws Exception {
        mockMvc.perform(options("/carteira/comprar")
                        .header(HttpHeaders.ORIGIN, "https://origem-invalida.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void deveAceitarIdempotencyKey() throws Exception {
        mockMvc.perform(post("/carteira/comprar")
                        .with(csrf())
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV)
                        .header("Idempotency-Key", "operacao-http-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRANSACAO_VALIDA))
                .andExpect(status().isOk());

        verify(carteiraService).registrarCompra(any(), eq("operacao-http-1"));
    }

    @Test
    void deveRetornarErroDeValidacaoConsistente() throws Exception {
        mockMvc.perform(post("/carteira/comprar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.acaoId").isString())
                .andExpect(jsonPath("$.erros.corretoraId").isString())
                .andExpect(jsonPath("$.erros.quantidade").isString())
                .andExpect(jsonPath("$.erros.valorUnitario").isString());
    }

    @Test
    void deveOcultarDetalhesDeJsonInvalido() throws Exception {
        mockMvc.perform(post("/carteira/comprar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{json-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("O corpo da requisição está inválido."))
                .andExpect(content().string(not(containsString("HttpMessageNotReadableException"))))
                .andExpect(content().string(not(containsString("Jackson"))));
    }

    @Test
    void devePreservarStatus422() throws Exception {
        when(carteiraService.listarPosicoes()).thenThrow(new RegraNegocioException("Regra visível."));

        mockMvc.perform(get("/carteira/posicao"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Regra visível."));
    }

    @Test
    void devePreservarStatus502SemRefletirDetalheExterno() throws Exception {
        when(carteiraService.listarPosicoes())
                .thenThrow(new RespostaExternaInvalidaException("apikey=segredo; SQL interno"));

        mockMvc.perform(get("/carteira/posicao"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("O provedor externo retornou uma resposta inválida."))
                .andExpect(content().string(not(containsString("segredo"))))
                .andExpect(content().string(not(containsString("SQL interno"))));
    }

    @Test
    void devePreservarStatus503SemExporCausaFeign() throws Exception {
        when(carteiraService.listarPosicoes()).thenThrow(new ProvedorExternoIndisponivelException(
                "https://api.example?token=segredo", new IllegalStateException("Feign internals")));

        mockMvc.perform(get("/carteira/posicao"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail")
                        .value("O provedor externo está temporariamente indisponível."))
                .andExpect(content().string(not(containsString("token=segredo"))))
                .andExpect(content().string(not(containsString("Feign"))));
    }

    @Test
    void deveRetornar500GenericoSemDetalhesTecnicos() throws Exception {
        when(carteiraService.listarPosicoes())
                .thenThrow(new IllegalStateException("org.hibernate erro SQL jdbc:postgresql://host"));

        mockMvc.perform(get("/carteira/posicao"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail")
                        .value("Ocorreu um erro interno. Tente novamente mais tarde."))
                .andExpect(content().string(not(containsString("hibernate"))))
                .andExpect(content().string(not(containsString("jdbc:postgresql"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void deveRejeitarWildcardNaConfiguracaoCors() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new HttpConfiguration("*"));

        assertEquals("app.http.cors.allowed-origins nao aceita '*'; configure origens explicitas",
                exception.getMessage());
    }

    @Test
    void deveConfigurarSwaggerPorAmbiente() throws Exception {
        Properties dev = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application-dev.properties"));
        Properties prod = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application-prod.properties"));

        assertEquals("true", dev.getProperty("springdoc.api-docs.enabled"));
        assertEquals("true", dev.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("${SWAGGER_ENABLED:false}", prod.getProperty("springdoc.api-docs.enabled"));
        assertEquals("${SWAGGER_ENABLED:false}", prod.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("${CORS_ALLOWED_ORIGINS:}", prod.getProperty("app.http.cors.allowed-origins"));
    }
}
