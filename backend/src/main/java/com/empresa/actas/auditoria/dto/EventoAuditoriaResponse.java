package com.empresa.actas.auditoria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Vista unificada de un evento de auditoria, sin importar su origen:
 * acta_historial (CAPA 1, documental) o auditoria_sistema (CAPA 2, seguridad/accesos).
 */
@Data
@Builder
@Schema(description = "Evento de auditoria unificado (acta_historial o auditoria_sistema)")
public class EventoAuditoriaResponse {

    @Schema(description = "ID del registro de auditoria")
    private Long id;

    @Schema(description = "Fecha y hora del evento")
    private LocalDateTime fecha;

    @Schema(description = "Tipo de evento, ej. ACTA_APROBADA, LOGIN_FALLIDO")
    private String tipoEvento;

    @Schema(description = "Usuario responsable (o SISTEMA / firmante)")
    private String usuario;

    @Schema(description = "Rol del responsable: ADMINISTRADOR, AUDITOR, TECNICO o SISTEMA")
    private String rol;

    @Schema(description = "Entidad afectada, ej. Acta, Firma, OTP, Usuario")
    private String entidad;

    @Schema(description = "ID de la entidad afectada")
    private Long entidadId;

    @Schema(description = "Accion realizada (descripcion legible del tipo de evento)")
    private String accion;

    @Schema(description = "Detalle / observacion del evento")
    private String detalle;

    @Schema(description = "Informacion adicional: estado anterior->nuevo, recurso, IP, token")
    private String informacionAdicional;

    @Schema(description = "Estado del acta al momento del evento (solo eventos de acta)")
    private String estadoActa;

    @Schema(description = "Correo del usuario asociado a la acta (solo eventos de acta)")
    private String correo;

    @Schema(description = "Categoria visual: DOCUMENTOS, SEGURIDAD o SISTEMA")
    private String categoria;

    @Schema(description = "Origen del evento: ACTAS (acta_historial) o SISTEMA (auditoria_sistema)")
    private String origen;
}