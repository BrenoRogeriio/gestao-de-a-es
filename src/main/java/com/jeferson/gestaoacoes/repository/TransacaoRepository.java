package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    Optional<Transacao> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select new com.jeferson.gestaoacoes.repository.ResultadoRealizadoPorMoeda(
                t.acao.moeda, sum(t.resultadoRealizado)
            )
            from Transacao t
            where t.tipoTransacao = com.jeferson.gestaoacoes.model.TipoTransacao.VENDA
              and t.resultadoRealizado is not null
            group by t.acao.moeda
            """)
    List<ResultadoRealizadoPorMoeda> somarResultadoRealizadoPorMoeda();
}
