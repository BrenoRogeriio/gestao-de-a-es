package com.jeferson.gestaoacoes.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SEGREDO_TESTE = "test-only-jwt-secret-with-at-least-32-bytes-long";

    @Test
    void deveGerarEValidarJwtAssinado() {
        JwtService service = new JwtService(SEGREDO_TESTE, 480);
        var usuario = User.withUsername("usuario@example.com").password("hash").roles("USER").build();

        String token = service.gerarToken(usuario);

        assertEquals("usuario@example.com", service.extrairEmail(token));
        assertTrue(service.tokenValido(token, usuario));
        assertEquals(28_800, service.getExpiracaoSegundos());
    }

    @Test
    void deveRejeitarSegredoCurto() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new JwtService("segredo-curto", 480));

        assertEquals("JWT_SECRET deve possuir pelo menos 32 bytes.", exception.getMessage());
    }

    @Test
    void deveRejeitarSegredoEmBrancoMesmoComTamanhoSuficiente() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new JwtService(" ".repeat(32), 480));

        assertEquals("JWT_SECRET deve possuir pelo menos 32 bytes.", exception.getMessage());
    }

    @Test
    void deveRejeitarExpiracaoInvalida() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> new JwtService(SEGREDO_TESTE, 0));

        assertEquals("JWT_EXPIRATION_MINUTES deve ser maior que zero.", exception.getMessage());
    }
}
