package com.jeferson.gestaoacoes.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HistoricoResponseDTO(
        String tipo,
        String ticker,
        String corretoraCnpj,
        Integer quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        OffsetDateTime data
) {}