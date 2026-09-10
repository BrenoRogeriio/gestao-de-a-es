package com.jeferson.gestaoacoes.dto;

import com.jeferson.gestaoacoes.model.Mercado;
import com.jeferson.gestaoacoes.model.Moeda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record HistoricoResponseDTO(
        Long acaoId,
        String tipo,
        String ticker,
        Mercado mercado,
        Moeda moeda,
        String corretoraCnpj,
        Integer quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        BigDecimal precoMedioOperacao,
        BigDecimal resultadoRealizado,
        BigDecimal rentabilidadeRealizada,
        OffsetDateTime data,
        LocalDate dataOperacao
) {}
