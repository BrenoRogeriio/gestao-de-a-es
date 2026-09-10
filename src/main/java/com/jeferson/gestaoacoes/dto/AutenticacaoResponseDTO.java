package com.jeferson.gestaoacoes.dto;

public record AutenticacaoResponseDTO(
        String token,
        String tipo,
        long expiraEmSegundos,
        UsuarioAutenticadoDTO usuario
) {}
