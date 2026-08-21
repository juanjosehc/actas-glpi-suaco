package com.empresa.actas.firma.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request para enviar un acta a firma. El correo es opcional: si se
 * envia, se actualiza el correo del usuario en el acta y se usa como
 * destinatario del correo de firma.
 */
@Schema(description = "Request para enviar un acta a firma")
public record EnviarActaRequest(
        @Schema(description = "Correo del destinatario de la solicitud de firma", example = "usuario@empresa.com")
        String correo
) {}
