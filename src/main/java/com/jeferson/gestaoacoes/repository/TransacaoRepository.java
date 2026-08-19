package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
}