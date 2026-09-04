package com.empresa.actas.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Confirmacion de la recuperacion: token del correo + nueva contrasena.
 */
@Schema(description = "Request para confirmar la recuperacion de contrasena con el token del correo")
public record ConfirmarRecuperacionRequest(

        @NotBlank(message = "El token de recuperacion es obligatorio")
        @Schema(description = "Token recibido por correo", example = "9f4e2c6a-1b7d-4e5f-8a9b-0c1d2e3f4a5b", requiredMode = Schema.RequiredMode.REQUIRED)
        String token,

        // SEC-016: misma politica de contrasena que registro y creacion.
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 128, message = "La nueva contrasena debe tener entre 8 y 128 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,128}$",
                message = "La nueva contrasena debe incluir al menos una mayuscula, una minuscula, un numero y un caracter especial")
        @Schema(description = "Nueva contrasena (minimo 8 caracteres: mayuscula, minuscula, numero y especial)", example = "ClaveNueva123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String nuevaPassword
) {}