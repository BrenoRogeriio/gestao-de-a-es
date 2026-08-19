package com.jeferson.gestaoacoes.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "brasilApiClient", url = "https://brasilapi.com.br/api")
public interface BrasilApiClient {

    @GetMapping("/cnpj/v1/{cnpj}")
    BrasilApiCnpjResponse consultarCnpj(@PathVariable("cnpj") String cnpj);

    @GetMapping("/cvm/corretoras/v1/{cnpj}")
    BrasilApiCvmResponse validarCorretoraCvm(@PathVariable("cnpj") String cnpj);
}