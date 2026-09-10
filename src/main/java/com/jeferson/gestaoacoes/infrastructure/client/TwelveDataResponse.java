package com.jeferson.gestaoacoes.infrastructure.client;

import java.math.BigDecimal;

public record TwelveDataResponse(
        String symbol,
        String name,
        String currency,
        BigDecimal close,
        Long timestamp,
        String exchange,
        Integer code,
        String message,
        String status
) {}
