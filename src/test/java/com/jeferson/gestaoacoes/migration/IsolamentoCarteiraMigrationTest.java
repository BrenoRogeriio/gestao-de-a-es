package com.jeferson.gestaoacoes.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IsolamentoCarteiraMigrationTest {

    private static final String ESTRUTURA = "db/changelog/changes/001-criar-estrutura-inicial.xml";
    private static final String IDEMPOTENCIA = "db/changelog/changes/005-adicionar-idempotencia-transacoes.xml";
    private static final String USUARIOS = "db/changelog/changes/007-adicionar-usuarios-autenticacao.xml";
    private static final String ISOLAMENTO = "db/changelog/changes/008-isolar-carteira-por-usuario.xml";

    @Test
    void devePreservarLegadoEAplicarPropriedadeComConstraintsCompostas() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "")) {
            migrar(connection, ESTRUTURA);
            migrar(connection, IDEMPOTENCIA);
            migrar(connection, USUARIOS);
            inserirReferencias(connection);
            inserirPosicao(connection, null);
            inserirTransacao(connection, null, "chave-legada");

            migrar(connection, ISOLAMENTO);

            assertEquals(1, consultar(connection, "SELECT COUNT(*) FROM posicoes WHERE usuario_id IS NULL"));
            assertEquals(1, consultar(connection, "SELECT COUNT(*) FROM transacoes WHERE usuario_id IS NULL"));

            inserirUsuario(connection, 1, "a@example.com");
            inserirUsuario(connection, 2, "b@example.com");
            inserirPosicao(connection, 1L);
            inserirPosicao(connection, 2L);
            assertEquals(3, consultar(connection, "SELECT COUNT(*) FROM posicoes"));
            assertThrows(SQLException.class, () -> inserirPosicao(connection, 1L));

            inserirTransacao(connection, 1L, "mesma-chave");
            inserirTransacao(connection, 2L, "mesma-chave");
            assertThrows(SQLException.class, () -> inserirTransacao(connection, 1L, "mesma-chave"));
            assertEquals(2, consultar(connection,
                    "SELECT COUNT(*) FROM transacoes WHERE idempotency_key = 'mesma-chave'"));

            assertThrows(SQLException.class, () -> inserirPosicao(connection, 999L));
            assertThrows(SQLException.class, () -> inserirTransacao(connection, 999L, "fk-invalida"));
        }
    }

    private void migrar(Connection connection, String changelog) throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            new Liquibase(changelog, resources, new JdbcConnection(connection))
                    .update(new Contexts(), new LabelExpression());
        }
    }

    private void inserirReferencias(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO acoes (id, ticker, nome_empresa, mercado, moeda)
                    VALUES (1, 'MULTI3', 'Empresa', 'BRASIL', 'BRL')
                    """);
            statement.executeUpdate("""
                    INSERT INTO corretoras (id, cnpj, razao_social, cep, logradouro, bairro, cidade, uf,
                                             situacao_cadastral, status_cvm, data_hora_cadastro)
                    VALUES (1, '12345678000199', 'Corretora', '12345678', 'Rua', 'Bairro', 'Cidade', 'SP',
                            'ATIVA', 'REGULAR', CURRENT_TIMESTAMP)
                    """);
        }
    }

    private void inserirUsuario(Connection connection, long id, String email) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO usuarios (id, nome, email, senha_hash, perfil, ativo, data_hora_cadastro)
                VALUES (?, 'Usuario', ?, 'hash-de-teste', 'USER', TRUE, CURRENT_TIMESTAMP)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private void inserirPosicao(Connection connection, Long usuarioId) throws SQLException {
        String colunas = usuarioId == null ? "acao_id, quantidade, preco_medio" : "usuario_id, acao_id, quantidade, preco_medio";
        String valores = usuarioId == null ? "1, 10, 10.0000" : "?, 1, 10, 10.0000";
        try (var statement = connection.prepareStatement("INSERT INTO posicoes (" + colunas + ") VALUES (" + valores + ")")) {
            if (usuarioId != null) statement.setLong(1, usuarioId);
            statement.executeUpdate();
        }
    }

    private void inserirTransacao(Connection connection, Long usuarioId, String chave) throws SQLException {
        String sql = usuarioId == null
                ? """
                  INSERT INTO transacoes (acao_id, corretora_id, tipo_transacao, quantidade,
                                           valor_unitario, data_hora_transacao, idempotency_key)
                  VALUES (1, 1, 'COMPRA', 1, 10.0000, CURRENT_TIMESTAMP, ?)
                  """
                : """
                  INSERT INTO transacoes (usuario_id, acao_id, corretora_id, tipo_transacao, quantidade,
                                           valor_unitario, data_hora_transacao, idempotency_key)
                  VALUES (?, 1, 1, 'COMPRA', 1, 10.0000, CURRENT_TIMESTAMP, ?)
                  """;
        try (var statement = connection.prepareStatement(sql)) {
            int indice = 1;
            if (usuarioId != null) statement.setLong(indice++, usuarioId);
            statement.setString(indice, chave);
            statement.executeUpdate();
        }
    }

    private long consultar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
