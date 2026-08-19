package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.CorretoraRequestDTO;
import com.jeferson.gestaoacoes.dto.CorretoraResponseDTO;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.infrastructure.client.*;
import com.jeferson.gestaoacoes.mapper.CorretoraMapper;
import com.jeferson.gestaoacoes.model.Corretora;
import com.jeferson.gestaoacoes.repository.CorretoraRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class CorretoraService {

    private final CorretoraRepository repository;
    private final CorretoraMapper mapper;
    private final BrasilApiClient brasilApiClient;
    private final ViaCepClient viaCepClient;

    public CorretoraService(CorretoraRepository repository, CorretoraMapper mapper,
                            BrasilApiClient brasilApiClient, ViaCepClient viaCepClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.brasilApiClient = brasilApiClient;
        this.viaCepClient = viaCepClient;
    }

    @Transactional
    public CorretoraResponseDTO cadastrar(CorretoraRequestDTO dto) {
        // 1. Validar duplicidade de CNPJ
        if (repository.existsByCnpj(dto.cnpj())) {
            throw new RegraNegocioException("Corretora já cadastrada com este CNPJ.");
        }

        // 2. Buscar e validar CNPJ na BrasilAPI
        BrasilApiCnpjResponse dadosCnpj;
        try {
            dadosCnpj = brasilApiClient.consultarCnpj(dto.cnpj());
        } catch (FeignException e) {
            throw new RegraNegocioException("CNPJ inválido ou não encontrado na Receita Federal.");
        }

        // 3. Validar se é uma Corretora autorizada pela CVM (RN03 - Rejeição)
        BrasilApiCvmResponse dadosCvm;
        try {
            dadosCvm = brasilApiClient.validarCorretoraCvm(dto.cnpj());
        } catch (FeignException e) {
            throw new RegraNegocioException("Instituição não validada/autorizada pela CVM para atuar como corretora.");
        }

        // 4. Buscar e validar o CEP no ViaCEP
        ViaCepResponse dadosCep;
        try {
            dadosCep = viaCepClient.consultarCep(dto.cep());
            if (Boolean.TRUE.equals(dadosCep.erro())) {
                throw new RegraNegocioException("CEP inexistente.");
            }
        } catch (FeignException e) {
            throw new RegraNegocioException("Erro ao validar o CEP informado.");
        }

        // 5. Montar a entidade agregando os dados externos (RN02)
        Corretora corretora = new Corretora();
        corretora.setCnpj(dto.cnpj());
        corretora.setRazaoSocial(dadosCnpj.razao_social());
        corretora.setNomeFantasia(dadosCnpj.nome_fantasia() != null ? dadosCnpj.nome_fantasia() : dadosCnpj.razao_social());
        corretora.setEmail(dto.email());
        corretora.setTelefone(dto.telefone());

        // Dados do Endereço
        corretora.setCep(dadosCep.cep().replace("-", ""));
        corretora.setLogradouro(dadosCep.logradouro());
        corretora.setBairro(dadosCep.bairro());
        corretora.setCidade(dadosCep.localidade());
        corretora.setUf(dadosCep.uf());
        corretora.setNumero(dto.numero());
        corretora.setComplemento(dto.complemento());

        // Status e Metadados
        corretora.setSituacaoCadastral(dadosCnpj.descricao_situacao_cadastral());
        corretora.setStatusCvm(dadosCvm.status());
        corretora.setDataHoraCadastro(OffsetDateTime.now());

        // 6. Salvar no banco e retornar
        Corretora corretoraSalva = repository.save(corretora);
        return mapper.toResponseDTO(corretoraSalva);
    }
}