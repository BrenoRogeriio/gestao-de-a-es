package com.jeferson.gestaoacoes.integration;

import com.jeferson.gestaoacoes.dto.TransacaoRequestDTO;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.model.Usuario;
import com.jeferson.gestaoacoes.repository.AcaoRepository;
import com.jeferson.gestaoacoes.security.UsuarioAtualService;
import com.jeferson.gestaoacoes.service.CarteiraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class PostgresTestcontainersIntegrationTest {

    private static final ZoneId FUSO_TRANSACAO = ZoneId.of("America/Sao_Paulo");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("gestao_acoes_test")
            .withUsername("gestao_acoes_test")
            .withPassword("gestao_acoes_test");

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.liquibase.contexts", () -> "test");
    }

    @Autowired
    private CarteiraService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private UsuarioAtualService usuarioAtualService;

    private Long usuarioId;

    @BeforeEach
    void limparBancoTemporario() {
        jdbc.execute("TRUNCATE TABLE transacoes, posicoes, acoes, corretoras, usuarios RESTART IDENTITY CASCADE");
        inserirUsuario("proprietario@example.com");
        usuarioId = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = 'proprietario@example.com'", Long.class);
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        when(usuarioAtualService.obterReferencia()).thenReturn(usuario);
        when(usuarioAtualService.obterId()).thenReturn(usuarioId);
    }

    @Test
    void deveAplicarChangelogCompletoComConstraintsETiposPostgres() {
        assertTrue(jdbc.queryForObject("SELECT version()", String.class).startsWith("PostgreSQL 17"));
        assertEquals(POSTGRES.getDatabaseName(),
                jdbc.queryForObject("SELECT current_database()", String.class));
        assertEquals(10, contar("SELECT COUNT(*) FROM databasechangelog"));
        assertEquals(9, contar("""
                SELECT COUNT(*)
                  FROM pg_constraint
                 WHERE conname IN ('uk_acao_ticker_mercado',
                                   'ck_posicoes_quantidade_nao_negativa',
                                   'ck_transacoes_quantidade_positiva',
                                   'ck_transacoes_valor_unitario_positivo',
                                   'uk_posicoes_usuario_acao',
                                   'uk_transacoes_usuario_idempotency_key',
                                   'uk_usuarios_email',
                                   'fk_posicao_usuario',
                                   'fk_transacao_usuario')
                """));
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM pg_constraint
                 WHERE conname IN ('uk_posicoes_acao', 'uk_transacoes_idempotency_key')
                """));
        assertEquals("timestamp with time zone", tipoColuna("acoes", "data_hora_cotacao"));
        assertEquals("timestamp with time zone", tipoColuna("transacoes", "data_hora_transacao"));
        assertEquals("timestamp with time zone", tipoColuna("usuarios", "data_hora_cadastro"));
        assertEquals("integer", tipoColuna("posicoes", "quantidade"));
        assertEquals("integer", tipoColuna("transacoes", "quantidade"));
        assertTipoNumerico("posicoes", "preco_medio", 19, 4);
        assertTipoNumerico("transacoes", "valor_unitario", 19, 4);
        assertTipoNumerico("transacoes", "preco_medio_operacao", 19, 4);
        assertTipoNumerico("transacoes", "resultado_realizado", 29, 4);
        assertEquals(20, tamanhoMaximo("acoes", "ticker"));
        assertEquals(100, tamanhoMaximo("transacoes", "idempotency_key"));
        assertEquals(254, tamanhoMaximo("usuarios", "email"));
    }

    @Test
    void deveAplicarPropriedadeEIdempotenciaPorUsuarioNoPostgres() {
        inserirUsuario("segundo@example.com");
        Long segundoUsuarioId = jdbc.queryForObject(
                "SELECT id FROM usuarios WHERE email = 'segundo@example.com'", Long.class);
        Long acaoId = inserirAcao("MULT3", "BRASIL");
        Long corretoraId = inserirCorretora();

        inserirPosicao(usuarioId, acaoId);
        inserirPosicao(segundoUsuarioId, acaoId);
        assertEquals(2, contar("SELECT COUNT(*) FROM posicoes WHERE acao_id = ?", acaoId));
        assertThrows(DataIntegrityViolationException.class, () -> inserirPosicao(usuarioId, acaoId));

        inserirTransacao(usuarioId, acaoId, corretoraId, "chave-compartilhada");
        inserirTransacao(segundoUsuarioId, acaoId, corretoraId, "chave-compartilhada");
        assertEquals(2, contar(
                "SELECT COUNT(*) FROM transacoes WHERE idempotency_key = 'chave-compartilhada'"));
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(usuarioId, acaoId, corretoraId, "chave-compartilhada"));

        assertThrows(DataIntegrityViolationException.class, () -> inserirPosicao(Long.MAX_VALUE, acaoId));
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(Long.MAX_VALUE, acaoId, corretoraId, "usuario-inexistente"));
    }

    @Test
    void deveImpedirEmailDuplicadoDiretamenteNoBanco() {
        inserirUsuario("usuario@example.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> inserirUsuario("usuario@example.com"));
    }

    @Test
    void deveAplicarUnicidadeDeTickerPorMercado() {
        inserirAcao("MESMO", "BRASIL");
        inserirAcao("MESMO", "ESTADOS_UNIDOS");

        assertEquals(2, contar("SELECT COUNT(*) FROM acoes WHERE ticker = 'MESMO'"));
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirAcao("MESMO", "BRASIL"));
    }

    @Test
    void deveImpedirQuantidadeNegativaNaPosicao() {
        Long acaoId = inserirAcao("POSNEG", "BRASIL");

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO posicoes (acao_id, quantidade, preco_medio)
                VALUES (?, -1, 10.0000)
                """, acaoId));
    }

    @Test
    void deveImpedirQuantidadeDeTransacaoMenorOuIgualAZero() {
        Long acaoId = inserirAcao("TXQTD", "BRASIL");
        Long corretoraId = inserirCorretora();

        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(acaoId, corretoraId, 0, "10.0000", chave()));
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(acaoId, corretoraId, -1, "10.0000", chave()));
    }

    @Test
    void deveImpedirValorDeTransacaoMenorOuIgualAZero() {
        Long acaoId = inserirAcao("TXVAL", "BRASIL");
        Long corretoraId = inserirCorretora();

        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(acaoId, corretoraId, 1, "0.0000", chave()));
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(acaoId, corretoraId, 1, "-0.0001", chave()));
    }

    @Test
    void deveImpedirIdempotencyKeyDuplicadaDiretamenteNoBanco() {
        Long acaoId = inserirAcao("IDEMDB", "BRASIL");
        Long corretoraId = inserirCorretora();
        String key = chave();
        inserirTransacao(acaoId, corretoraId, 1, "10.0000", key);

        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(acaoId, corretoraId, 1, "10.0000", key));
        assertEquals(1, contar("SELECT COUNT(*) FROM transacoes WHERE idempotency_key = ?", key));
    }

    @Test
    void deveAplicarTamanhosMaximosDeTickerEIdempotencyKey() {
        Long acaoId = inserirAcao("T".repeat(20), "BRASIL");
        Long corretoraId = inserirCorretora();
        inserirTransacao(acaoId, corretoraId, 1, "10.0000", "K".repeat(100));

        assertThrows(DataIntegrityViolationException.class,
                () -> inserirAcao("T".repeat(21), "BRASIL"));
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirTransacao(acaoId, corretoraId, 1, "10.0000", "K".repeat(101)));
    }

    @Test
    void devePersistirTimestampsComTimezoneELerEmUtc() {
        OffsetDateTime horarioCotacao = OffsetDateTime.parse("2024-02-15T10:30:45-03:00");
        Long acaoId = inserirAcao("TEMPO", "BRASIL", horarioCotacao);
        Long corretoraId = inserirCorretora();
        LocalDate dataOperacao = LocalDate.of(2024, 7, 10);

        service.registrarCompra(
                new TransacaoRequestDTO(acaoId, corretoraId, 1, new BigDecimal("10.0000"), dataOperacao),
                chave());

        OffsetDateTime cotacaoPersistida = jdbc.queryForObject(
                "SELECT data_hora_cotacao FROM acoes WHERE id = ?", OffsetDateTime.class, acaoId);
        OffsetDateTime transacaoPersistida = jdbc.queryForObject(
                "SELECT data_hora_transacao FROM transacoes WHERE acao_id = ?", OffsetDateTime.class, acaoId);

        assertEquals(horarioCotacao.toInstant(), cotacaoPersistida.toInstant());
        assertEquals(ZoneOffset.UTC, cotacaoPersistida.getOffset());
        assertEquals(dataOperacao.atStartOfDay(FUSO_TRANSACAO).toInstant(), transacaoPersistida.toInstant());
        assertEquals(ZoneOffset.UTC, transacaoPersistida.getOffset());
    }

    @Test
    void devePersistirResultadoRealizadoEManterNullParaCompraERegistroAntigo() {
        Long acaoId = inserirAcao("PGFIN", "BRASIL");
        Long corretoraId = inserirCorretora();
        inserirTransacao(acaoId, corretoraId, 1, "90.0000", chave());

        assertEquals(1, contar("""
                SELECT COUNT(*) FROM transacoes
                 WHERE preco_medio_operacao IS NULL AND resultado_realizado IS NULL
                """));

        service.registrarCompra(dto(acaoId, corretoraId, 15, "106.6667"), chave());
        service.registrarVenda(dto(acaoId, corretoraId, 4, "130.0000"), chave());

        assertEquals(2, contar("""
                SELECT COUNT(*) FROM transacoes
                 WHERE tipo_transacao = 'COMPRA'
                   AND preco_medio_operacao IS NULL
                   AND resultado_realizado IS NULL
                """));
        BigDecimal precoMedio = jdbc.queryForObject("""
                SELECT preco_medio_operacao FROM transacoes WHERE tipo_transacao = 'VENDA'
                """, BigDecimal.class);
        BigDecimal resultado = jdbc.queryForObject("""
                SELECT resultado_realizado FROM transacoes WHERE tipo_transacao = 'VENDA'
                """, BigDecimal.class);
        assertEquals(0, new BigDecimal("106.6667").compareTo(precoMedio));
        assertEquals(0, new BigDecimal("93.3332").compareTo(resultado));
        assertEquals(4, precoMedio.scale());
        assertEquals(4, resultado.scale());
    }

    @Test
    void deveAgregarSomenteVendasComResultadoPorMoedaNoPostgres() {
        Long acaoBrlId = inserirAcao("AGGBRL", "BRASIL");
        Long acaoUsdId = inserirAcao("AGGUSD", "ESTADOS_UNIDOS");
        Long corretoraId = inserirCorretora();

        inserirTransacaoFinanceira(acaoBrlId, corretoraId, "COMPRA", "999.0000");
        inserirTransacaoFinanceira(acaoBrlId, corretoraId, "VENDA", null);
        inserirTransacaoFinanceira(acaoBrlId, corretoraId, "VENDA", "350.0000");
        inserirTransacaoFinanceira(acaoBrlId, corretoraId, "VENDA", "-50.0000");
        inserirTransacaoFinanceira(acaoUsdId, corretoraId, "VENDA", "25.0000");

        var resumos = service.resumirCarteiraPorMoeda();

        assertEquals(2, resumos.size());
        assertEquals("BRL", resumos.get(0).moeda().name());
        assertDecimal("300.0000", resumos.get(0).resultadoRealizadoTotal());
        assertDecimal("300.0000", resumos.get(0).resultadoTotal());
        assertEquals("USD", resumos.get(1).moeda().name());
        assertDecimal("25.0000", resumos.get(1).resultadoRealizadoTotal());
        assertDecimal("25.0000", resumos.get(1).resultadoTotal());
    }

    @Test
    void deveConsolidarCriacaoInicialConcorrenteSemPerderAtualizacao() throws Exception {
        Long acaoId = inserirAcao("PGCONC1", "BRASIL");
        Long corretoraId = inserirCorretora();

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarCompra(dto(acaoId, corretoraId, 10, "10.0000"), chave()),
                () -> service.registrarCompra(dto(acaoId, corretoraId, 20, "20.0000"), chave()));

        assertTrue(resultados.stream().allMatch(resultado -> resultado == null));
        assertPosicao(acaoId, 30, "16.6667");
        assertEquals(1, contar("SELECT COUNT(*) FROM posicoes WHERE acao_id = ?", acaoId));
        assertEquals(2, contar("SELECT COUNT(*) FROM transacoes WHERE acao_id = ?", acaoId));
    }

    @Test
    void deveSerializarAtualizacoesConcorrentesDaMesmaPosicao() throws Exception {
        Long acaoId = inserirAcao("PGCONC2", "BRASIL");
        Long corretoraId = inserirCorretora();
        service.registrarCompra(dto(acaoId, corretoraId, 10, "10.0000"), chave());

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarCompra(dto(acaoId, corretoraId, 5, "10.0000"), chave()),
                () -> service.registrarCompra(dto(acaoId, corretoraId, 7, "10.0000"), chave()));

        assertTrue(resultados.stream().allMatch(resultado -> resultado == null));
        assertPosicao(acaoId, 22, "10.0000");
        assertEquals(3, contar("SELECT COUNT(*) FROM transacoes WHERE acao_id = ?", acaoId));
    }

    @Test
    void deveBloquearSegundaTransacaoComPessimisticWriteAtePrimeiroCommit() throws Exception {
        Long acaoId = inserirAcao("PGLOCK", "BRASIL");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch primeiroLockObtido = new CountDownLatch(1);
        CountDownLatch liberarPrimeiraTransacao = new CountDownLatch(1);
        CompletableFuture<Integer> pidSegundaTransacao = new CompletableFuture<>();

        try {
            Future<?> primeira = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                acaoRepository.findByIdForUpdate(acaoId).orElseThrow();
                primeiroLockObtido.countDown();
                aguardar(liberarPrimeiraTransacao);
            }));
            assertTrue(primeiroLockObtido.await(5, TimeUnit.SECONDS));

            Future<?> segunda = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                pidSegundaTransacao.complete(
                        jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                acaoRepository.findByIdForUpdate(acaoId).orElseThrow();
            }));

            int pid = pidSegundaTransacao.get(5, TimeUnit.SECONDS);
            assertTrue(aguardarLockNoPostgres(pid),
                    "A segunda transacao deveria aguardar o row lock da primeira");
            assertFalse(segunda.isDone());

            liberarPrimeiraTransacao.countDown();
            primeira.get(5, TimeUnit.SECONDS);
            segunda.get(5, TimeUnit.SECONDS);
        } finally {
            liberarPrimeiraTransacao.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void deveIgnorarMesmaChaveComMesmoPayloadERejeitarPayloadDiferente() {
        Long acaoId = inserirAcao("PGIDEM1", "BRASIL");
        Long corretoraId = inserirCorretora();
        String key = chave();
        TransacaoRequestDTO original = dto(acaoId, corretoraId, 5, "10.0000");

        service.registrarCompra(original, key);
        service.registrarCompra(original, key);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.registrarCompra(dto(acaoId, corretoraId, 6, "10.0000"), key));
        assertTrue(erro.getMessage().contains("idempot"));
        assertPosicao(acaoId, 5, "10.0000");
        assertEquals(1, contar("SELECT COUNT(*) FROM transacoes WHERE idempotency_key = ?", key));
    }

    @Test
    void deveProcessarUmaUnicaVezRequisicoesConcorrentesComMesmaChave() throws Exception {
        Long acaoId = inserirAcao("PGIDEM2", "BRASIL");
        Long corretoraId = inserirCorretora();
        String key = chave();
        TransacaoRequestDTO dto = dto(acaoId, corretoraId, 5, "10.0000");

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarCompra(dto, key),
                () -> service.registrarCompra(dto, key));

        assertTrue(resultados.stream().allMatch(resultado -> resultado == null));
        assertPosicao(acaoId, 5, "10.0000");
        assertEquals(1, contar("SELECT COUNT(*) FROM transacoes WHERE idempotency_key = ?", key));
    }

    private Long inserirAcao(String ticker, String mercado) {
        return inserirAcao(ticker, mercado, OffsetDateTime.parse("2024-01-01T12:00:00Z"));
    }

    private Long inserirAcao(String ticker, String mercado, OffsetDateTime dataHoraCotacao) {
        jdbc.update("""
                INSERT INTO acoes (ticker, nome_empresa, mercado, moeda, cotacao_atual,
                                   data_hora_cotacao, provedor_origem)
                VALUES (?, 'Empresa do teste', ?, ?, 10.0000, ?, 'teste')
                """, ticker, mercado, mercado.equals("BRASIL") ? "BRL" : "USD", dataHoraCotacao);
        return jdbc.queryForObject(
                "SELECT id FROM acoes WHERE ticker = ? AND mercado = ?", Long.class, ticker, mercado);
    }

    private Long inserirCorretora() {
        jdbc.update("""
                INSERT INTO corretoras (cnpj, razao_social, cep, logradouro, bairro, cidade, uf,
                                         situacao_cadastral, status_cvm, data_hora_cadastro)
                VALUES ('02332886000104', 'Corretora do teste', '04538133', 'Rua do teste',
                        'Bairro do teste', 'Sao Paulo', 'SP', 'ATIVA', 'REGULAR', CURRENT_TIMESTAMP)
                """);
        return jdbc.queryForObject("SELECT id FROM corretoras", Long.class);
    }

    private void inserirUsuario(String email) {
        jdbc.update("""
                INSERT INTO usuarios (nome, email, senha_hash, perfil, ativo, data_hora_cadastro)
                VALUES ('Usuario do teste', ?, '$2a$10$hashapenasparatestedebancodedados0000000000000000000',
                        'USER', TRUE, CURRENT_TIMESTAMP)
                """, email);
    }

    private void inserirTransacao(Long acaoId, Long corretoraId, int quantidade,
                                  String valorUnitario, String idempotencyKey) {
        jdbc.update("""
                INSERT INTO transacoes (usuario_id, acao_id, corretora_id, tipo_transacao, quantidade,
                                        valor_unitario, data_hora_transacao, idempotency_key)
                VALUES (?, ?, ?, 'COMPRA', ?, ?, ?, ?)
                """, usuarioId, acaoId, corretoraId, quantidade, new BigDecimal(valorUnitario),
                OffsetDateTime.parse("2024-01-01T12:00:00Z"), idempotencyKey);
    }

    private void inserirTransacao(Long proprietarioId, Long acaoId, Long corretoraId,
                                  String idempotencyKey) {
        jdbc.update("""
                INSERT INTO transacoes (usuario_id, acao_id, corretora_id, tipo_transacao, quantidade,
                                        valor_unitario, data_hora_transacao, idempotency_key)
                VALUES (?, ?, ?, 'COMPRA', 1, 10.0000, CURRENT_TIMESTAMP, ?)
                """, proprietarioId, acaoId, corretoraId, idempotencyKey);
    }

    private void inserirPosicao(Long proprietarioId, Long acaoId) {
        jdbc.update("""
                INSERT INTO posicoes (usuario_id, acao_id, quantidade, preco_medio)
                VALUES (?, ?, 1, 10.0000)
                """, proprietarioId, acaoId);
    }

    private void inserirTransacaoFinanceira(Long acaoId, Long corretoraId, String tipo,
                                             String resultadoRealizado) {
        jdbc.update("""
                INSERT INTO transacoes (usuario_id, acao_id, corretora_id, tipo_transacao, quantidade,
                                        valor_unitario, resultado_realizado, data_hora_transacao)
                VALUES (?, ?, ?, ?, 1, 10.0000, ?, ?)
                """, usuarioId, acaoId, corretoraId, tipo,
                resultadoRealizado == null ? null : new BigDecimal(resultadoRealizado),
                OffsetDateTime.parse("2024-01-01T12:00:00Z"));
    }

    private TransacaoRequestDTO dto(Long acaoId, Long corretoraId, int quantidade, String valor) {
        return new TransacaoRequestDTO(acaoId, corretoraId, quantidade, new BigDecimal(valor));
    }

    private List<Throwable> executarConcorrente(Runnable primeira, Runnable segunda) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<Throwable> primeiro = executor.submit(aguardarInicio(primeira, prontas, iniciar));
            Future<Throwable> segundo = executor.submit(aguardarInicio(segunda, prontas, iniciar));
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            return Arrays.asList(primeiro.get(15, TimeUnit.SECONDS), segundo.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Throwable> aguardarInicio(Runnable operacao, CountDownLatch prontas,
                                                CountDownLatch iniciar) {
        return () -> {
            prontas.countDown();
            iniciar.await();
            try {
                operacao.run();
                return null;
            } catch (Throwable throwable) {
                return throwable;
            }
        };
    }

    private void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout aguardando sincronizacao do teste");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Teste interrompido", e);
        }
    }

    private boolean aguardarLockNoPostgres(int pid) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        do {
            Boolean aguardandoLock = jdbc.queryForObject("""
                    SELECT wait_event_type = 'Lock'
                      FROM pg_stat_activity
                     WHERE pid = ?
                    """, Boolean.class, pid);
            if (Boolean.TRUE.equals(aguardandoLock)) {
                return true;
            }
            Thread.onSpinWait();
        } while (System.nanoTime() < limite);
        return false;
    }

    private void assertPosicao(Long acaoId, int quantidade, String precoMedio) {
        assertEquals(quantidade, jdbc.queryForObject(
                "SELECT quantidade FROM posicoes WHERE acao_id = ?", Integer.class, acaoId));
        BigDecimal atual = jdbc.queryForObject(
                "SELECT preco_medio FROM posicoes WHERE acao_id = ?", BigDecimal.class, acaoId);
        assertEquals(0, new BigDecimal(precoMedio).compareTo(atual));
    }

    private int contar(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Integer.class, parametros);
    }

    private String tipoColuna(String tabela, String coluna) {
        return jdbc.queryForObject("""
                SELECT data_type
                  FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, String.class, tabela, coluna);
    }

    private int tamanhoMaximo(String tabela, String coluna) {
        return jdbc.queryForObject("""
                SELECT character_maximum_length
                  FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, Integer.class, tabela, coluna);
    }

    private void assertTipoNumerico(String tabela, String coluna, int precisao, int escala) {
        var metadados = jdbc.queryForMap("""
                SELECT data_type, numeric_precision, numeric_scale
                  FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, tabela, coluna);
        assertEquals("numeric", metadados.get("data_type"));
        assertEquals(precisao, ((Number) metadados.get("numeric_precision")).intValue());
        assertEquals(escala, ((Number) metadados.get("numeric_scale")).intValue());
    }

    private void assertDecimal(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual));
    }

    private String chave() {
        return UUID.randomUUID().toString();
    }
}
