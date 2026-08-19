package com.jeferson.gestaoacoes.infrastructure.client;

public record BrasilApiCnpjResponse(
        String cnpj,
        String razao_social,
        String nome_fantasia,
        String descricao_situacao_cadastral
) {}