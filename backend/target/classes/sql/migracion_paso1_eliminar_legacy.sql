-- =====================================================================
-- MIGRACION PASO 1 — ELIMINAR MODELO LEGACY
-- Base: SaucoDB (PostgreSQL 18)  |  Fecha: 2026-09-03
--
-- Elimina:
--   1. acta.id_asignacion        (columna nunca poblada: 0/90 NULL, sin FK)
--   2. tablas asignacion, dispositivo, marca, tipo  (0 filas; sin entity JPA)
--   3. sequences asociadas (4)  (los indices legacy los arrastra el DROP TABLE)
--
-- Riesgo:        MUY BAJO. Sin datos que preservar; sin dependencias de codigo.
-- Dependencias:  misma release que la limpieza de Acta.idAsignacion
--                (backend/src/main/java/com/empresa/actas/acta/entity/Acta.java)
--                y de la whitelist de sort en ActaController.java.
-- Rollback:      bloque comentado al final del archivo.
-- Tablas:        acta (1 columna), asignacion (DROP), dispositivo (DROP),
--                marca (DROP), tipo (DROP).
-- Validaciones:  queries POSTCHECK al final (antes del bloque de rollback).
-- =====================================================================

BEGIN;

-- ------------------------------------------------------------------
-- PRECHECKS: abortar transaccion si los datos no son los esperados
-- ------------------------------------------------------------------
DO $$
BEGIN
    IF (SELECT count(*) FROM asignacion)   > 0 THEN
        RAISE EXCEPTION 'ABORT: asignacion tiene % filas', (SELECT count(*) FROM asignacion);
    END IF;
    IF (SELECT count(*) FROM dispositivo)  > 0 THEN
        RAISE EXCEPTION 'ABORT: dispositivo tiene % filas', (SELECT count(*) FROM dispositivo);
    END IF;
    IF (SELECT count(*) FROM marca)        > 0 THEN
        RAISE EXCEPTION 'ABORT: marca tiene % filas', (SELECT count(*) FROM marca);
    END IF;
    IF (SELECT count(*) FROM tipo)         > 0 THEN
        RAISE EXCEPTION 'ABORT: tipo tiene % filas', (SELECT count(*) FROM tipo);
    END IF;
    IF EXISTS (SELECT 1 FROM acta WHERE id_asignacion IS NOT NULL) THEN
        RAISE EXCEPTION 'ABORT: acta.id_asignacion tiene datos';
    END IF;
END $$;

-- ------------------------------------------------------------------
-- 1. Columna huerfana en acta
-- ------------------------------------------------------------------
ALTER TABLE acta DROP COLUMN id_asignacion;

-- ------------------------------------------------------------------
-- 2. Tablas legacy (DROP arrastra indices y FKs internas)
-- ------------------------------------------------------------------
DROP TABLE asignacion;
DROP TABLE dispositivo;
DROP TABLE marca;
DROP TABLE tipo;

-- ------------------------------------------------------------------
-- 3. Sequences sin uso
-- ------------------------------------------------------------------
DROP SEQUENCE asignacion_id_asignacion_seq;
DROP SEQUENCE dispositivo_id_dispositivo_seq;
DROP SEQUENCE marca_id_marca_seq;
DROP SEQUENCE tipo_id_tipo_seq;

COMMIT;

-- =====================================================================
-- POSTCHECK (ejecutar DESPUES del COMMIT):
-- ---------------------------------------------------------------------
-- SELECT count(*) AS tablas_esperadas_10
--   FROM information_schema.tables WHERE table_schema = 'public';
--
-- SELECT column_name FROM information_schema.columns
--  WHERE table_schema='public' AND table_name='acta'
--    AND column_name = 'id_asignacion';          -- 0 filas
--
-- SELECT count(*) AS columnas_acta_esperadas_34
--   FROM information_schema.columns
--  WHERE table_schema='public' AND table_name='acta';
-- =====================================================================

-- =====================================================================
-- ROLLBACK PASO 1 (restaurar estructura, datos eran 0)
-- ---------------------------------------------------------------------
-- BEGIN;
-- ALTER TABLE acta ADD COLUMN id_asignacion bigint;
--
-- CREATE TABLE tipo (
--     id_tipo bigint NOT NULL,
--     nombre  varchar(100) NOT NULL,
--     CONSTRAINT tipo_pkey PRIMARY KEY (id_tipo),
--     CONSTRAINT tipo_nombre_key UNIQUE (nombre)
-- );
--
-- CREATE TABLE marca (
--     id_marca bigint NOT NULL,
--     nombre   varchar(100) NOT NULL,
--     id_tipo  bigint NOT NULL,
--     CONSTRAINT marca_pkey PRIMARY KEY (id_marca),
--     CONSTRAINT fk_marca_tipo FOREIGN KEY (id_tipo) REFERENCES tipo (id_tipo)
-- );
--
-- CREATE TABLE dispositivo (
--     id_dispositivo bigint NOT NULL,
--     numero_serie   varchar(100) NOT NULL,
--     numero_placa   varchar(100),
--     descripcion    text,
--     estado         varchar(50) NOT NULL,
--     id_marca       bigint NOT NULL,
--     CONSTRAINT dispositivo_pkey PRIMARY KEY (id_dispositivo),
--     CONSTRAINT dispositivo_numero_serie_key UNIQUE (numero_serie),
--     CONSTRAINT dispositivo_numero_placa_key UNIQUE (numero_placa),
--     CONSTRAINT fk_dispositivo_marca FOREIGN KEY (id_marca) REFERENCES marca (id_marca)
-- );
--
-- CREATE TABLE asignacion (
--     id_asignacion     bigint NOT NULL,
--     id_dispositivo    bigint NOT NULL,
--     cedula_usuario    varchar(20) NOT NULL,
--     nombre_usuario    varchar(100) NOT NULL,
--     correo_usuario    varchar(100) NOT NULL,
--     fecha_asignacion  timestamp NOT NULL,
--     fecha_entrega     timestamp,
--     fecha_devolucion  timestamp,
--     CONSTRAINT asignacion_pkey PRIMARY KEY (id_asignacion),
--     CONSTRAINT fk_asignacion_dispositivo FOREIGN KEY (id_dispositivo) REFERENCES dispositivo (id_dispositivo)
-- );
--
-- CREATE SEQUENCE tipo_id_tipo_seq OWNED BY tipo.id_tipo;
-- CREATE SEQUENCE marca_id_marca_seq OWNED BY marca.id_marca;
-- CREATE SEQUENCE dispositivo_id_dispositivo_seq OWNED BY dispositivo.id_dispositivo;
-- CREATE SEQUENCE asignacion_id_asignacion_seq OWNED BY asignacion.id_asignacion;
-- COMMIT;
-- =====================================================================