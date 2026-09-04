-- =====================================================================
-- MIGRACION PASO 2 — DROP COLUMNAS OBSOLETAS DE acta
-- Base: SaucoDB (PostgreSQL 18)  |  Fecha: 2026-09-03
--
-- Elimina 12 columnas heredadas del esquema V1 (una columna por atributo
-- de equipo). Hoy NINGUNA tiene datos (0/90) y el flujo documental se
-- alimenta exclusivamente de acta.datos_originales.
--
-- Riesgo:        BAJO si se aplica el requisito previo de codigo.
-- Dependencias:  REQUIERE ANTES el refactor de PdfService.generarPdfFinal
--                para leer desde acta.datos_originales en lugar de los
--                getters de la entidad, rebuild `mvn clean` y smoke test.
--                Sin ese refactor, la aprobacion de actas legacy (13 sin
--                datos_originales) dejaria de emitir campos en el PDF.
-- Rollback:      ADD COLUMN con TYPES/LENGTHS originales (bloque comentado
--                al final). No hay datos que restaurar.
-- Tablas:        acta (12 columnas).
-- Validaciones:  queries POSTCHECK al final; requieren smoke test funcional.
-- =====================================================================

BEGIN;

-- ------------------------------------------------------------------
-- PRECHECKS: abortar si CUALQUIER columna tiene datos
-- ------------------------------------------------------------------
DO $$
DECLARE
    v_total bigint;
BEGIN
    SELECT count(*) FROM acta WHERE
           marca_modelo      IS NOT NULL OR procesador       IS NOT NULL OR
           memoria_ram       IS NOT NULL OR disco_duro       IS NOT NULL OR
           sistema_operativo IS NOT NULL OR monitor          IS NOT NULL OR
           accesorios        IS NOT NULL OR estado_equipo    IS NOT NULL OR
           cargo             IS NOT NULL OR lugar_trabajo    IS NOT NULL OR
           empresa           IS NOT NULL OR observaciones    IS NOT NULL
    INTO v_total;

    IF v_total > 0 THEN
        RAISE EXCEPTION 'ABORT: % filas de acta tienen datos en columnas obsoletas', v_total;
    END IF;
END $$;

-- ------------------------------------------------------------------
-- Drop de columnas obsoletas
-- ------------------------------------------------------------------
ALTER TABLE acta DROP COLUMN IF EXISTS marca_modelo;
ALTER TABLE acta DROP COLUMN IF EXISTS procesador;
ALTER TABLE acta DROP COLUMN IF EXISTS memoria_ram;
ALTER TABLE acta DROP COLUMN IF EXISTS disco_duro;
ALTER TABLE acta DROP COLUMN IF EXISTS sistema_operativo;
ALTER TABLE acta DROP COLUMN IF EXISTS monitor;
ALTER TABLE acta DROP COLUMN IF EXISTS accesorios;
ALTER TABLE acta DROP COLUMN IF EXISTS estado_equipo;
ALTER TABLE acta DROP COLUMN IF EXISTS cargo;
ALTER TABLE acta DROP COLUMN IF EXISTS lugar_trabajo;
ALTER TABLE acta DROP COLUMN IF EXISTS empresa;
ALTER TABLE acta DROP COLUMN IF EXISTS observaciones;

COMMIT;

-- =====================================================================
-- POSTCHECK (ejecutar DESPUES del COMMIT):
-- ---------------------------------------------------------------------
-- SELECT count(*) AS columnas_acta_esperadas_22
--   FROM information_schema.columns
--  WHERE table_schema='public' AND table_name='acta';
--
-- SELECT column_name FROM information_schema.columns
--  WHERE table_schema='public' AND table_name='acta'
--    AND column_name IN ('marca_modelo','procesador','memoria_ram',
--                        'disco_duro','sistema_operativo','monitor',
--                        'accesorios','estado_equipo','cargo',
--                        'lugar_trabajo','empresa','observaciones'); -- 0 filas
--
-- SMOKE TEST (imperativo):
--   1. POST /generar-acta (ENTREGA)          -> ZIP + PDF OK.
--   2. POST /generar-devolucion              -> documento OK.
--   3. POST /generar-formateo-seguro         -> documento OK.
--   4. Aprobar un acta FIRMADA               -> regenera PDF firmado sin errores
--      (ejercita PdfService solo si el acta no tiene datos_originales).
--   5. GET /actas?q=... y portal /firma/{token}  -> campos intactos.
-- =====================================================================

-- =====================================================================
-- ROLLBACK PASO 2 (restaurar columnas, vacias)  — requiere refactor Java UNDO
-- ---------------------------------------------------------------------
-- BEGIN;
-- ALTER TABLE acta ADD COLUMN marca_modelo      varchar(100);
-- ALTER TABLE acta ADD COLUMN procesador        varchar(100);
-- ALTER TABLE acta ADD COLUMN memoria_ram       varchar(50);
-- ALTER TABLE acta ADD COLUMN disco_duro        varchar(100);
-- ALTER TABLE acta ADD COLUMN sistema_operativo varchar(100);
-- ALTER TABLE acta ADD COLUMN monitor           varchar(100);
-- ALTER TABLE acta ADD COLUMN accesorios        varchar(255);
-- ALTER TABLE acta ADD COLUMN estado_equipo     varchar(50);
-- ALTER TABLE acta ADD COLUMN cargo             varchar(100);
-- ALTER TABLE acta ADD COLUMN lugar_trabajo     varchar(100);
-- ALTER TABLE acta ADD COLUMN empresa           varchar(100);
-- ALTER TABLE acta ADD COLUMN observaciones     varchar(500);
-- COMMIT;
-- NOTA: restaura la estructura, NO los getters de Acta.java/PdfService.
-- =====================================================================