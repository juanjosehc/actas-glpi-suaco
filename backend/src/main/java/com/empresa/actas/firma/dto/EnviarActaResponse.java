package com.empresa.actas.firma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response despues de enviar un acta para firma")
public class EnviarActaResponse {

    @Schema(description = "Token unico para firmar el acta", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String token;

    @Schema(description = "URL publica para que el usuario firme el acta", example = "http://localhost:5500/firma.html?token=a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String urlFirma;
}
