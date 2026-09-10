package com.jeferson.gestaoacoes.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAccessDeniedHandlerTest {

    @Test
    void deveRetornarProblemDetailJsonPara403SemDetalhesInternos() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(new SecurityProblemWriter(objectMapper));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/recurso-restrito");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("detalhe interno"));

        JsonNode json = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertEquals("Acesso negado", json.get("title").asText());
        assertEquals("Você não possui autorização para acessar este recurso.", json.get("detail").asText());
        assertFalse(response.getContentAsString().contains("detalhe interno"));
    }
}
