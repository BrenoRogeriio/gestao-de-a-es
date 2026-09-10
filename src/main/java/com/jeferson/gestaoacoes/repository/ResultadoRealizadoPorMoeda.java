package com.jeferson.gestaoacoes.repository;

import com.jeferson.gestaoacoes.model.Moeda;

import java.math.BigDecimal;

public record ResultadoRealizadoPorMoeda(Moeda moeda, BigDecimal valor) {
}
