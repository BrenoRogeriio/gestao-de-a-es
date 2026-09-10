package com.jeferson.gestaoacoes.dto;

import com.jeferson.gestaoacoes.model.Moeda;

import java.math.BigDecimal;

public record ResumoCarteiraResponseDTO(
        Moeda moeda,
        BigDecimal valorInvestidoTotal,
        BigDecimal valorAtualTotal,
        BigDecimal resultadoNaoRealizadoTotal,
        BigDecimal rentabilidadePercentual,
        BigDecimal resultadoRealizadoTotal,
        BigDecimal resultadoTotal
) {}
