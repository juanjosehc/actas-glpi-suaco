package com.empresa.actas.firma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para rechazar un acta desde el portal publico de firma")
public record FirmaRechazoRequest(

        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
        @Schema(description = "Motivo por el cual el usuario rechaza el acta", example = "No corresponde a mi equipo")
        String motivo
) {}
