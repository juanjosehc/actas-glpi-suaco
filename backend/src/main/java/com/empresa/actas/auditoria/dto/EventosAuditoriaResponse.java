package com.empresa.actas.auditoria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Paginador de la consulta unificada de auditoria.
 */
@Data
@Builder
@Schema(description = "Resultado paginado de eventos de auditoria")
public class EventosAuditoriaResponse {

    @Schema(description = "Eventos de la pagina actual")
    private List<EventoAuditoriaResponse> eventos;

    @Schema(description = "Total de eventos que cumplen los filtros")
    private long total;

    @Schema(description = "Numero de pagina (base 0)")
    private int pagina;

    @Schema(description = "Tamano de pagina")
    private int tamano;

    @Schema(description = "Total de paginas")
    private int totalPaginas;
}