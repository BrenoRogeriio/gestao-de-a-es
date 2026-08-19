package com.jeferson.gestaoacoes.dto;

import java.time.OffsetDateTime;

public record CorretoraResponseDTO(
        Long id,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String email,
        String telefone,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String situacaoCadastral,
        String statusCvm,
        OffsetDateTime dataHoraCadastro
) {}