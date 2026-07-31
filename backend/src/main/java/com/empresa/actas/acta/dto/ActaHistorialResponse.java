package com.empresa.actas.acta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Evento de auditoria de una acta")
public class ActaHistorialResponse {

    @Schema(description = "Identificador del registro de historial")
    private Long idHistorial;

    @Schema(description = "Identificador del acta")
    private Long idActa;

    @Schema(description = "Tipo de evento de auditoria", example = "ACTA_RECHAZADA_USUARIO")
    private String tipoEvento;

    @Schema(description = "Estado anterior del acta", example = "ENVIADA")
    private String estadoAnterior;

    @Schema(description = "Estado nuevo del acta", example = "RECHAZADA")
    private String estadoNuevo;

    @Schema(description = "Identificador del usuario del sistema que ejecuto la accion")
    private Long actorId;

    @Schema(description = "Nombre del actor (usuario del sistema o firmante)")
    private String actorNombre;

    @Schema(description = "Token de firma utilizado, cuando aplica")
    private Long idTokenFirma;

    @Schema(description = "Fecha exacta del evento")
    private LocalDateTime fechaCambio;

    @Schema(description = "Observacion asociada al evento")
    private String observacion;
}
