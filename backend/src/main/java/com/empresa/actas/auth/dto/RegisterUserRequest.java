package com.empresa.actas.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para registrar un nuevo usuario")
public record RegisterUserRequest(
        // SEC-012: cedula numerica (integridad de datos; desincentiva cuentas
        // masivas generadas por scripts con valores basura).
        @NotBlank(message = "La cedula es obligatoria")
        @Size(max = 20, message = "La cedula no puede exceder 20 caracteres")
        @Pattern(regexp = "^\\d{6,12}$", message = "La cedula debe contener solo digitos (entre 6 y 12)")
        @Schema(description = "Numero de cedula", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        String cedula,

        // SEC-012: juegos de caracteres validos para identidades personales.
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
        @Pattern(regexp = "^[A-Za-zÀ-ÿÑñ' .-]+$",
                message = "Los nombres contienen caracteres no validos")
        @Schema(description = "Nombres del usuario", example = "Juan Jose", requiredMode = Schema.RequiredMode.REQUIRED)
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
        @Pattern(regexp = "^[A-Za-zÀ-ÿÑñ' .-]+$",
                message = "Los apellidos contienen caracteres no validos")
        @Schema(description = "Apellidos del usuario", example = "Hernandez Correa", requiredMode = Schema.RequiredMode.REQUIRED)
        String apellidos,

        // SEC-012: username sobre juego de caracteres seguro (sin espacios en
        // blanco ni caracteres de control, dificulta nombres colisionantes o
        // de relleno generados por scripts).
        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "El username solo puede contener letras, numeros, punto, guion o guion bajo")
        @Schema(description = "Nombre de usuario para login", example = "jhernandez", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser valido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        @Schema(description = "Correo electronico", example = "jhernandez@coltefinanciera.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String correo,

        // SEC-016: politica de contrasena. Minimo 8 caracteres y una clase de cada
        // grupo (mayuscula, minuscula, digito, especial). Regla moderada: no
        // exige longitud extrema ni rotacion para no romper la experiencia.
        @NotBlank(message = "La password es obligatoria")
        @Size(min = 8, max = 128, message = "La password debe tener entre 8 y 128 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,128}$",
                message = "La password debe incluir al menos una mayuscula, una minuscula, un numero y un caracter especial")
        @Schema(description = "Contrasena (minimo 8 caracteres: mayuscula, minuscula, numero y especial)", example = "Clave123!", requiredMode = Schema.RequiredMode.REQUIRED)
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
