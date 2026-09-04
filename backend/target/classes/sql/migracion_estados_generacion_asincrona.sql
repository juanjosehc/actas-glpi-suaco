-- =====================================================
-- MIGRACIÓN: Estados de generación asíncrona de actas
-- Agrega GENERANDO_DOCUMENTOS y GENERACION_FALLIDA al dominio de
-- estado de las actas (y del historial).
-- =====================================================
-- Contexto: la generación de documentos (DOCX→ZIP→PDF) pasa a segundo
-- plano tras el POST. La acta se persiste de inmediato en
-- GENERANDO_DOCUMENTOS y el hilo async la lleva a GENERADA (o a
-- GENERACION_FALLIDA si algo falla). Los CHECK constraints manuales
-- (acta_estado_check y los 2 de acta_historial) fueron creados con el
-- dominio viejo (GENERADA/ENVIADA/FIRMADA/APROBADA/RECHAZADA);
-- ddl-auto: update NO puede alterar constraints existentes, así que
-- hay que eliminarlos y recrearlos con la lista completa.
--
-- Ejecutar a mano (igual que migracion_tipo_acta_formateo.sql):
--   psql -h localhost -U postgres -d SaucoDB -f migracion_estados_generacion_asincrona.sql
--
-- SOLO se eliminan y recrean los checks. No toca datos existentes.
-- =====================================================

ALTER TABLE acta DROP CONSTRAINT IF EXISTS acta_estado_check;

ALTER TABLE acta ADD CONSTRAINT acta_estado_check
    CHECK (estado IN ('GENERADA', 'ENVIADA', 'FIRMADA', 'APROBADA',
                      'RECHAZADA', 'GENERANDO_DOCUMENTOS', 'GENERACION_FALLIDA'));

ALTER TABLE acta_historial DROP CONSTRAINT IF EXISTS acta_historial_estado_anterior_check;

ALTER TABLE acta_historial ADD CONSTRAINT acta_historial_estado_anterior_check
    CHECK (estado_anterior IS NULL OR estado_anterior IN ('GENERADA', 'ENVIADA', 'FIRMADA',
                      'APROBADA', 'RECHAZADA', 'GENERANDO_DOCUMENTOS', 'GENERACION_FALLIDA'));

ALTER TABLE acta_historial DROP CONSTRAINT IF EXISTS acta_historial_estado_nuevo_check;

ALTER TABLE acta_historial ADD CONSTRAINT acta_historial_estado_nuevo_check
    CHECK (estado_nuevo IS NULL OR estado_nuevo IN ('GENERADA', 'ENVIADA', 'FIRMADA',
                      'APROBADA', 'RECHAZADA', 'GENERANDO_DOCUMENTOS', 'GENERACION_FALLIDA'));