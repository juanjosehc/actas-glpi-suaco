package com.empresa.actas.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response despues de un login exitoso")
public record LoginResponse(
        @Schema(description = "Token JWT para autenticacion", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Nombre de usuario", example = "admin")
        String username,

        @Schema(description = "Rol del usuario", example = "ADMINISTRADOR")
        String role
) {}
