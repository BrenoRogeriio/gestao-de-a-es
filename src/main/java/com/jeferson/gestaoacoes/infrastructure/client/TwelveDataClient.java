package com.jeferson.gestaoacoes.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "twelveDataClient", url = "${app.market-data.twelve-data.url:https://api.twelvedata.com}")
public interface TwelveDataClient {

    @GetMapping("/quote")
    TwelveDataResponse consultarCotacao(
            @RequestParam("symbol") String symbol,
            @RequestParam("apikey") String apiKey
    );
}