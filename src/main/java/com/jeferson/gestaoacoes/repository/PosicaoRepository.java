package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Posicao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PosicaoRepository extends JpaRepository<Posicao, Long> {
    Optional<Posicao> findByAcaoId(Long acaoId);
}