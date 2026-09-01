package com.empresa.actas.service;

import com.empresa.actas.exception.GlpiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Cliente HTTP compartido para la integracion con GLPI.
 *
 * Centraliza los timeouts de la integracion:
 * - connectTimeout: limite para establecer la conexion TCP.
 * - requestTimeout global: limite para que la respuesta completa llegue
 *   (el HttpClient del JDK no expone un request timeout nativo; se aplica
 *   con sendAsync().orTimeout()).
 *
 * Un fallo de red o un timeout se traduce en {@link GlpiException} con un
 * mensaje claro para el usuario, nunca en retorno vacio silencioso.
 */
@Component
public class GlpiHttpClient {

    private final HttpClient client;
    private final long requestTimeoutMs;

    public GlpiHttpClient(
            @Value("${glpi.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${glpi.request-timeout-ms:15000}") long requestTimeoutMs) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public HttpResponse<String> enviar(HttpRequest request) {
        try {
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            if (causa instanceof TimeoutException) {
                throw new GlpiException("GLPI no respondio en " + requestTimeoutMs + " ms. Verifique la disponibilidad del servidor.");
            }
            throw new GlpiException("Fallo de conexion con GLPI: " + causa.getMessage());
        }
    }
}