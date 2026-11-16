package com.nexocrm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI nexoCrmOpenApi() {
        return new OpenAPI().info(new Info()
                .title("NexoCRM API")
                .version("1.0.0")
                .description("API REST del CRM de ventas: clientes, oportunidades, actividades, "
                        + "analítica e integraciones (CSV, Excel, tasas de cambio y webhooks).")
                .contact(new Contact().name("jgbeli34").url("https://github.com/jgbeli34"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
