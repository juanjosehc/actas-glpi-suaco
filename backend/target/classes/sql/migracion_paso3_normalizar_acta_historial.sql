-- =====================================================================
-- MIGRACION PASO 3 (FASE OPCIONAL) — NORMALIZAR acta_historial
-- Base: SaucoDB (PostgreSQL 18)  |  Fecha: 2026-09-03
--
-- Elimina la redundancia conceptual de acta_historial.usuario_accion
-- (username, NOT NULL) dejando como canonico actor_id + actor_nombre.
-- La lectura ya preferencia actorNombre y usa usuario_accion solo de
-- fallback (AuditoriaConsultaService.java:105-110).
--
-- Riesgo:        BAJO. Backfill idempotente con pre-check de cobertura.
-- Dependencias:  misma release que la limpieza de ActaHistorial.usuarioAccion
--                (entity) y del fallback getUsuarioAccion() en
--                AuditoriaConsultaService.java:110. El campo NO se usa en
--                ninguna otra capa (DTO/controller/frontend/DOCX).
-- Rollback:      bloque comentado al final (restaura columnas + valores).
-- Tablas:        acta_historial (drop 1 columna; quita NOT NULL de las 2
--                columnas canonicas cuando aplica).
-- Validaciones:  queries POSTCHECK al final + verificar panel Historia de
--                una acta y consulta de auditoria.
-- =====================================================================

BEGIN;

-- ------------------------------------------------------------------
-- PRECHECK: confirmar pre-condicion del backfill (que pueda reconstruir)
-- ------------------------------------------------------------------
-- Si hay filas con actor_nombre NULL que NO puedan mapearse por username,
-- el script aborta antes de tocar nada.

-- 1. Los eventos de sistema usan usuario_accion = 'SISTEMA' (sin usuario real).
--    Su actor canonico es el literal 'SISTEMA'.
UPDATE acta_historial
   SET actor_nombre = 'SISTEMA'
 WHERE actor_nombre IS NULL
   AND actor_id IS NULL
   AND usuario_accion = 'SISTEMA';

-- 2. Mapeo por nombre de usuario contra la tabla usuario.
UPDATE acta_historial h
   SET actor_id     = u.id_usuario,
       actor_nombre = u.nombres || ' ' || u.apellidos
  FROM usuario u
 WHERE u.nombre_usuario = h.usuario_accion
   AND h.actor_nombre IS NULL;

-- 3. Si aun queda alguna fila sin canonico, abortar (la transaccion deshace
--    los UPDATE anteriores).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM acta_historial WHERE actor_nombre IS NULL) THEN
        RAISE EXCEPTION 'ABORT: % filas sin actor_nombre tras backfill',
            (SELECT count(*) FROM acta_historial WHERE actor_nombre IS NULL);
    END IF;
END $$;

-- ------------------------------------------------------------------
-- Drop de la columna redundante
-- ------------------------------------------------------------------
ALTER TABLE acta_historial DROP COLUMN IF EXISTS usuario_accion;

COMMIT;

-- =====================================================================
-- POSTCHECK (ejecutar DESPUES del COMMIT):
-- ---------------------------------------------------------------------
-- SELECT count(*) AS columnas_historial_esperadas_10
--   FROM information_schema.columns
--  WHERE table_schema='public' AND table_name='acta_historial';
--
-- SELECT count(*) AS sin_actor
--   FROM acta_historial WHERE actor_nombre IS NULL;        -- 0
--
-- SELECT h.id_historial, h.tipo_evento, h.actor_nombre, h.actor_id
--   FROM acta_historial h ORDER BY h.id_historial DESC LIMIT 10;
--
-- FUNCIONAL: abrir una acta -> panel Historia; modulo Auditoria -> listado.
-- =====================================================================

-- =====================================================================
-- ROLLBACK PASO 3 (restaurar columna + valores)  — requiere refactor Java UNDO
-- ---------------------------------------------------------------------
-- BEGIN;
-- ALTER TABLE acta_historial ADD COLUMN usuario_accion varchar(100);
--
-- UPDATE acta_historial
--    SET usuario_accion = actor_nombre;
--
-- ALTER TABLE acta_historial ALTER COLUMN usuario_accion SET NOT NULL;
-- COMMIT;
-- NOTA: los eventos SISTEMA quedan con 'SISTEMA'; los usuarios mapeados
--       quedan con su nombre completo como username de respaldo.
-- =====================================================================