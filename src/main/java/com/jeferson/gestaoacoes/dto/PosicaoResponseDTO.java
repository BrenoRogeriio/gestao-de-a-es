package com.jeferson.gestaoacoes.dto;

import com.jeferson.gestaoacoes.model.Mercado;
import com.jeferson.gestaoacoes.model.Moeda;

import java.math.BigDecimal;

public record PosicaoResponseDTO(
        Long acaoId,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        Moeda moeda,
        Integer quantidade,
        BigDecimal precoMedio,
        BigDecimal cotacaoAtual,
        BigDecimal valorInvestido,
        BigDecimal valorAtual,
        BigDecimal resultadoNaoRealizado,
        BigDecimal rentabilidadePercentual,
        BigDecimal saldoTotalAtual
) {}
