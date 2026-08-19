package com.jeferson.gestaoacoes.dto;

import com.jeferson.gestaoacoes.model.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcaoRequestDTO(
        @NotBlank(message = "O ticker é obrigatório")
        @Size(max = 20, message = "O ticker deve ter no máximo 20 caracteres")
        String ticker,

        @NotNull(message = "O mercado é obrigatório")
        Mercado mercado
) {}