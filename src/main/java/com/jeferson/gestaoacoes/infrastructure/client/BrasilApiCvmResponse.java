package com.jeferson.gestaoacoes.infrastructure.client;

public record BrasilApiCvmResponse(
        String cnpj,
        String status,
        String type,
        String codigo_cvm
) {}
