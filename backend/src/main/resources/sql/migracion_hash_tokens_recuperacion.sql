-- =====================================================
-- SEC-120: persistir el token de recuperacion como digest SHA-256 hex
-- =====================================================
-- El UUID en claro jamas debe vivir en la BD (un dump = la capability).
-- Esta migracion:
--   1. Ancla el soporte de hash (pgcrypto).
--   2. Ensancha la columna token a varchar(64) (el digest SHA-256 hex tiene 64 chars).
--   3. Backfill: convierte los tokens existentes (UUID de 36 chars) a su digest,
--      para que la nueva entidad (que guarda el hash) siga encontrandolos.
--
-- IDEMPOTENTE: cada paso se ejecuta solo si falta. Ejecutar a mano:
--   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d SaucoDB -f backend/src/main/resources/sql/migracion_hash_tokens_recuperacion.sql
-- PostgreSQL pide el password de postgres por separado.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. Ensanchar columna antes de guardar hashes de 64 chars.
ALTER TABLE password_reset_token
    ALTER COLUMN token TYPE varchar(64);

-- 3. Backfill: UUID guardado en claro (36 chars) -> digest SHA-256 hex (64 chars).
--    Solo filas que sigan siendo un UUID en claro, para no re-hashear las que ya
--    se migraron (si se reejecuta) ni tocar datos que no correspondan.
UPDATE password_reset_token
SET token = encode(digest(token::bytea, 'sha256'), 'hex')
WHERE utilizado = false
  AND length(token) = 36
  AND token ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

-- =====================================================
-- SEC-125: ampliar el CHECK de auditoria_sistema.tipo_evento
-- =====================================================
-- Se agrego el evento CONSULTA_GLPI_USUARIOS al enum TipoEventoAuditoria.
-- ddl-auto:update NO altera check constraints con datos, asi que se recrea
-- (mismo nombre, valor agregado) a mano.
ALTER TABLE auditoria_sistema DROP CONSTRAINT IF EXISTS auditoria_sistema_tipo_evento_check;
ALTER TABLE auditoria_sistema ADD CONSTRAINT auditoria_sistema_tipo_evento_check
    CHECK (tipo_evento IN (
        'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'LOGOUT', 'ACCESO_DENEGADO',
        'DOCUMENTO_VISTO', 'EVIDENCIA_VISTA', 'TOKEN_EXPIRADO', 'TOKEN_INVALIDO',
        'OTP_GENERADO', 'OTP_ENVIADO', 'OTP_ENVIO_FALLIDO', 'OTP_VALIDADO',
        'OTP_INVALIDO', 'OTP_BLOQUEADO', 'OTP_EXPIRADO', 'OTP_REENVIADO',
        'FIRMA_TECNICO_REGISTRADA', 'FIRMA_TECNICO_ACTUALIZADA', 'FIRMA_TECNICO_ELIMINADA',
        'CAMBIO_CONTRASENA', 'RESET_CONTRASENA_ADMIN', 'RECUPERACION_SOLICITADA',
        'RECUPERACION_COMPLETADA', 'RECUPERACION_TOKEN_INVALIDO',
        'CONSULTA_GLPI_USUARIOS'
    ));
