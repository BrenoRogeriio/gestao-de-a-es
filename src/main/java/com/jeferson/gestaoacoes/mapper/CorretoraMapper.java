package com.jeferson.gestaoacoes.mapper;

import com.jeferson.gestaoacoes.dto.CorretoraResponseDTO;
import com.jeferson.gestaoacoes.model.Corretora;
import org.springframework.stereotype.Component;

@Component
public class CorretoraMapper {

    public CorretoraResponseDTO toResponseDTO(Corretora corretora) {
        if (corretora == null) {
            return null;
        }
        return new CorretoraResponseDTO(
                corretora.getId(),
                corretora.getCnpj(),
                corretora.getRazaoSocial(),
                corretora.getNomeFantasia(),
                corretora.getEmail(),
                corretora.getTelefone(),
                corretora.getCep(),
                corretora.getLogradouro(),
                corretora.getNumero(),
                corretora.getComplemento(),
                corretora.getBairro(),
                corretora.getCidade(),
                corretora.getUf(),
                corretora.getSituacaoCadastral(),
                corretora.getStatusCvm(),
                corretora.getDataHoraCadastro()
        );
    }
}