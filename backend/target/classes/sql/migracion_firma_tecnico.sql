-- Migración: habilita los eventos de auditoría de la firma permanente del técnico
-- en la restricción CHECK de auditoria_sistema.tipo_evento.
--
-- ddl-auto: update no puede alterar CHECK constraints sobre tabla con datos,
-- se ejecuta a mano (misma mecanica que migracion_auditoria_acta_historial.sql):
--   psql -h localhost -U postgres -d SaucoDB -f backend/src/main/resources/sql/migracion_firma_tecnico.sql
--
-- Eventos nuevos: FIRMA_TECNICO_REGISTRADA, FIRMA_TECNICO_ACTUALIZADA, FIRMA_TECNICO_ELIMINADA

ALTER TABLE auditoria_sistema DROP CONSTRAINT IF EXISTS auditoria_sistema_tipo_evento_check;

ALTER TABLE auditoria_sistema ADD CONSTRAINT auditoria_sistema_tipo_evento_check CHECK (
    tipo_evento IN (
        'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'LOGOUT', 'ACCESO_DENEGADO',
        'DOCUMENTO_VISTO', 'EVIDENCIA_VISTA', 'TOKEN_EXPIRADO', 'TOKEN_INVALIDO',
        'OTP_GENERADO', 'OTP_ENVIADO', 'OTP_ENVIO_FALLIDO', 'OTP_VALIDADO',
        'OTP_INVALIDO', 'OTP_BLOQUEADO', 'OTP_EXPIRADO', 'OTP_REENVIADO',
        'FIRMA_TECNICO_REGISTRADA', 'FIRMA_TECNICO_ACTUALIZADA', 'FIRMA_TECNICO_ELIMINADA'
    )
);