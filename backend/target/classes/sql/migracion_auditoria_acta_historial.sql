-- =============================================================
-- MIGRACION: AUDITORIA COMPLETA DE ACTAS
-- Amplia acta_historial como fuente oficial de auditoria.
--
-- 1) tipo_evento     -> tipo de evento de auditoria
-- 2) actor_id        -> id del usuario del sistema que actuo (admin/tecnico)
-- 3) actor_nombre    -> nombre del actor (usuario del sistema o firmante)
-- 4) id_token_firma  -> token de firma asociado al evento (si aplica)
--
-- Nota: el proyecto usa ddl-auto=update; la columna tipo_evento (NOT NULL
-- sin default) no pudo crearse sola sobre una tabla con filas, por eso esta
-- migracion manual. Ejecutar UNA sola vez.
-- =============================================================

-- 1) Crear la columna tipo_evento (nullable temporalmente)
ALTER TABLE acta_historial ADD COLUMN tipo_evento VARCHAR(40);

-- 2) Backfill: clasificar las filas existentes por su transicion de estado
UPDATE acta_historial
SET tipo_evento = CASE
    WHEN estado_anterior IS NULL     AND estado_nuevo = 'GENERADA'  THEN 'ACTA_GENERADA'
    WHEN estado_anterior = 'GENERADA' AND estado_nuevo = 'ENVIADA'  THEN 'ACTA_ENVIADA'
    WHEN estado_anterior = 'ENVIADA'  AND estado_nuevo = 'FIRMADA'  THEN 'ACTA_FIRMADA'
    WHEN estado_anterior = 'FIRMADA'  AND estado_nuevo = 'APROBADA' THEN 'ACTA_APROBADA'
    WHEN estado_anterior = 'ENVIADA'  AND estado_nuevo = 'RECHAZADA' THEN 'ACTA_RECHAZADA_USUARIO'
    WHEN estado_anterior = 'FIRMADA'  AND estado_nuevo = 'RECHAZADA' THEN 'ACTA_RECHAZADA_ADMIN'
    WHEN estado_anterior IS NULL     AND estado_nuevo = 'ENVIADA'  THEN 'ACTA_ABIERTA_USUARIO'
    ELSE 'ACTA_GENERADA'
END
WHERE tipo_evento IS NULL;

-- 3) Backfill actor_nombre:
--    - eventos de firma/rechazo de firmante -> nombre del usuario del acta
--    - demas eventos -> valor historico de usuario_accion
UPDATE acta_historial h
SET actor_nombre = CASE
    WHEN h.tipo_evento IN ('ACTA_FIRMADA', 'ACTA_RECHAZADA_USUARIO', 'ACTA_ABIERTA_USUARIO')
         AND (SELECT a.nombre_usuario FROM acta a WHERE a.id_acta = h.id_acta) IS NOT NULL
    THEN (SELECT a.nombre_usuario FROM acta a WHERE a.id_acta = h.id_acta)
    ELSE h.usuario_accion
END
WHERE h.actor_nombre IS NULL;

-- 4) Fijar NOT NULL sobre tipo_evento ya poblado
ALTER TABLE acta_historial ALTER COLUMN tipo_evento SET NOT NULL;

-- 5) Ampliar usuario_accion (puede contener ahora nombre + cedula del firmante)
ALTER TABLE acta_historial ALTER COLUMN usuario_accion TYPE VARCHAR(100);
