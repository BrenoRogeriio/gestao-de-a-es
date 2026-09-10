package com.jeferson.gestaoacoes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record LoginRequestDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
    public LoginRequestDTO {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
