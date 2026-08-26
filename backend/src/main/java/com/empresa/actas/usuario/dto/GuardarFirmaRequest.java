package com.empresa.actas.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request para registrar o reemplazar la firma permanente del tecnico")
public record GuardarFirmaRequest(
        @NotBlank(message = "La firma es obligatoria")
        @Schema(description = "Imagen PNG de la firma en formato Base64 (sin prefijo data:)", requiredMode = Schema.RequiredMode.REQUIRED)
        String firmaBase64
) {}