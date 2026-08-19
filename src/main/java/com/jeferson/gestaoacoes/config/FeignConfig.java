package com.jeferson.gestaoacoes.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.jeferson.gestaoacoes.infrastructure.client")
public class FeignConfig {
}