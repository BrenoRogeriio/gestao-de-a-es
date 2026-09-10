package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Acao;
import com.jeferson.gestaoacoes.model.Mercado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acao a where a.id = :id")
    Optional<Acao> findByIdForUpdate(@Param("id") Long id);

    // Busca e validação considerando a chave composta sugerida pela arquitetura
    Optional<Acao> findByTickerAndMercado(String ticker, Mercado mercado);

    boolean existsByTickerAndMercado(String ticker, Mercado mercado);

    List<Acao> findByTicker(String ticker);
}
