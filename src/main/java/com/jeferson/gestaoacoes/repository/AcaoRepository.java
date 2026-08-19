package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Acao;
import com.jeferson.gestaoacoes.model.Mercado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    // Busca e validação considerando a chave composta sugerida pela arquitetura
    Optional<Acao> findByTickerAndMercado(String ticker, Mercado mercado);

    boolean existsByTickerAndMercado(String ticker, Mercado mercado);

    List<Acao> findByTicker(String ticker);
}