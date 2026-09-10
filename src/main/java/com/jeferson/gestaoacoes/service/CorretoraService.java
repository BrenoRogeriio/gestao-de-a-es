package com.jeferson.gestaoacoes.service;

import com.jeferson.gestaoacoes.dto.CorretoraRequestDTO;
import com.jeferson.gestaoacoes.dto.CorretoraResponseDTO;
import com.jeferson.gestaoacoes.exception.ProvedorExternoIndisponivelException;
import com.jeferson.gestaoacoes.exception.RegraNegocioException;
import com.jeferson.gestaoacoes.exception.RespostaExternaInvalidaException;
import com.jeferson.gestaoacoes.infrastructure.client.*;
import com.jeferson.gestaoacoes.mapper.CorretoraMapper;
import com.jeferson.gestaoacoes.model.Corretora;
import com.jeferson.gestaoacoes.repository.CorretoraRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Locale;

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
        String cnpj = normalizarCnpj(dto.cnpj());
        if (!cnpjValido(cnpj)) {
            throw new RegraNegocioException("CNPJ inválido.");
        }

        if (repository.existsByCnpj(cnpj)) {
            throw new RegraNegocioException("Corretora já cadastrada com este CNPJ.");
        }

        BrasilApiCnpjResponse dadosCnpj = consultarEValidarCnpj(cnpj);
        BrasilApiCvmResponse dadosCvm = consultarEValidarCvm(cnpj);
        ViaCepResponse dadosCep = consultarEValidarCep(dto.cep());

        // 5. Montar a entidade agregando os dados externos (RN02)
        Corretora corretora = new Corretora();
        corretora.setCnpj(cnpj);
        corretora.setRazaoSocial(dadosCnpj.razao_social().trim());
        corretora.setNomeFantasia(textoObrigatorio(dadosCnpj.nome_fantasia())
                ? dadosCnpj.nome_fantasia().trim()
                : dadosCnpj.razao_social().trim());
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
        corretora.setSituacaoCadastral(dadosCnpj.descricao_situacao_cadastral().trim());
        corretora.setStatusCvm(dadosCvm.status().trim());
        corretora.setDataHoraCadastro(OffsetDateTime.now());

        // 6. Salvar no banco e retornar
        Corretora corretoraSalva = repository.save(corretora);
        return mapper.toResponseDTO(corretoraSalva);
    }

    private BrasilApiCnpjResponse consultarEValidarCnpj(String cnpj) {
        BrasilApiCnpjResponse resposta;
        try {
            resposta = brasilApiClient.consultarCnpj(cnpj);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new RegraNegocioException("CNPJ não encontrado na Receita Federal.");
            }
            throw new ProvedorExternoIndisponivelException(
                    "A BrasilAPI está indisponível para consulta de CNPJ.", e);
        }

        if (resposta == null
                || !textoObrigatorio(resposta.cnpj())
                || !textoObrigatorio(resposta.razao_social())
                || !textoObrigatorio(resposta.descricao_situacao_cadastral())) {
            throw new RespostaExternaInvalidaException("A BrasilAPI retornou dados incompletos para o CNPJ.");
        }
        if (!cnpj.equals(normalizarCnpj(resposta.cnpj()))) {
            throw new RespostaExternaInvalidaException("A BrasilAPI retornou um CNPJ diferente do solicitado.");
        }
        if (!"ATIVA".equals(normalizarTexto(resposta.descricao_situacao_cadastral()))) {
            throw new RegraNegocioException("O CNPJ não possui situação cadastral ativa.");
        }
        return resposta;
    }

    private BrasilApiCvmResponse consultarEValidarCvm(String cnpj) {
        BrasilApiCvmResponse resposta;
        try {
            resposta = brasilApiClient.validarCorretoraCvm(cnpj);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new RegraNegocioException("CNPJ não encontrado no cadastro de corretoras da CVM.");
            }
            throw new ProvedorExternoIndisponivelException(
                    "A BrasilAPI está indisponível para consulta da CVM.", e);
        }

        if (resposta == null
                || !textoObrigatorio(resposta.cnpj())
                || !textoObrigatorio(resposta.status())
                || !textoObrigatorio(resposta.codigo_cvm())) {
            throw new RespostaExternaInvalidaException("A BrasilAPI retornou dados incompletos do registro CVM.");
        }
        if (!cnpj.equals(normalizarCnpj(resposta.cnpj()))) {
            throw new RespostaExternaInvalidaException("A BrasilAPI retornou um registro CVM de outro CNPJ.");
        }
        if (textoObrigatorio(resposta.type())) {
            String tipo = normalizarTexto(resposta.type());
            if (!"CORRETORA".equals(tipo) && !"CORRETORAS".equals(tipo)) {
                throw new RegraNegocioException("O registro CVM não corresponde a uma corretora.");
            }
        }
        if (!"EM FUNCIONAMENTO NORMAL".equals(normalizarTexto(resposta.status()))) {
            throw new RegraNegocioException("A corretora não está em funcionamento normal segundo a CVM.");
        }
        return resposta;
    }

    private ViaCepResponse consultarEValidarCep(String cep) {
        ViaCepResponse resposta;
        try {
            resposta = viaCepClient.consultarCep(cep);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new RegraNegocioException("CEP inexistente.");
            }
            throw new ProvedorExternoIndisponivelException("O ViaCEP está indisponível no momento.", e);
        }

        if (resposta == null) {
            throw new RespostaExternaInvalidaException("O ViaCEP retornou uma resposta vazia.");
        }
        if (Boolean.TRUE.equals(resposta.erro())) {
            throw new RegraNegocioException("CEP inexistente.");
        }
        String cepResposta = resposta.cep() == null ? "" : resposta.cep().replaceAll("\\D", "");
        if (!cep.equals(cepResposta)
                || !textoObrigatorio(resposta.logradouro())
                || !textoObrigatorio(resposta.bairro())
                || !textoObrigatorio(resposta.localidade())
                || resposta.uf() == null
                || !resposta.uf().trim().matches("(?i)[A-Z]{2}")) {
            throw new RespostaExternaInvalidaException("O ViaCEP retornou dados incompletos ou incompatíveis.");
        }
        return resposta;
    }

    private static boolean textoObrigatorio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String normalizarTexto(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizarCnpj(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("\\D", "");
    }

    private static boolean cnpjValido(String cnpj) {
        if (!cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int[] pesosPrimeiroDigito = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesosSegundoDigito = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return calcularDigito(cnpj, pesosPrimeiroDigito) == Character.digit(cnpj.charAt(12), 10)
                && calcularDigito(cnpj, pesosSegundoDigito) == Character.digit(cnpj.charAt(13), 10);
    }

    private static int calcularDigito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.digit(cnpj.charAt(i), 10) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
