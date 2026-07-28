package com.empresa.actas.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para crear un usuario (solo ADMINISTRADOR)")
public record CrearUsuarioRequest(
        @NotBlank(message = "La cedula es obligatoria")
        @Size(max = 20, message = "La cedula no puede exceder 20 caracteres")
        @Schema(description = "Numero de cedula", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        String cedula,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
        @Schema(description = "Nombres del usuario", example = "Juan Jose", requiredMode = Schema.RequiredMode.REQUIRED)
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
        @Schema(description = "Apellidos del usuario", example = "Hernandez Correa", requiredMode = Schema.RequiredMode.REQUIRED)
        String apellidos,

        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        @Schema(description = "Nombre de usuario para login", example = "jhernandez", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser valido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        @Schema(description = "Correo electronico", example = "jhernandez@coltefinanciera.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String correo,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 6, max = 128, message = "La password debe tener entre 6 y 128 caracteres")
        @Schema(description = "Contrasena (minimo 6 caracteres)", example = "clave123", requiredMode = Schema.RequiredMode.REQUIRED)
        String password,

        @Size(max = 100, message = "El cargo no puede exceder 100 caracteres")
        @Schema(description = "Cargo del usuario", example = "Ingeniero de Soporte")
        String cargo,

        @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
        @Schema(description = "Empresa", example = "Coltefinanciera")
        String empresa,

        @Size(max = 150, message = "El lugar de trabajo no puede exceder 150 caracteres")
        @Schema(description = "Lugar de trabajo", example = "Oficina Principal Bogota")
        String lugarTrabajo,

        @NotBlank(message = "El rol es obligatorio")
        @Schema(description = "Rol del usuario", example = "TECNICO", allowableValues = {"ADMINISTRADOR", "TECNICO", "AUDITOR"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String rol
) {}
