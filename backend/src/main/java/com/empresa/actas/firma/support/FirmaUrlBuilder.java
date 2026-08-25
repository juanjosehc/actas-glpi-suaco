package com.empresa.actas.firma.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Construye la URL publica del portal de firma a partir de
 * {@code app.firma-url-base}. Nunca se usa localhost.
 */
@Component
public class FirmaUrlBuilder {

    @Value("${app.firma-url-base:}")
    private String firmaUrlBase;

    public String construir(String token) {
        if (firmaUrlBase == null || firmaUrlBase.isBlank()) {
            return "/firma.html?token=" + token;
        }
        return firmaUrlBase.replaceAll("/+$", "") + "/firma.html?token=" + token;
    }
}