package com.empresa.actas.firma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Schema(description = "Nombre del usuario que debe firmar", example = "Carlos Perez")
    private String nombreUsuario;

    @Schema(description = "Cedula del usuario", example = "1234567890")
    private String cedulaUsuario;

    @Schema(description = "Descripcion del equipo", example = "Laptop Dell Latitude 5540")
    private String descripcionEquipo;

    @Schema(description = "Contenido HTML del acta")
    private String contenidoHtml;
}
