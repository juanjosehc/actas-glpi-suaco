package com.empresa.actas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private String mensaje;
    private Object data;

    /**
     * Codigo de negocio opcional para automaciones del frontend (ej.
     * "CUENTA_BLOQUEADA"). Con @JsonInclude(NON_NULL) se omite de la respuesta
     * cuando es null, sin contaminar el contrato existente.
     */
    private String codigo;

    public static ErrorResponse of(String mensaje) {
        return new ErrorResponse(false, mensaje, null, null);
    }

    /** Respuesta de error con codigo de negocio (omitido si null en el JSON). */
    public static ErrorResponse ofConCodigo(String mensaje, String codigo) {
        return codigo == null || codigo.isBlank()
                ? of(mensaje)
                : new ErrorResponse(false, mensaje, null, codigo);
    }

    public static <T> ErrorResponse of(String mensaje, T data) {
        return new ErrorResponse(false, mensaje, data, null);
    }

    public static ErrorResponse ok(String mensaje) {
        return new ErrorResponse(true, mensaje, null, null);
    }

    public static <T> ErrorResponse ok(String mensaje, T data) {
        return new ErrorResponse(true, mensaje, data, null);
    }
}
