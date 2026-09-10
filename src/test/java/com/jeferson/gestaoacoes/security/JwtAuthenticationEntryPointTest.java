package com.jeferson.gestaoacoes.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationEntryPointTest {

    @Test
    void deveRetornarProblemDetailJsonPara401SemDetalhesInternos() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(
                new SecurityProblemWriter(objectMapper));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/acoes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("detalhe interno"));

        JsonNode json = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertEquals("Não autorizado", json.get("title").asText());
        assertEquals("Autenticação ausente ou inválida.", json.get("detail").asText());
        assertFalse(response.getContentAsString().contains("detalhe interno"));
    }
}
