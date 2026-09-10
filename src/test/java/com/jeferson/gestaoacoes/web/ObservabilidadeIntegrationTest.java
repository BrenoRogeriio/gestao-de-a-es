package com.jeferson.gestaoacoes.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ObservabilidadeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebEndpointsSupplier webEndpointsSupplier;

    @Test
    void healthDeveSerPublicoMinimoERastreavel() throws Exception {
        String requestId = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json")))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        UUID.fromString(requestId);
        assertNull(MDC.get("requestId"));
    }

    @Test
    void requestIdValidoEnviadoPeloClienteDeveSerPreservado() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "demo-request_123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "demo-request_123"));
    }

    @Test
    void requestIdInvalidoDeveSerSubstituido() throws Exception {
        String recebido = mockMvc.perform(get("/actuator/health")
                        .header("X-Request-Id", "valor inseguro com espaços"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertNotEquals("valor inseguro com espaços", recebido);
        UUID.fromString(recebido);
    }

    @Test
    void endpointFinanceiroDeveContinuar401ComCorrelationId() throws Exception {
        mockMvc.perform(get("/carteira/resumo").header("X-Request-Id", "request-sem-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Request-Id", "request-sem-jwt"));
    }

    @Test
    void apenasHealthEInfoDevemEstarExpostosPeloActuator() {
        Set<String> endpoints = webEndpointsSupplier.getEndpoints().stream()
                .map(endpoint -> endpoint.getEndpointId().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("health", "info"), endpoints);
    }

    @Test
    @WithMockUser
    void infoDeveExigirAutenticacaoEEndpointSensivelDeveSerNegado() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.name").value("gestao-acoes"));

        mockMvc.perform(get("/actuator/env").header("X-Request-Id", "request-negado"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Request-Id", "request-negado"));
    }

    @Test
    void infoNaoDeveSerPublico() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"));
    }
}
