-- =====================================================================
-- MIGRACION PASO 5 — ELIMINAR contenido_html DE acta
-- Base: SaucoDB (PostgreSQL 18)  |  Fecha: 2026-09-03
--
-- Decision del usuario (2026-09-03): el modelo documental oficial de SAUCO
-- es datos_originales -> DOCX -> PDF -> Firma -> Evidencias. La arquitectura
-- V2 basada en contenido_html -> Vista HTML -> PDF nunca termino de adoptarse:
--   - Las actas reales se crean con formularios V1 (contenidoHtml siempre null).
--   - El flujo V2 quedo abandonado; generar-acta.html y frontend/templates/ no existen.
--   - El portal de firma, la regeneracion DOCX/PDF y las evidencias NO dependen de el.
-- Con esta migracion, datos_originales queda como unica fuente documental persistente.
--
-- Riesgo:        BAJO (con respaldo previo obligatorio de los 7 registros que
--                tienen contenido_html, en db-backups/respaldo_contenido_html_2026-09-03.sql
--                y en la tabla public.respaldo_contenido_html de la BD).
-- Dependencias:  misma release que la limpieza Java de contenidoHtml (entity,
--                DTOs, mappers, services, whitelist sort y HtmlSanitizadorService),
--                que ya NO mapea la columna.
-- Rollback:      bloque comentado al final del archivo.
-- Tablas:        acta (1 columna).
-- Validaciones:  PRECHECK (7 registros respaldados) + POSTCHECK (columna ausente).
-- =====================================================================

BEGIN;

-- ------------------------------------------------------------------
-- PRECHECK: abortar si el respaldo anterior ya fue aplicado (idempotencia)
-- o si hay registros con contenido_html no respaldados. El respaldo en BD
-- se carga ANTES de ejecutar esta migracion desde db-backups/respaldo_...sql.
-- ------------------------------------------------------------------
DO $$
DECLARE
    n_con_contenido integer;
    n_respaldo      integer;
BEGIN
    SELECT count(*) INTO n_con_contenido FROM acta
     WHERE contenido_html IS NOT NULL AND contenido_html <> '';

    SELECT count(*) INTO n_respaldo FROM public.respaldo_contenido_html;

    IF n_con_contenido = 0 THEN
        -- Estado ya limpio (idempotente): DROP IF EXISTS no falla
        RETURN;
    END IF;

    IF n_respaldo <> n_con_contenido THEN
        RAISE EXCEPTION 'ABORT: hay % actas con contenido_html pero solo % respaldadas. Ejecutar primero db-backups/respaldo_contenido_html_2026-09-03.sql',
            n_con_contenido, n_respaldo;
    END IF;
END $$;

-- ------------------------------------------------------------------
-- 1. Columna obsoleta
-- ------------------------------------------------------------------
ALTER TABLE acta DROP COLUMN IF EXISTS contenido_html;

COMMIT;

-- =====================================================================
-- POSTCHECK (ejecutar DESPUES del COMMIT):
-- ---------------------------------------------------------------------
-- SELECT count(*) FROM information_schema.columns
--  WHERE table_schema='public' AND table_name='acta'
--    AND column_name='contenido_html';     -- 0 filas
--
-- SELECT count(*) AS columnas_acta_ahora
--  FROM information_schema.columns
--  WHERE table_schema='public' AND table_name='acta';   -- 21
--
-- SELECT count(*) AS respaldo_conservado FROM public.respaldo_contenido_html;  -- 7
-- =====================================================================

-- =====================================================================
-- ROLLBACK PASO 5 (restaurar estructura; los datos se restauran desde
-- public.respaldo_contenido_html)
-- ---------------------------------------------------------------------
-- BEGIN;
-- ALTER TABLE acta ADD COLUMN contenido_html text;
-- UPDATE acta a SET contenido_html = r.contenido_html
--   FROM public.respaldo_contenido_html r WHERE r.id_acta = a.id_acta;
-- COMMIT;
-- =====================================================================