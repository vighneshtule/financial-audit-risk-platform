package com.vighnesh.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aurexOpenAPI() {
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
                                .url("https://aurex.local/terms")));
    }
}
