package com.empresa.actas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS y recursos estáticos.
 *
 * Orígenes permitidos:
 * - http://127.0.0.1       → Frontend servido localmente.
 * - http://localhost        → Variante localhost.
 * - http://127.0.0.1:5500  → Live Server de VS Code.
 * - http://localhost:5500   → Live Server de VS Code (variante).
 * - http://127.0.0.1:8080   → Servidor alternativo.
 * - http://localhost:8080    → Servidor alternativo (variante).
 *
 * Métodos permitidos: todos (*).
 * Headers permitidos: todos (*).
 * Headers expuestos: Content-Disposition (necesario para descarga de ZIP).
 */
@Configuration
public class CorsConfig {

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    @Bean
    public WebMvcConfigurer webConfig() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://127.0.0.1",
                                "http://localhost",
                                "http://127.0.0.1:5500",
                                "http://localhost:5500",
                                "http://127.0.0.1:8080",
                                "http://localhost:8080"
                        )
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Disposition");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + uploadsDir + "/");
            }
        };
    }
}
