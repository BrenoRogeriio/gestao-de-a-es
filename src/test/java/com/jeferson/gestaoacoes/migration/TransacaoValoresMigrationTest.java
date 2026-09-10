package com.jeferson.gestaoacoes.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransacaoValoresMigrationTest {

    private static final String ESTRUTURA = "db/changelog/changes/001-criar-estrutura-inicial.xml";
    private static final String VALIDACOES = "db/changelog/changes/004-validar-valores-transacoes-posicoes.xml";

    @Test
    void devePreservarDadosValidosEAdicionarRestricoes() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, ESTRUTURA);
            inserirReferencias(connection);
            inserirPosicao(connection, 0);
            inserirTransacao(connection, 1, "10.1234");

            migrar(connection, VALIDACOES);

            assertEquals(1, consultarNumero(connection, "SELECT COUNT(*) FROM posicoes"));
            assertEquals(1, consultarNumero(connection, "SELECT COUNT(*) FROM transacoes"));
            assertThrows(SQLException.class, () -> atualizarQuantidadePosicao(connection, -1));
            assertThrows(SQLException.class, () -> inserirTransacao(connection, 0, "10.0000"));
            assertThrows(SQLException.class, () -> inserirTransacao(connection, -1, "10.0000"));
            assertThrows(SQLException.class, () -> inserirTransacao(connection, 1, "0"));
            assertThrows(SQLException.class, () -> inserirTransacao(connection, 1, "-0.0001"));
        }
    }

    @Test
    void deveInterromperAntesDeAlterarSchemaQuandoHouverDadosInvalidos() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, ESTRUTURA);
            inserirReferencias(connection);
            inserirPosicao(connection, -1);
            inserirTransacao(connection, 0, "-1.0000");

            assertThrows(LiquibaseException.class, () -> migrar(connection, VALIDACOES));

            assertEquals(-1, consultarNumero(connection, "SELECT quantidade FROM posicoes"));
            assertEquals(0, consultarNumero(connection, "SELECT quantidade FROM transacoes"));
            assertEquals(0, consultarNumero(connection, """
                    SELECT COUNT(*) FROM DATABASECHANGELOG
                    WHERE ID = '007-validar-valores-transacoes-posicoes'
                    """));
        }
    }

    private Connection abrirBanco() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "");
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
                    VALUES (1, 'TEST3', 'Empresa', 'BRASIL', 'BRL', 10,
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

    private void inserirPosicao(Connection connection, int quantidade) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO posicoes (acao_id, quantidade, preco_medio) VALUES (1, ?, 10.0000)
                """)) {
            statement.setInt(1, quantidade);
            statement.executeUpdate();
        }
    }

    private void atualizarQuantidadePosicao(Connection connection, int quantidade) throws SQLException {
        try (var statement = connection.prepareStatement("UPDATE posicoes SET quantidade = ? WHERE acao_id = 1")) {
            statement.setInt(1, quantidade);
            statement.executeUpdate();
        }
    }

    private void inserirTransacao(Connection connection, int quantidade, String valorUnitario) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO transacoes (acao_id, corretora_id, tipo_transacao, quantidade,
                                        valor_unitario, data_hora_transacao)
                VALUES (1, 1, 'COMPRA', ?, ?, TIMESTAMP WITH TIME ZONE '2024-01-01 10:00:00+00')
                """)) {
            statement.setInt(1, quantidade);
            statement.setBigDecimal(2, new BigDecimal(valorUnitario));
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
