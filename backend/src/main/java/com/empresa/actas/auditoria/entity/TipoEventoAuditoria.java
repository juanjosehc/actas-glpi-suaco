package com.empresa.actas.auditoria.entity;

/**
 * Eventos de auditoria de sistema (CAPA 2): autenticacion, accesos,
 * visualizaciones, tokens y seguridad. Separados de los eventos
 * documentales de {@code acta_historial} (CAPA 1).
 */
public enum TipoEventoAuditoria {
    LOGIN_EXITOSO,
    LOGIN_FALLIDO,
    LOGOUT,
    ACCESO_DENEGADO,
    DOCUMENTO_VISTO,
    EVIDENCIA_VISTA,
    TOKEN_EXPIRADO,
    TOKEN_INVALIDO,
    OTP_GENERADO,
    OTP_ENVIADO,
    OTP_ENVIO_FALLIDO,
    OTP_VALIDADO,
    OTP_INVALIDO,
    OTP_BLOQUEADO,
    OTP_EXPIRADO,
    OTP_REENVIADO,
    FIRMA_TECNICO_REGISTRADA,
    FIRMA_TECNICO_ACTUALIZADA,
    FIRMA_TECNICO_ELIMINADA
}