package com.empresa.actas.mail.dto;

import java.time.LocalDateTime;

/**
 * Evidencia objetiva del resultado de un envio SMTP (FORTALECIMIENTO_EVIDENCIA_FIRMA).
 *
 * Reemplaza al {@code boolean} que devolvia {@code enviarCorreoFirma}: ademas de
 * exito/fallo entrega los identificadores que el proveedor SMTP reporta (o que
 * JavaMail genera) y la fecha real de envio, para persistirlos como prueba de que
 * el correo salio hacia el destinatario indicado.
 *
 * @param enviado   {@code true} si el correo fue aceptado por el SMTP para envio
 * @param messageId identificador del mensaje reportado por el transporte (SMTP);
 *                  {@code null} si no se envio o el proveedor no lo expuso
 * @param fechaEnvio fecha real del envio ({@code LocalDateTime.now()} al aceptar
 *                  el mensaje); {@code null} si no se envio
 * @param estado    codigo de estado del envio: ENVIADO | FALLIDO | NO_CONFIGURADO
 *                  | DESTINATARIO_VACIO
 * @param motivo    detalle tecnico del fallo (si {@code enviado=false}); puede
 *                  ser {@code null}
 */
public record ResultadoEnvio(boolean enviado,
                             String messageId,
                             LocalDateTime fechaEnvio,
                             String estado,
                             String motivo) {

    /** Fabrica para envio aceptado por el SMTP. */
    public static ResultadoEnvio exito(String messageId, LocalDateTime fechaEnvio, String estado) {
        return new ResultadoEnvio(true, messageId, fechaEnvio, estado, null);
    }

    /** Fabrica para envio no realizado (fallo, SMTP no configurado, destinatario vacio). */
    public static ResultadoEnvio fallo(String estado, String motivo) {
        return new ResultadoEnvio(false, null, null, estado, motivo);
    }

    public static final String ESTADO_ENVIADO = "ENVIADO";
    public static final String ESTADO_FALLIDO = "FALLIDO";
    public static final String ESTADO_NO_CONFIGURADO = "NO_CONFIGURADO";
    public static final String ESTADO_DESTINATARIO_VACIO = "DESTINATARIO_VACIO";

    public boolean esEnviado() {
        return enviado;
    }
}
