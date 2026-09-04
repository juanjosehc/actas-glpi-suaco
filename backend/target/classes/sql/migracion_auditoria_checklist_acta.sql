-- =====================================================
-- Migracion: Checklist como documento asociado al Acta
-- =====================================================
-- Contexto: el Checklist no es una entidad de negocio independiente, forma
-- parte del expediente documental de la misma Acta. La entidad auditada debe
-- ser siempre ACTA {id}; el documento concreto (Acta, Checklist, futuros
-- anexos) se refleja en el detalle del evento.
--
-- Antes: entidad='CHECKLIST', entidad_id=<id acta>  (entidad independiente)
-- Ahora: entidad='ACTA',   entidad_id=<id acta>     (documento asociado)
--
-- Solo corrige entidad; el detalle se preserva intacto para conservar que
-- documento del expediente se visualizo. El codigo nuevo (ActaService /
-- FirmaService) ya registra entidad='ACTA' con detalle diferenciado.
-- Idempotente: no afecta filas con entidad distinta ni ya corregidas.
-- =====================================================

UPDATE auditoria_sistema
SET    entidad = 'ACTA'
WHERE  entidad = 'CHECKLIST';