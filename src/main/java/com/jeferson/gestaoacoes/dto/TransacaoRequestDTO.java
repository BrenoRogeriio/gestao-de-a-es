package com.jeferson.gestaoacoes.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransacaoRequestDTO(
        @NotNull(message = "O ID da ação é obrigatório")
        Long acaoId,

        @NotNull(message = "O ID da corretora é obrigatório")
        Long corretoraId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1")
        Integer quantidade,

        @NotNull(message = "O preço de execução é obrigatório")
        BigDecimal valorUnitario
) {}