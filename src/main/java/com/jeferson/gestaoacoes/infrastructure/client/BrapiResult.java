package com.jeferson.gestaoacoes.infrastructure.client;

import java.math.BigDecimal;

public record BrapiResult(
        String symbol,
        String shortName,
        String currency,
        BigDecimal regularMarketPrice,
        String regularMarketTime
) {}