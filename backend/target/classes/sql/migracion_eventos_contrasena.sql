-- Migración: habilita los eventos de auditoría de contraseñas en la
-- restricción CHECK de auditoria_sistema.tipo_evento.
--
-- ddl-auto: update no puede alterar CHECK constraints sobre tabla con datos,
-- se ejecuta a mano (misma mecanica que migracion_firma_tecnico.sql):
--   psql -h localhost -U postgres -d SaucoDB -f backend/src/main/resources/sql/migracion_eventos_contrasena.sql
--
-- Eventos nuevos: CAMBIO_CONTRASENA, RESET_CONTRASENA_ADMIN, RECUPERACION_SOLICITADA,
--                 RECUPERACION_COMPLETADA, RECUPERACION_TOKEN_INVALIDO

ALTER TABLE auditoria_sistema DROP CONSTRAINT IF EXISTS auditoria_sistema_tipo_evento_check;

ALTER TABLE auditoria_sistema ADD CONSTRAINT auditoria_sistema_tipo_evento_check CHECK (
    tipo_evento IN (
        'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'LOGOUT', 'ACCESO_DENEGADO',
        'DOCUMENTO_VISTO', 'EVIDENCIA_VISTA', 'TOKEN_EXPIRADO', 'TOKEN_INVALIDO',
        'OTP_GENERADO', 'OTP_ENVIADO', 'OTP_ENVIO_FALLIDO', 'OTP_VALIDADO',
        'OTP_INVALIDO', 'OTP_BLOQUEADO', 'OTP_EXPIRADO', 'OTP_REENVIADO',
        'FIRMA_TECNICO_REGISTRADA', 'FIRMA_TECNICO_ACTUALIZADA', 'FIRMA_TECNICO_ELIMINADA',
        'CAMBIO_CONTRASENA', 'RESET_CONTRASENA_ADMIN', 'RECUPERACION_SOLICITADA',
        'RECUPERACION_COMPLETADA', 'RECUPERACION_TOKEN_INVALIDO'
    )
);