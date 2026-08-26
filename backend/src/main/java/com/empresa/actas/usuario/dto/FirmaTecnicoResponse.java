package com.empresa.actas.usuario.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Estado de la firma permanente del tecnico autenticado.
 * {@code tiene=false} cuando el usuario aun no registra firma.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FirmaTecnicoResponse(
        boolean tiene,
        String ruta,
        LocalDateTime fechaActualizacion
) {}