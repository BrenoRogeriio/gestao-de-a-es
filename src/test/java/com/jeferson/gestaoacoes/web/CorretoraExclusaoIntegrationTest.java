package com.jeferson.gestaoacoes.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:corretora-exclusao;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
class CorretoraExclusaoIntegrationTest {

    private static final String ORIGEM_DEV = "http://localhost:5173";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void prepararCenario() throws Exception {
        jdbc.update("DELETE FROM transacoes");
        jdbc.update("DELETE FROM posicoes");
        jdbc.update("DELETE FROM acoes");
        jdbc.update("DELETE FROM corretoras");
        jdbc.update("DELETE FROM usuarios");
        token = cadastrarEObterToken();
    }

    @Test
    void deleteSemJwtDeveRetornar401() throws Exception {
        mockMvc.perform(delete("/corretoras/1")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void preflightDoDeleteDeveAceitarOrigemEHeadersAutorizados() throws Exception {
        mockMvc.perform(options("/corretoras/2")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        containsString("DELETE")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Idempotency-Key")));
    }

    @Test
    void excluirCorretoraInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(delete("/corretoras/999999")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV))
                .andExpect(jsonPath("$.detail").value("Corretora não encontrada."));
    }

    @Test
    void excluirCorretoraSemTransacoesDeveRetornar204ERemoverRegistro() throws Exception {
        Long corretoraId = inserirCorretora("11111111000191", "Corretora sem operações");

        mockMvc.perform(delete("/corretoras/{id}", corretoraId)
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV))
                .andExpect(content().string(""));

        assertEquals(0, contar("SELECT COUNT(*) FROM corretoras WHERE id = ?", corretoraId));
    }

    @Test
    void excluirCorretoraComTransacaoDeveRetornar409EPreservarHistorico() throws Exception {
        Long corretoraId = inserirCorretora("22222222000191", "Corretora em uso");
        Long acaoId = inserirAcao();
        Long usuarioId = jdbc.queryForObject(
                "SELECT id FROM usuarios WHERE email = 'exclusao@example.com'", Long.class);
        jdbc.update("""
                INSERT INTO transacoes (acao_id, corretora_id, usuario_id, tipo_transacao, quantidade,
                                        valor_unitario, preco_medio_operacao, data_hora_transacao)
                VALUES (?, ?, ?, 'COMPRA', 1, 10.0000, 10.0000, CURRENT_TIMESTAMP)
                """, acaoId, corretoraId, usuarioId);
        jdbc.update("""
                INSERT INTO posicoes (acao_id, usuario_id, quantidade, preco_medio)
                VALUES (?, ?, 1, 10.0000)
                """, acaoId, usuarioId);

        mockMvc.perform(delete("/corretoras/{id}", corretoraId)
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "Não é possível excluir a corretora porque existem operações vinculadas."));

        assertEquals(1, contar("SELECT COUNT(*) FROM corretoras WHERE id = ?", corretoraId));
        assertEquals(1, contar("SELECT COUNT(*) FROM transacoes WHERE corretora_id = ?", corretoraId));
        assertEquals(1, contar("SELECT COUNT(*) FROM posicoes WHERE acao_id = ? AND usuario_id = ?",
                acaoId, usuarioId));
    }

    private String cadastrarEObterToken() throws Exception {
        String resposta = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Teste Exclusão","email":"exclusao@example.com","senha":"senha-segura-123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resposta).get("token").asText();
    }

    private Long inserirCorretora(String cnpj, String razaoSocial) {
        jdbc.update("""
                INSERT INTO corretoras (cnpj, razao_social, cep, logradouro, bairro, cidade, uf,
                                         situacao_cadastral, status_cvm, data_hora_cadastro)
                VALUES (?, ?, '01001000', 'Praça da Sé', 'Sé', 'São Paulo', 'SP',
                        'ATIVA', 'EM FUNCIONAMENTO NORMAL', CURRENT_TIMESTAMP)
                """, cnpj, razaoSocial);
        return jdbc.queryForObject("SELECT id FROM corretoras WHERE cnpj = ?", Long.class, cnpj);
    }

    private Long inserirAcao() {
        jdbc.update("""
                INSERT INTO acoes (ticker, nome_empresa, mercado, moeda, cotacao_atual,
                                   data_hora_cotacao, provedor_origem)
                VALUES ('DEL3', 'Empresa exclusão', 'BRASIL', 'BRL', 10.0000,
                        CURRENT_TIMESTAMP, 'teste')
                """);
        return jdbc.queryForObject("SELECT id FROM acoes WHERE ticker = 'DEL3'", Long.class);
    }

    private int contar(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Integer.class, parametros);
    }
}
