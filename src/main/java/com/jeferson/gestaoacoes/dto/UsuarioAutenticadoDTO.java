package com.jeferson.gestaoacoes.dto;

import com.jeferson.gestaoacoes.model.PerfilUsuario;

import java.time.OffsetDateTime;

public record UsuarioAutenticadoDTO(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        OffsetDateTime dataHoraCadastro
) {}
