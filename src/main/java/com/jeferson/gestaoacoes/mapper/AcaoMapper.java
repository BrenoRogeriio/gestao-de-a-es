package com.jeferson.gestaoacoes.mapper;

import com.jeferson.gestaoacoes.dto.AcaoResponseDTO;
import com.jeferson.gestaoacoes.model.Acao;
import org.springframework.stereotype.Component;

@Component
public class AcaoMapper {

    public AcaoResponseDTO toResponseDTO(Acao acao) {
        if (acao == null) {
            return null;
        }
        return new AcaoResponseDTO(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                acao.getCotacaoAtual(),
                acao.getDataHoraCotacao(),
                acao.getProvedorOrigem()
        );
    }
}