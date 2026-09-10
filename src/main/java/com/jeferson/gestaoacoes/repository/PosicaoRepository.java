package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Posicao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PosicaoRepository extends JpaRepository<Posicao, Long> {
    Optional<Posicao> findByUsuarioIdAndAcaoId(Long usuarioId, Long acaoId);

    List<Posicao> findAllByUsuarioIdAndQuantidadeGreaterThan(Long usuarioId, Integer quantidade);
}
