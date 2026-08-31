-- =====================================================
-- MIGRACIÓN: Acta Tipo FORMATEO
-- Permite persistir actas de formateo seguro en la tabla `acta`.
-- =====================================================
-- Contexto: el check acta_tipo_acta_check se creó manualmente con solo
-- ENTREGA/DEVOLUCION. ddl-auto: update NO puede alterar constraints
-- existentes; al intentar guardar TipoActa.FORMATEO el insert viola el
-- check y la transacción hace rollback (el PDF/ZIP se generan en disco,
-- pero la fila nunca existe → no aparece en el listado de actas).
--
-- Ejecutar a mano (igual que migracion_auditoria_acta_historial.sql):
--   psql -h localhost -U postgres -d SaucoDB -f migracion_tipo_acta_formateo.sql
--
-- SOLO se elimina y recrea el check. No toca datos existentes.
-- =====================================================

ALTER TABLE acta DROP CONSTRAINT acta_tipo_acta_check;

ALTER TABLE acta ADD CONSTRAINT acta_tipo_acta_check
    CHECK (tipo_acta IN ('ENTREGA', 'DEVOLUCION', 'FORMATEO'));