package com.empresa.actas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "Bearer";

    // SEC-110: el contacto de la documentacion no lleva un correo personal
    // hardcodeado. Vacio por defecto; el equipo puede setear un buzón funcional
    // por entorno (APP_DOCUMENTACION_CONTACTO_EMAIL) si lo considera.
    @Value("${app.documentacion.contacto-email:}")
    private String contactoEmail;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Actas GLPI API")
                        .version("1.0.0")
                        .description("API del sistema de generacion de actas GLPI con firma digital")
                        .contact(new Contact()
                                .name("Equipo Actas GLPI")
                                .email(contactoEmail)))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER,
                                new SecurityScheme()
                                        .name(BEARER)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
