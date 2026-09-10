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

class IdempotenciaMigrationTest {

    private static final String ESTRUTURA = "db/changelog/changes/001-criar-estrutura-inicial.xml";
    private static final String IDEMPOTENCIA = "db/changelog/changes/005-adicionar-idempotencia-transacoes.xml";

    @Test
    void devePreservarTransacoesExistentesEImpedirChavesDuplicadas() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "")) {
            migrar(connection, ESTRUTURA);
            inserirReferencias(connection);
            inserirTransacao(connection, null);

            migrar(connection, IDEMPOTENCIA);

            assertEquals(1, consultarNumero(connection, "SELECT COUNT(*) FROM transacoes"));
            inserirTransacao(connection, "requisicao-1");
            assertThrows(SQLException.class, () -> inserirTransacao(connection, "requisicao-1"));
            inserirTransacao(connection, null);
            assertEquals(3, consultarNumero(connection, "SELECT COUNT(*) FROM transacoes"));
        }
    }

    private void migrar(Connection connection, String changelog) throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            var liquibase = new Liquibase(changelog, resources, new JdbcConnection(connection));
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }

    private void inserirReferencias(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO acoes (id, ticker, nome_empresa, mercado, moeda, cotacao_atual,
                                       data_hora_cotacao, provedor_origem)
                    VALUES (1, 'IDEM3', 'Empresa', 'BRASIL', 'BRL', 10,
                            TIMESTAMP WITH TIME ZONE '2024-01-01 10:00:00+00', 'teste')
                    """);
            statement.executeUpdate("""
                    INSERT INTO corretoras (id, cnpj, razao_social, cep, logradouro, bairro, cidade, uf,
                                             situacao_cadastral, status_cvm, data_hora_cadastro)
                    VALUES (1, '12345678000199', 'Corretora', '12345678', 'Rua', 'Bairro', 'Cidade', 'SP',
                            'ATIVA', 'REGULAR', TIMESTAMP WITH TIME ZONE '2024-01-01 10:00:00+00')
                    """);
        }
    }

    private void inserirTransacao(Connection connection, String chave) throws SQLException {
        String sql = chave == null
                ? """
                  INSERT INTO transacoes (acao_id, corretora_id, tipo_transacao, quantidade,
                                           valor_unitario, data_hora_transacao)
                  VALUES (1, 1, 'COMPRA', 1, 10, CURRENT_TIMESTAMP)
                  """
                : """
                  INSERT INTO transacoes (acao_id, corretora_id, tipo_transacao, quantidade,
                                           valor_unitario, data_hora_transacao, idempotency_key)
                  VALUES (1, 1, 'COMPRA', 1, 10, CURRENT_TIMESTAMP, ?)
                  """;
        try (var statement = connection.prepareStatement(sql)) {
            if (chave != null) {
                statement.setString(1, chave);
            }
            statement.executeUpdate();
        }
    }

    private long consultarNumero(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
