package com.empresa.actas.firma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request para enviar un acta a firma. El correo es opcional: si se
 * envia, se actualiza el correo del usuario en el acta y se usa como
 * destinatario del correo de firma. Si viene con valor, se valida formato.
 */
@Schema(description = "Request para enviar un acta a firma")
public record EnviarActaRequest(
        @Email(message = "El correo de envio no es valido")
        @Size(max = 254, message = "El correo excede 254 caracteres")
        @Schema(description = "Correo del destinatario de la solicitud de firma", example = "usuario@empresa.com")
        String correo
) {}
