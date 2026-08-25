package com.empresa.actas.firma.dto;

/**
 * Estado del paso OTP del portal. Jamas incluye el codigo OTP.
 *
 * @param valido            ya existe sesion validada en vigencia; el portal puede cargar el acta
 * @param correoEnmascarado correo del destinatario enmascarado (ca***@dominio)
 * @param enviado           SI el correo con el codigo se envio correctamente (null en legacy sin fila)
 * @param expiraSegundos    segundos restantes de vigencia del codigo actual (null si no hay)
 * @param reenviosRestantes reenvios disponibles antes del limite
 * @param cooldownSegundos  cooldown pendiente antes de poder reenviar/solicitar otro codigo
 * @param codigoVencido     el codigo actual expiro; el usuario debe solicitar uno nuevo
 */
public record FirmaOtpEstadoResponse(
        Boolean valido,
        String correoEnmascarado,
        Boolean enviado,
        Long expiraSegundos,
        Integer reenviosRestantes,
        Long cooldownSegundos,
        Boolean codigoVencido
) {
}