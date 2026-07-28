package com.empresa.actas.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para iniciar sesion")
public record LoginRequest(
        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        @Schema(description = "Nombre de usuario", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank(message = "La password es obligatoria")
        @Schema(description = "Contrasena del usuario", example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {}
