package com.actasglpi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "La cedula es obligatoria")
        @Size(max = 20, message = "La cedula no puede exceder 20 caracteres")
        String cedula,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
        String apellidos,

        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser valido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
        String correo,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 6, max = 128, message = "La password debe tener entre 6 y 128 caracteres")
        String password,

        @Size(max = 100, message = "El cargo no puede exceder 100 caracteres")
        String cargo,

        @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
        String empresa,

        @Size(max = 150, message = "El lugar de trabajo no puede exceder 150 caracteres")
        String lugarTrabajo,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {}
