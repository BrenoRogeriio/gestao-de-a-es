package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.TransacaoRequestDTO;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:carteira-concorrencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
class CarteiraConcorrenciaIntegrationTest {

    @Autowired
    private CarteiraService service;

    @Autowired
    private JdbcTemplate jdbc;

    private Long corretoraId;

    @BeforeEach
    void limparOperacoes() {
        jdbc.update("DELETE FROM transacoes");
        jdbc.update("DELETE FROM posicoes");
        jdbc.update("DELETE FROM acoes");
        jdbc.update("DELETE FROM corretoras");
        jdbc.update("""
                INSERT INTO corretoras (cnpj, razao_social, cep, logradouro, bairro, cidade, uf,
                                         situacao_cadastral, status_cvm, data_hora_cadastro)
                VALUES ('02332886000104', 'Corretora do teste', '04538133', 'Rua do teste',
                        'Bairro do teste', 'Sao Paulo', 'SP', 'ATIVA', 'REGULAR', CURRENT_TIMESTAMP)
                """);
        corretoraId = jdbc.queryForObject("SELECT MIN(id) FROM corretoras", Long.class);
    }

    @Test
    void deveConsolidarDuasPrimeirasComprasConcorrentesSemPerderAtualizacao() throws Exception {
        Long acaoId = inserirAcao("CONC1");

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarCompra(dto(acaoId, 10, "10.0000"), chave()),
                () -> service.registrarCompra(dto(acaoId, 20, "20.0000"), chave()));

        assertTrue(resultados.stream().allMatch(resultado -> resultado == null));
        assertPosicao(acaoId, 30, "16.6667");
        assertEquals(1, contar("SELECT COUNT(*) FROM posicoes WHERE acao_id = ?", acaoId));
        assertEquals(2, contar("SELECT COUNT(*) FROM transacoes WHERE acao_id = ?", acaoId));
    }

    @Test
    void deveImpedirQueDuasVendasConcorrentesUltrapassemOSaldo() throws Exception {
        Long acaoId = inserirAcao("CONC2");
        service.registrarCompra(dto(acaoId, 10, "10.0000"), chave());

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarVenda(dto(acaoId, 7, "12.0000"), chave()),
                () -> service.registrarVenda(dto(acaoId, 7, "12.0000"), chave()));

        assertEquals(1, resultados.stream().filter(resultado -> resultado == null).count());
        Throwable rejeicao = resultados.stream().filter(resultado -> resultado != null).findFirst().orElseThrow();
        assertInstanceOf(RegraNegocioException.class, rejeicao);
        assertPosicao(acaoId, 3, "10.0000");
        assertEquals(1, contar("""
                SELECT COUNT(*) FROM transacoes WHERE acao_id = ? AND tipo_transacao = 'VENDA'
                """, acaoId));
    }

    @Test
    void deveSerializarCompraEVendaConcorrentesMantendoEstadoValido() throws Exception {
        Long acaoId = inserirAcao("CONC3");
        service.registrarCompra(dto(acaoId, 10, "10.0000"), chave());

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarCompra(dto(acaoId, 5, "20.0000"), chave()),
                () -> service.registrarVenda(dto(acaoId, 12, "15.0000"), chave()));

        assertNull(resultados.get(0));
        Integer quantidade = jdbc.queryForObject(
                "SELECT quantidade FROM posicoes WHERE acao_id = ?", Integer.class, acaoId);
        assertTrue(quantidade == 3 || quantidade == 15);
        assertPosicao(acaoId, quantidade, "13.3333");
        int vendas = contar("""
                SELECT COUNT(*) FROM transacoes WHERE acao_id = ? AND tipo_transacao = 'VENDA'
                """, acaoId);
        assertEquals(quantidade == 3 ? 1 : 0, vendas);
        if (quantidade == 15) {
            assertInstanceOf(RegraNegocioException.class, resultados.get(1));
        } else {
            assertNull(resultados.get(1));
        }
    }

    @Test
    void deveProcessarUmaUnicaVezRequisicoesConcorrentesComMesmaChave() throws Exception {
        Long acaoId = inserirAcao("CONC4");
        String idempotencyKey = chave();

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarCompra(dto(acaoId, 5, "10.0000"), idempotencyKey),
                () -> service.registrarCompra(dto(acaoId, 5, "10.0000"), idempotencyKey));

        assertTrue(resultados.stream().allMatch(resultado -> resultado == null));
        assertPosicao(acaoId, 5, "10.0000");
        assertEquals(1, contar("SELECT COUNT(*) FROM transacoes WHERE idempotency_key = ?", idempotencyKey));
    }

    @Test
    void deveManterRequisicoesComChavesDiferentes() {
        Long acaoId = inserirAcao("CONC5");

        service.registrarCompra(dto(acaoId, 5, "10.0000"), chave());
        service.registrarCompra(dto(acaoId, 5, "20.0000"), chave());

        assertPosicao(acaoId, 10, "15.0000");
        assertEquals(2, contar("SELECT COUNT(*) FROM transacoes WHERE acao_id = ?", acaoId));
    }

    @Test
    void devePermitirOperacoesConcorrentesEmAcoesDiferentes() throws Exception {
        Long primeiraAcao = inserirAcao("CONC6");
        Long segundaAcao = inserirAcao("CONC7");

        List<Throwable> resultados = executarConcorrente(
                () -> service.registrarCompra(dto(primeiraAcao, 2, "10.0000"), chave()),
                () -> service.registrarCompra(dto(segundaAcao, 3, "20.0000"), chave()));

        assertTrue(resultados.stream().allMatch(resultado -> resultado == null));
        assertPosicao(primeiraAcao, 2, "10.0000");
        assertPosicao(segundaAcao, 3, "20.0000");
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

    private Callable<Throwable> aguardarInicio(Runnable operacao, CountDownLatch prontas, CountDownLatch iniciar) {
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

    private Long inserirAcao(String ticker) {
        jdbc.update("""
                INSERT INTO acoes (ticker, nome_empresa, mercado, moeda, cotacao_atual,
                                   data_hora_cotacao, provedor_origem)
                VALUES (?, 'Empresa', 'BRASIL', 'BRL', 10.0000, CURRENT_TIMESTAMP, 'teste')
                """, ticker);
        return jdbc.queryForObject("SELECT id FROM acoes WHERE ticker = ?", Long.class, ticker);
    }

    private TransacaoRequestDTO dto(Long acaoId, int quantidade, String valor) {
        return new TransacaoRequestDTO(acaoId, corretoraId, quantidade, new BigDecimal(valor));
    }

    private String chave() {
        return UUID.randomUUID().toString();
    }

    private int contar(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Integer.class, parametros);
    }

    private void assertPosicao(Long acaoId, int quantidade, String precoMedio) {
        assertEquals(quantidade, jdbc.queryForObject(
                "SELECT quantidade FROM posicoes WHERE acao_id = ?", Integer.class, acaoId));
        BigDecimal atual = jdbc.queryForObject(
                "SELECT preco_medio FROM posicoes WHERE acao_id = ?", BigDecimal.class, acaoId);
        assertEquals(0, new BigDecimal(precoMedio).compareTo(atual));
    }
}
