package com.jeferson.gestaoacoes.dto;

import java.math.BigDecimal;

public record PosicaoResponseDTO(
        String ticker,
        String nomeEmpresa,
        Integer quantidade,
        BigDecimal precoMedio,
        BigDecimal cotacaoAtual,
        BigDecimal rentabilidadePercentual,
        BigDecimal saldoTotalAtual
) {}