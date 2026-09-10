package com.jeferson.gestaoacoes.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcaoMigrationTest {

    private static final String ESTRUTURA = "db/changelog/changes/001-criar-estrutura-inicial.xml";
    private static final String AJUSTE = "db/changelog/changes/003-ajustar-ticker-mercado-acoes.xml";
    private static final String MASTER = "db/changelog/db.changelog-master.xml";

    @Test
    void devePreservarAcaoEPosicaoExistentesAoMigrar() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, ESTRUTURA);
            inserirAcao(connection, "PETR4", "BRASIL");
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO posicoes (acao_id, quantidade, preco_medio)
                        SELECT id, 10, 25.5000 FROM acoes WHERE ticker = 'PETR4'
                        """);
            }
            long idOriginal = consultarNumero(connection, "SELECT id FROM acoes WHERE ticker = 'PETR4'");

            migrar(connection, AJUSTE);

            assertEquals(idOriginal, consultarNumero(connection, "SELECT id FROM acoes WHERE ticker = 'PETR4'"));
            assertEquals(1, consultarNumero(connection, """
                    SELECT COUNT(*) FROM acoes a JOIN posicoes p ON p.acao_id = a.id
                    WHERE a.ticker = 'PETR4' AND a.mercado = 'BRASIL'
                      AND a.nome_empresa = 'Empresa de teste' AND a.cotacao_atual = 30
                      AND a.moeda = 'BRL' AND a.provedor_origem = 'teste'
                      AND a.data_hora_cotacao = TIMESTAMP WITH TIME ZONE '2024-01-01 10:00:00+00'
                      AND p.quantidade = 10 AND p.preco_medio = 25.5000
                    """));
            inserirAcao(connection, "PETR4", "ESTADOS_UNIDOS");
            assertEquals(2, consultarNumero(connection, "SELECT COUNT(*) FROM acoes WHERE ticker = 'PETR4'"));
            assertThrows(SQLException.class, () -> inserirAcao(connection, "PETR4", "BRASIL"));
        }
    }

    @Test
    void deveAplicarLimiteEUnicidadeEmBancoNovoSemReexecutarMigration() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, MASTER);
            String ticker = "A".repeat(20);

            inserirAcao(connection, ticker, "BRASIL");
            inserirAcao(connection, ticker, "ESTADOS_UNIDOS");
            assertThrows(SQLException.class, () -> inserirAcao(connection, ticker, "BRASIL"));
            assertThrows(SQLException.class, () -> inserirAcao(connection, "B".repeat(21), "BRASIL"));
            assertThrows(SQLException.class, () -> inserirAcao(connection, "SEM_MERCADO", null));

            migrar(connection, MASTER);
            assertEquals(2, consultarNumero(connection, "SELECT COUNT(*) FROM acoes"));
            assertEquals(1, consultarNumero(connection, """
                    SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = '006-ajustar-ticker-mercado-acoes'
                    """));
        }
    }

    @Test
    void deveInterromperAntesDeAlterarTabelaSeExistirMercadoNulo() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, ESTRUTURA);
            inserirAcao(connection, "LEGADO", null);

            assertThrows(LiquibaseException.class, () -> migrar(connection, AJUSTE));

            assertEquals(1, consultarNumero(connection, "SELECT COUNT(*) FROM acoes WHERE mercado IS NULL"));
            assertThrows(SQLException.class, () -> inserirAcao(connection, "LEGADO", "BRASIL"));
            assertThrows(SQLException.class, () -> inserirAcao(connection, "A".repeat(11), "BRASIL"));
            assertEquals(0, consultarNumero(connection, """
                    SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = '006-ajustar-ticker-mercado-acoes'
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

    private void inserirAcao(Connection connection, String ticker, String mercado) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO acoes (ticker, nome_empresa, mercado, moeda, cotacao_atual,
                                   data_hora_cotacao, provedor_origem)
                VALUES (?, 'Empresa de teste', ?, ?, 30, TIMESTAMP WITH TIME ZONE '2024-01-01 10:00:00+00', 'teste')
                """)) {
            statement.setString(1, ticker);
            statement.setString(2, mercado);
            statement.setString(3, "ESTADOS_UNIDOS".equals(mercado) ? "USD" : "BRL");
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
