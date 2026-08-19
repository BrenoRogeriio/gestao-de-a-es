package com.jeferson.gestaoacoes.dto;

import com.jeferson.gestaoacoes.model.Mercado;
import com.jeferson.gestaoacoes.model.Moeda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AcaoResponseDTO(
        Long id,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        Moeda moeda,
        BigDecimal cotacaoAtual,
        OffsetDateTime dataHoraCotacao,
        String provedorOrigem
) {}