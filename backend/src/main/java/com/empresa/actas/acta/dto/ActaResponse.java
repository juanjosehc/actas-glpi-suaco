package com.empresa.actas.acta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response con los datos de un acta")
public class ActaResponse {

    @Schema(description = "ID del acta", example = "1")
    private Long id;

    @Schema(description = "Tipo de acta", example = "ENTREGA")
    private String tipoActa;

    @Schema(description = "Estado actual del acta", example = "GENERADA")
    private String estado;

    @Schema(description = "Cedula del usuario receptor", example = "1234567890")
    private String cedulaUsuario;

    @Schema(description = "Nombre del usuario receptor", example = "Carlos Perez")
    private String nombreUsuario;

    @Schema(description = "Correo del usuario receptor", example = "cperez@empresa.com")
    private String correoUsuario;

    @Schema(description = "Serial del equipo", example = "SN-2024-001")
    private String serialEquipo;

    @Schema(description = "Placa del equipo", example = "PL-001")
    private String placaEquipo;

    @Schema(description = "Descripcion del equipo", example = "Laptop Dell Latitude 5540")
    private String descripcionEquipo;

    @Schema(description = "Contenido HTML del acta")
    private String contenidoHtml;

    @Schema(description = "Fecha de creacion del acta")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha de envio para firma")
    private LocalDateTime fechaEnvio;

    @Schema(description = "Fecha de firma digital")
    private LocalDateTime fechaFirma;

    @Schema(description = "Fecha de aprobacion")
    private LocalDateTime fechaAprobacion;

    @Schema(description = "Numero de ticket GLPI", example = "12345")
    private Long ticketGlpi;

    @Schema(description = "Observacion de rechazo", example = "Firma no valida")
    private String observacionRechazo;

    @Schema(description = "Fecha de rechazo del acta")
    private LocalDateTime fechaRechazo;

    @Schema(description = "Ruta del archivo PDF", example = "uploads/acta_1.pdf")
    private String rutaPdf;

    @Schema(description = "Token activo de firma (solo si estado = ENVIADA)")
    private String tokenFirma;
}
