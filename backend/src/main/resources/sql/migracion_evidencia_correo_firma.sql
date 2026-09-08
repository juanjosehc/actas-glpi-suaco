-- =====================================================
-- FORTALECIMIENTO_EVIDENCIA_FIRMA: evidencia consolidada en auditoria_sistema
-- =====================================================
-- Fortalece el valor probatorio del proceso de firma electronica sin cambiar
-- el flujo funcional (ver docs/FORTALECIMIENTO_EVIDENCIA_FIRMA.md):
--
--   A. Evidencia SMTP del envio (message_id, estado_envio) consolidada como
--      columnas tipadas de auditoria_sistema -- UNICA fuente de verdad, sin la
--      tabla redundante correo_envio que se elimina al final.
--   C. Columna firma_otp.hash_correo: huella criptografica inmutable (SHA-256)
--      del correo objetivo del OTP.
--
-- En desarrollo JPA ddl-auto:update crea las columnas automaticamente. Esta
-- migracion es para entornos con JPA_DDL_AUTO=validate (produccion por SEC-109),
-- donde Hibernate no altera la BD: se ejecuta a mano.
--
-- IDEMPOTENTE: cada paso se ejecuta solo si falta.
--   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d SaucoDB -f backend/src/main/resources/sql/migracion_evidencia_correo_firma.sql
-- PostgreSQL pide el password de postgres por separado.

-- -----------------------------------------------------
-- C. firma_otp.hash_correo (SHA-256 hex, 64 chars, NOT NULL)
-- -----------------------------------------------------
ALTER TABLE firma_otp ADD COLUMN IF NOT EXISTS hash_correo varchar(64);

-- Backfill: los OTP anteriores no tienen hash. Se computa sobre el correo ya
-- persistido (correo_destino), identico al valor usado para emitirlos, para
-- que la serie historica quede cubierta por la misma huella.
UPDATE firma_otp
SET hash_correo = encode(digest(correo_destino::bytea, 'sha256'), 'hex')
WHERE hash_correo IS NULL
  AND correo_destino IS NOT NULL
  AND correo_destino <> '';

-- Consentimiento NOT NULL una vez cubierto el backfill (ddl-auto no lo hace).
ALTER TABLE firma_otp ALTER COLUMN hash_correo SET NOT NULL;

-- -----------------------------------------------------
-- A. Columnas tipadas de evidencia de envio SMTP en auditoria_sistema
-- -----------------------------------------------------
ALTER TABLE auditoria_sistema ADD COLUMN IF NOT EXISTS message_id   varchar(255);
ALTER TABLE auditoria_sistema ADD COLUMN IF NOT EXISTS hash_correo  varchar(64);
ALTER TABLE auditoria_sistema ADD COLUMN IF NOT EXISTS estado_envio varchar(30);

COMMENT ON COLUMN auditoria_sistema.message_id IS
    'FORTALECIMIENTO_EVIDENCIA_FIRMA: message-id que entrego el SMTP (integro).';
COMMENT ON COLUMN auditoria_sistema.hash_correo IS
    'FORTALECIMIENTO_EVIDENCIA_FIRMA: SHA-256 hex del correo destino del OTP.';
COMMENT ON COLUMN auditoria_sistema.estado_envio IS
    'FORTALECIMIENTO_EVIDENCIA_FIRMA: ENVIADO|FALLIDO|NO_CONFIGURADO|DESTINATARIO_VACIO.';

-- -----------------------------------------------------
-- Migracion de la tabla historica correo_envio -> auditoria_sistema
-- (solo si la tabla redundante aun existe y tiene filas).
-- -----------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'correo_envio') THEN
        -- Cada envio registrado pasa a auditoria_sistema como OTP_ENVIADO/OTP_ENVIO_FALLIDO,
        -- con entidad_id enmascarado (SEC-105: un FirmaToken no se guarda en claro)
        -- y message_id/hash_correo/estado_envio completos en las columnas tipadas.
        INSERT INTO auditoria_sistema
            (fecha_evento, tipo_evento, entidad, entidad_id, recurso, detalle,
             message_id, hash_correo, estado_envio)
        SELECT
            ce.fecha_envio,
            CASE WHEN ce.estado_envio = 'ENVIADO'
                 THEN 'OTP_ENVIADO' ELSE 'OTP_ENVIO_FALLIDO' END::varchar,
            'FIRMA_TOKEN',
            substr(ft.token, 1, 8) || '...',
            '/firma/otp',
            'Migracion correo_envio->auditoria_sistema: envio a '
                || ce.correo_destino
                || ' | hash_correo=' || ce.hash_correo
                || ' | message_id=' || coalesce(ce.message_id, ''),
            ce.message_id,
            ce.hash_correo,
            ce.estado_envio
        FROM correo_envio ce
        LEFT JOIN firma_token ft ON ft.id_token = ce.id_token_firma
        WHERE NOT EXISTS (
            SELECT 1 FROM auditoria_sistema a
            WHERE a.message_id = ce.message_id
              AND a.estado_envio = ce.estado_envio
        );

        DROP TABLE correo_envio;
    END IF;
END $$;
