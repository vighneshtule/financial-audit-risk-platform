package com.vighnesh.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aurexOpenAPI() {
        SecurityScheme bearerAuthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token obtained from POST /api/auth/login");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("BearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("AUREX — Financial Audit Risk Platform API")
                        .description("High-performance deterministic risk intelligence and transaction investigation REST APIs.")
                        .version("v2.2.0")
                        .contact(new Contact()
                                .name("AUREX Engineering Team")
                                .email("engineering@aurex.local"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://aurex.local/terms")))
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes("BearerAuth", bearerAuthScheme));
    }
}
