package com.jeferson.gestaoacoes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CorretoraRequestDTO(
        @NotBlank(message = "O CNPJ é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter exatamente 14 dígitos numéricos")
        String cnpj,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter exatamente 8 dígitos numéricos")
        String cep,

        @Size(max = 50)
        String numero,

        @Size(max = 255)
        String complemento,

        @Email(message = "Email com formato inválido")
        @Size(max = 255)
        String email,

        @Size(max = 50)
        String telefone
) {}