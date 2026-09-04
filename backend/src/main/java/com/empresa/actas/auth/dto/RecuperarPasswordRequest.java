package com.empresa.actas.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de recuperacion de contrasena por correo.
 * Respuesta GENERICA siempre (anti-enumeracion: no revela si el correo existe).
 */
@Schema(description = "Request para solicitar la recuperacion de contrasena por correo")
public record RecuperarPasswordRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser valido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        @Schema(description = "Correo registrado del usuario", example = "jhernandez@coltefinanciera.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String correo
) {}