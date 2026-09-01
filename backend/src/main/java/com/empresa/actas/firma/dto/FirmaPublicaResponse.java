package com.empresa.actas.firma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response con los datos del acta para la pagina de firma publica")
public class FirmaPublicaResponse {

    @Schema(description = "ID del acta", example = "1")
    private Long idActa;

    @Schema(description = "Tipo de acta", example = "ENTREGA")
    private String tipoActa;

    @Schema(description = "Estado actual del acta", example = "ENVIADA")
    private String estado;

    @Schema(description = "Ruta del archivo PDF oficial (misma fuente que el modulo de Actas)", example = "uploads/pdf/acta_1.pdf")
    private String rutaPdf;

    @Schema(description = "Ruta del PDF del checklist de entrega (solo ENTREGA)", example = "uploads/pdf/checklist_1.pdf")
    private String rutaPdfChecklist;

    @Schema(description = "Nombre del usuario que debe firmar", example = "Carlos Perez")
    private String nombreUsuario;

    @Schema(description = "Cedula del usuario", example = "1234567890")
    private String cedulaUsuario;

    @Schema(description = "Correo del usuario", example = "carlos@example.com")
    private String correoUsuario;

    @Schema(description = "Descripcion del equipo", example = "Laptop Dell Latitude 5540")
    private String descripcionEquipo;

    @Schema(description = "Serial del equipo", example = "SN-12345")
    private String serialEquipo;

    @Schema(description = "Placa del equipo", example = "PL-001")
    private String placaEquipo;

    @Schema(description = "Ticket GLPI", example = "12345")
    private Long ticketGlpi;

    @Schema(description = "Contenido HTML del acta")
    private String contenidoHtml;

    @Schema(description = "Fecha de rechazo del acta")
    private LocalDateTime fechaRechazo;

    @Schema(description = "Observacion de rechazo")
    private String observacionRechazo;
}
