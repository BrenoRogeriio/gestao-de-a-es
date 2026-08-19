package com.jeferson.gestaoacoes.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "brapiClient", url = "${app.market-data.brapi.url:https://brapi.dev/api}")
public interface BrapiClient {

    @GetMapping("/quote/{ticker}")
    BrapiResponse consultarCotacao(
            @PathVariable("ticker") String ticker,
            @RequestParam("token") String token
    );
}