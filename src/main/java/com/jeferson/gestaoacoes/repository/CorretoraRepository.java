package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Corretora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    // Busca pelo CNPJ, ignorando a máscara conforme a RN01
    Optional<Corretora> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}