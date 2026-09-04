package com.empresa.actas.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Restablecimiento de contrasena por el administrador (POST /usuarios/{id}/restablecer-password).
 * El usuario queda marcado para cambiar la contrasena en el proximo login
 * ({@code cambiarPasswordObligatorio}).
 */
@Schema(description = "Request para que el administrador restablezca la contrasena de un usuario")
public record RestablecerPasswordRequest(

        // SEC-016: misma politica de contrasena que registro y creacion (el admin
        // entrega una temporal fuerte; al primer login se fuerza el cambio).
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 128, message = "La nueva contrasena debe tener entre 8 y 128 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,128}$",
                message = "La nueva contrasena debe incluir al menos una mayuscula, una minuscula, un numero y un caracter especial")
        @Schema(description = "Contrasena temporal (minimo 8 caracteres: mayuscula, minuscula, numero y especial)", example = "Temporal123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String nuevaPassword
) {}