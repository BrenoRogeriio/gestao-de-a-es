package com.jeferson.gestaoacoes.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRequestDTO(
        @NotNull(message = "O ID da ação é obrigatório")
        Long acaoId,

        @NotNull(message = "O ID da corretora é obrigatório")
        Long corretoraId,

        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "O preço de execução é obrigatório")
        @Positive(message = "O preço de execução deve ser maior que zero")
        @Digits(integer = 15, fraction = 4,
                message = "O preço de execução deve ter no máximo 15 dígitos inteiros e 4 casas decimais")
        BigDecimal valorUnitario,

        LocalDate data
) {
    public TransacaoRequestDTO(Long acaoId, Long corretoraId, Integer quantidade, BigDecimal valorUnitario) {
        this(acaoId, corretoraId, quantidade, valorUnitario, null);
    }
}
