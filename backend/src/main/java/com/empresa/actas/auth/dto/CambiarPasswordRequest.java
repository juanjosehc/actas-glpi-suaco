package com.empresa.actas.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contrasena del usuario autenticado (autoservicio).
 * El frontend valida la confirmacion; el backend solo exige la correcta
 * validacion de la anterior y la politica SEC-016 para la nueva.
 */
@Schema(description = "Request para cambiar la contrasena del usuario autenticado")
public record CambiarPasswordRequest(

        @NotBlank(message = "La contrasena actual es obligatoria")
        @Schema(description = "Contrasena actual", example = "Clave123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String passwordActual,

        // SEC-016: misma politica de contrasena que registro y creacion.
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 128, message = "La nueva contrasena debe tener entre 8 y 128 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,128}$",
                message = "La nueva contrasena debe incluir al menos una mayuscula, una minuscula, un numero y un caracter especial")
        @Schema(description = "Nueva contrasena (minimo 8 caracteres: mayuscula, minuscula, numero y especial)", example = "ClaveNueva123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String nuevaPassword
) {}