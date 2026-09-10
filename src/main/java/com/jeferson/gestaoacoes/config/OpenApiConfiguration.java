package com.jeferson.gestaoacoes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI gestaoAcoesOpenApi() {
        SecurityScheme bearerJwt = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Informe o JWT obtido em /auth/login ou /auth/register.");

        return new OpenAPI()
                .info(new Info()
                        .title("Gestão de Ações API")
                        .description("API de carteira de ações com autenticação JWT e isolamento por usuário."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerJwt))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
