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

    public static ErrorResponse of(String mensaje) {
        return new ErrorResponse(false, mensaje, null);
    }

    public static <T> ErrorResponse of(String mensaje, T data) {
        return new ErrorResponse(false, mensaje, data);
    }

    public static ErrorResponse ok(String mensaje) {
        return new ErrorResponse(true, mensaje, null);
    }

    public static <T> ErrorResponse ok(String mensaje, T data) {
        return new ErrorResponse(true, mensaje, data);
    }
}
