-- =====================================================================
-- MIGRACION PASO 4 — INTEGRIDAD REFERENCIAL (FKs reales)
-- Base: SaucoDB (PostgreSQL 18)  |  Fecha: 2026-09-03
--
-- Crea las FK que la normalizacion exige para el nuevo modelo:
--   1. acta.id_tecnico           -> usuario.id_usuario
--   2. evidencia.id_acta         -> acta.id_acta
--   3. firma_token.id_acta       -> acta.id_acta   (ya tiene UK sobre id_acta)
--   4. firma_otp.id_token_firma  -> firma_token.id_token
--   5. acta_historial.id_acta    -> acta.id_acta
--
-- Riesgo:        BAJO. Prechecks validan ausencia de huerfanos esperados.
--                La unica depuracion es un OTP muerto (registro de OTP de un
--                token de firma que ya no existe: firma_token id=86 se borro
--                tras generar el OTP; OTP no usado y expirado).
-- Dependencias:  misma release que entities JPA (no se mapean las FKs en
--                JPA; la BD garantiza la integridad por constraint).
-- Rollback:      bloque comentado al final (DROP CONSTRAINT).
-- Tablas:        acta, evidencia, firma_token, firma_otp, acta_historial.
-- Validaciones:  queries POSTCHECK al final (joins con 0 huerfanos).
-- =====================================================================

BEGIN;

-- ------------------------------------------------------------------
-- PRECHECK 1: los UNICOS huerfanos permitidos son los ya conocidos.
-- El OTP 49 (firma_token=86 inexistente, no usado, expirado) se
-- depura de forma controlada y documentada mas abajo.
-- ------------------------------------------------------------------
DO $$
BEGIN
    IF (SELECT count(*) FROM acta a LEFT JOIN usuario u ON u.id_usuario=a.id_tecnico WHERE u.id_usuario IS NULL) <> 0 THEN
        RAISE EXCEPTION 'ABORT: existen acta.id_tecnico huerfanos sin depurar';
    END IF;
    IF (SELECT count(*) FROM evidencia e LEFT JOIN acta a ON a.id_acta=e.id_acta WHERE a.id_acta IS NULL) <> 0 THEN
        RAISE EXCEPTION 'ABORT: existen evidencia.id_acta huerfanos';
    END IF;
    IF (SELECT count(*) FROM firma_token ft LEFT JOIN acta a ON a.id_acta=ft.id_acta WHERE a.id_acta IS NULL) <> 0 THEN
        RAISE EXCEPTION 'ABORT: existen firma_token.id_acta huerfanos';
    END IF;
    IF (SELECT count(*) FROM firma_otp fo LEFT JOIN firma_token ft ON ft.id_token=fo.id_token_firma WHERE ft.id_token IS NULL) <> 1 THEN
        RAISE EXCEPTION 'ABORT: numero de firma_otp huerfanos distinto del esperado (1)';
    END IF;
    IF (SELECT count(*) FROM acta_historial h LEFT JOIN acta a ON a.id_acta=h.id_acta WHERE a.id_acta IS NULL) <> 0 THEN
        RAISE EXCEPTION 'ABORT: existen acta_historial.id_acta huerfanos';
    END IF;
END $$;

-- ------------------------------------------------------------------
-- Depuracion controlada: el unico OTP huerfano.
-- Evidencia: id_otp=49, correo maria.lopez@empresa.com, fecha_creacion
-- 2026-08-31 12:29, expiracion 12:39, usado=f, id_token_firma=86.
-- El firma_token 86 no existe: el token se emitio (auditoria 595
-- OTP_GENERADO) y su fila se elimino despues. OTP sin uso ni cesion
-- posible: depurado. Backup completo previo: db-backups/.
-- ------------------------------------------------------------------
DELETE FROM firma_otp WHERE id_otp = 49 AND id_token_firma = 86;

-- ------------------------------------------------------------------
-- Creacion de FKs
-- ------------------------------------------------------------------
ALTER TABLE acta          ADD CONSTRAINT fk_acta_id_tecnico  FOREIGN KEY (id_tecnico)     REFERENCES usuario (id_usuario);
ALTER TABLE evidencia     ADD CONSTRAINT fk_evidencia_id_acta FOREIGN KEY (id_acta)        REFERENCES acta (id_acta);
ALTER TABLE firma_token   ADD CONSTRAINT fk_token_id_acta     FOREIGN KEY (id_acta)        REFERENCES acta (id_acta);
ALTER TABLE firma_otp     ADD CONSTRAINT fk_otp_id_token      FOREIGN KEY (id_token_firma) REFERENCES firma_token (id_token);
ALTER TABLE acta_historial ADD CONSTRAINT fk_historial_id_acta FOREIGN KEY (id_acta)       REFERENCES acta (id_acta);

COMMIT;

-- =====================================================================
-- POSTCHECK (ejecutar DESPUES del COMMIT):
-- ---------------------------------------------------------------------
-- SELECT conname, conrelid::regclass, pg_get_constraintdef(oid)
--   FROM pg_constraint WHERE contype='f'
--    AND conrelid IN (SELECT c.oid FROM pg_class c
--                     JOIN pg_namespace n ON n.oid=c.relnamespace
--                    WHERE n.nspname='public')
--   ORDER BY conname;   -- debe listar las 5 nuevas + fk_usuario_rol
--
-- SELECT count(*) FROM firma_otp WHERE id_token_firma=86;  -- 0
-- SELECT count(*) FROM firma_otp fo LEFT JOIN firma_token ft
--    ON ft.id_token=fo.id_token_firma WHERE ft.id_token IS NULL;  -- 0
-- =====================================================================

-- =====================================================================
-- ROLLBACK PASO 4 (quitar FKs; la depuracion del OTP 49 NO se revierte
-- porque el token 86 ya no existia — no hay integridad que restaurar)
-- ---------------------------------------------------------------------
-- BEGIN;
-- ALTER TABLE acta_historial DROP CONSTRAINT IF EXISTS fk_historial_id_acta;
-- ALTER TABLE firma_otp     DROP CONSTRAINT IF EXISTS fk_otp_id_token;
-- ALTER TABLE firma_token   DROP CONSTRAINT IF EXISTS fk_token_id_acta;
-- ALTER TABLE evidencia     DROP CONSTRAINT IF EXISTS fk_evidencia_id_acta;
-- ALTER TABLE acta          DROP CONSTRAINT IF EXISTS fk_acta_id_tecnico;
-- COMMIT;
-- =====================================================================