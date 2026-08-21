package com.empresa.actas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para la consulta de usuarios desde GLPI.
 *
 * El frontend lo usa para auto completar el nombre del usuario
 * (receptor en entrega / devolucion) y capturar internamente el
 * correo para la firma electronica, sin campos visibles adicionales.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioGlpiResponse {

    private String login = "";
    private String nombre = "";
    private String correo = "";
}
