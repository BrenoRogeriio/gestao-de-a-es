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

class DadosDemoMigrationTest {

    private static final String MASTER = "db/changelog/db.changelog-master.xml";
    private static final String ESTRUTURA = "db/changelog/changes/001-criar-estrutura-inicial.xml";
    private static final String DADOS_DEMO = "db/changelog/changes/002-inserir-dados-teste.xml";

    @Test
    void deveIgnorarDadosDemoQuandoContextoNaoForExplicitamenteAtivado() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, MASTER, "prod");

            assertEquals(0, consultarNumero(connection, "SELECT COUNT(*) FROM corretoras"));
            assertEquals(1, consultarNumero(connection, """
                    SELECT COUNT(*) FROM DATABASECHANGELOG
                    WHERE ID = '001-criar-tabela-acoes'
                    """));
        }
    }

    @Test
    void deveInserirDadosDemoSomenteComContextoDemo() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, MASTER, "demo");

            assertEquals(2, consultarNumero(connection, "SELECT COUNT(*) FROM corretoras"));
            assertEquals(1, consultarNumero(connection, """
                    SELECT COUNT(*) FROM DATABASECHANGELOG
                    WHERE ID = '005-inserir-corretoras-teste'
                    """));
        }
    }

    @Test
    void devePreservarBancoQueJaExecutouOChangeSetDeDadosDemo() throws Exception {
        try (Connection connection = abrirBanco()) {
            migrar(connection, ESTRUTURA, "");
            migrar(connection, DADOS_DEMO, "");

            migrar(connection, MASTER, "prod");

            assertEquals(2, consultarNumero(connection, "SELECT COUNT(*) FROM corretoras"));
            assertEquals(1, consultarNumero(connection, """
                    SELECT COUNT(*) FROM DATABASECHANGELOG
                    WHERE ID = '005-inserir-corretoras-teste'
                    """));
        }
    }

    private Connection abrirBanco() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "");
    }

    private void migrar(Connection connection, String changelog, String contexts) throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            var liquibase = new Liquibase(changelog, resources, new JdbcConnection(connection));
            liquibase.update(new Contexts(contexts), new LabelExpression());
        }
    }

    private long consultarNumero(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
