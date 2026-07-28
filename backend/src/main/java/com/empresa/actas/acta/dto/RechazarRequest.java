package com.empresa.actas.acta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para rechazar un acta firmada")
public record RechazarRequest(
        @Size(max = 500, message = "La observacion no puede exceder 500 caracteres")
        @Schema(description = "Motivo del rechazo", example = "Firma no corresponde al usuario identificado")
        String observacion
) {}
