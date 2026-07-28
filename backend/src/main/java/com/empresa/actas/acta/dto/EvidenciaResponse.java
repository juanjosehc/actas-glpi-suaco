package com.empresa.actas.acta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response con la informacion de una evidencia")
public class EvidenciaResponse {

    @Schema(description = "ID de la evidencia", example = "1")
    private Long id;

    @Schema(description = "Tipo de evidencia", example = "FIRMA")
    private String tipo;

    @Schema(description = "Ruta del archivo de evidencia", example = "uploads/firmas/abc123.png")
    private String rutaArchivo;
}
