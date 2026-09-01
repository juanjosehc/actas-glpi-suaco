package com.empresa.actas.exception;

/**
 * Error de integracion con GLPI (timeout, red, autenticacion o respuesta
 * invalida). Se traduce en HTTP 502 con un mensaje claro para el usuario.
 */
public class GlpiException extends RuntimeException {

    public GlpiException(String mensaje) {
        super(mensaje);
    }
}