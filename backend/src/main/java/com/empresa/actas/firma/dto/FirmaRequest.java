package com.empresa.actas.firma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request para firmar un acta")
public record FirmaRequest(
        @NotBlank(message = "La firma es obligatoria")
        @Schema(description = "Imagen de la firma en formato Base64 (data:image/png;base64,...)", requiredMode = Schema.RequiredMode.REQUIRED)
        String firmaBase64,

        @NotBlank(message = "La foto es obligatoria")
        @Schema(description = "Foto del usuario firmando en formato Base64 (data:image/jpeg;base64,...)", requiredMode = Schema.RequiredMode.REQUIRED)
        String fotoBase64
) {}
