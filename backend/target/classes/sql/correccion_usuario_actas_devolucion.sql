/*
=====================================================================
CORRECCIÓN CONCEPTUAL: USUARIO EN ACTAS DE DEVOLUCIÓN
=====================================================================

Regla de negocio (verificada en DevolucionService y frontend):

  ACTA DE ENTREGA  -> Usuario  = quien RECIBE el equipo (entregado_a)
                       Técnico = quien entrega el trámite (JWT)
  ACTA DE DEVOLUCIÓN -> Usuario  = quien ENTREGA el equipo (entregado_por)
                         Técnico = quien recibe el trámite / la devolución (JWT)

BUG HISTÓRICO
-------------
En las actas de devolución, el campo `acta.nombre_usuario` se guardó con la
persona que RECIBE el equipo (recibido_por = el técnico), cuando según la
regla debe ser quien ENTREGA el equipo (entregado_por = el usuario).

Causa raíz (ya corregida en código):
- backend .../service/DevolucionService.java, persistirActa() usaba
  `.nombreUsuario(request.getRecibido_por())`  ->  ahora .getEntregado_por()
  (este script corrige SOLO los registros históricos ya persistidos)

El request original completo quedó guardado en `acta.datos_originales` (JSON),
por lo que el dato correcto (entregado_por) está disponible para cada acta.

QUÉ CORRIGE ESTE SCRIPT
-----------------------
1. nombre_usuario  = entregado_por   (de datos_originales)  — el usuario correcto
   (solo cuando existe un entregado_por no vacío en el JSON original)
2. cedula_usuario  = cedula          (de datos_originales)  — ya era correcto,
   se reafirma para el caso real donde la cédula es "de quien entrega"

Seguro: cuando el registro fue de prueba con recibido_por == entregado_por
(ambos el mismo valor, ej. "tttt"), la corrección deja el mismo valor (no-op).

PREVIEW (no cambia nada) — revisar antes de ejecutar:
  SELECT id_acta, nombre_usuario AS actual, entregado_por AS correcto
  FROM acta WHERE tipo_acta = 'DEVOLUCION'
     AND datos_originales IS NOT NULL;

EJECUTAR:
  CD backend; psql -U postgres -d SaucoDB -f src/main/resources/sql/correccion_usuario_actas_devolucion.sql
=====================================================================
*/

BEGIN;

-- Vista previa de lo que se va a corregir (respaldar antes de UPDATE real)
SELECT id_acta,
       nombre_usuario AS nombre_actual,
       COALESCE(cast(datos_originales::json->>'entregado_por' AS varchar), nombre_usuario) AS nombre_correcto
FROM acta
WHERE tipo_acta = 'DEVOLUCION'
  AND datos_originales IS NOT NULL
ORDER BY id_acta;

-- 1) Corregir nombre_usuario = quien entrega el equipo (entregado_por)
UPDATE acta
SET nombre_usuario = cast(datos_originales::json->>'entregado_por' AS varchar)
WHERE tipo_acta = 'DEVOLUCION'
  AND datos_originales IS NOT NULL
  AND datos_originales::json->>'entregado_por' IS NOT NULL
  AND length(trim(datos_originales::json->>'entregado_por')) > 0
  AND trim(nombre_usuario) IS DISTINCT FROM trim(datos_originales::json->>'entregado_por');

-- 2) Reafirmar cedula_usuario = cédula de quien entrega (cedula)
UPDATE acta
SET cedula_usuario = cast(datos_originales::json->>'cedula' AS varchar)
WHERE tipo_acta = 'DEVOLUCION'
  AND datos_originales IS NOT NULL
  AND datos_originales::json->>'cedula' IS NOT NULL
  AND length(trim(datos_originales::json->>'cedula')) > 0
  AND COALESCE(trim(cedula_usuario),'') IS DISTINCT FROM trim(datos_originales::json->>'cedula');

-- Verificación post-corrección
SELECT id_acta, nombre_usuario AS nombre, cedula_usuario AS cedula
FROM acta
WHERE tipo_acta = 'DEVOLUCION'
ORDER BY id_acta;

COMMIT;
