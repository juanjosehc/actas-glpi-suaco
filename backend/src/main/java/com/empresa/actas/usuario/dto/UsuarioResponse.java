package com.empresa.actas.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response con los datos de un usuario")
public class UsuarioResponse {

    @Schema(description = "ID del usuario", example = "1")
    private Long id;

    @Schema(description = "Numero de cedula", example = "1234567890")
    private String cedula;

    @Schema(description = "Nombres", example = "Juan Jose")
    private String nombres;

    @Schema(description = "Apellidos", example = "Hernandez Correa")
    private String apellidos;

    @Schema(description = "Nombre de usuario", example = "jhernandez")
    private String username;

    @Schema(description = "Correo electronico", example = "jhernandez@coltefinanciera.com")
    private String correo;

    @Schema(description = "Cargo", example = "Ingeniero de Soporte")
    private String cargo;

    @Schema(description = "Empresa", example = "Coltefinanciera")
    private String empresa;

    @Schema(description = "Lugar de trabajo", example = "Oficina Principal Bogota")
    private String lugarTrabajo;

    @Schema(description = "Indica si el usuario esta bloqueado", example = "false")
    private Boolean bloqueado;

    @Schema(description = "Rol del usuario", example = "TECNICO")
    private String rol;
}
