--
-- PostgreSQL database dump
--

\restrict RM5JaGLcmRFgcVCgX4gtoAEr7ON3E1E9QeBaDz0QIqTTUBtX0EgMm5LlQPZ6VnE

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.usuario DROP CONSTRAINT IF EXISTS fk_usuario_rol;
ALTER TABLE IF EXISTS ONLY public.marca DROP CONSTRAINT IF EXISTS fk_marca_tipo;
ALTER TABLE IF EXISTS ONLY public.dispositivo DROP CONSTRAINT IF EXISTS fk_dispositivo_marca;
ALTER TABLE IF EXISTS ONLY public.asignacion DROP CONSTRAINT IF EXISTS fk_asignacion_dispositivo;
DROP INDEX IF EXISTS public.idx_firma_token;
ALTER TABLE IF EXISTS ONLY public.usuario DROP CONSTRAINT IF EXISTS usuario_pkey;
ALTER TABLE IF EXISTS ONLY public.usuario DROP CONSTRAINT IF EXISTS usuario_nombre_usuario_key;
ALTER TABLE IF EXISTS ONLY public.usuario_firma DROP CONSTRAINT IF EXISTS usuario_firma_pkey;
ALTER TABLE IF EXISTS ONLY public.usuario DROP CONSTRAINT IF EXISTS usuario_correo_key;
ALTER TABLE IF EXISTS ONLY public.usuario DROP CONSTRAINT IF EXISTS usuario_cedula_key;
ALTER TABLE IF EXISTS ONLY public.firma_otp DROP CONSTRAINT IF EXISTS ukewi7emeer4le2qor3ils7gxjx;
ALTER TABLE IF EXISTS ONLY public.usuario_firma DROP CONSTRAINT IF EXISTS ukc5sqrpyaagkp3741jhtuulibi;
ALTER TABLE IF EXISTS ONLY public.tipo DROP CONSTRAINT IF EXISTS tipo_pkey;
ALTER TABLE IF EXISTS ONLY public.tipo DROP CONSTRAINT IF EXISTS tipo_nombre_key;
ALTER TABLE IF EXISTS ONLY public.rol DROP CONSTRAINT IF EXISTS rol_pkey;
ALTER TABLE IF EXISTS ONLY public.rol DROP CONSTRAINT IF EXISTS rol_nombre_key;
ALTER TABLE IF EXISTS ONLY public.marca DROP CONSTRAINT IF EXISTS marca_pkey;
ALTER TABLE IF EXISTS ONLY public.jwt_revocado DROP CONSTRAINT IF EXISTS jwt_revocado_pkey;
ALTER TABLE IF EXISTS ONLY public.firma_token DROP CONSTRAINT IF EXISTS firma_token_token_key;
ALTER TABLE IF EXISTS ONLY public.firma_token DROP CONSTRAINT IF EXISTS firma_token_pkey;
ALTER TABLE IF EXISTS ONLY public.firma_token DROP CONSTRAINT IF EXISTS firma_token_id_acta_key;
ALTER TABLE IF EXISTS ONLY public.firma_otp DROP CONSTRAINT IF EXISTS firma_otp_pkey;
ALTER TABLE IF EXISTS ONLY public.evidencia DROP CONSTRAINT IF EXISTS evidencia_pkey;
ALTER TABLE IF EXISTS ONLY public.dispositivo DROP CONSTRAINT IF EXISTS dispositivo_pkey;
ALTER TABLE IF EXISTS ONLY public.dispositivo DROP CONSTRAINT IF EXISTS dispositivo_numero_serie_key;
ALTER TABLE IF EXISTS ONLY public.dispositivo DROP CONSTRAINT IF EXISTS dispositivo_numero_placa_key;
ALTER TABLE IF EXISTS ONLY public.auditoria_sistema DROP CONSTRAINT IF EXISTS auditoria_sistema_pkey;
ALTER TABLE IF EXISTS ONLY public.asignacion DROP CONSTRAINT IF EXISTS asignacion_pkey;
ALTER TABLE IF EXISTS ONLY public.acta DROP CONSTRAINT IF EXISTS acta_pkey;
ALTER TABLE IF EXISTS ONLY public.acta_historial DROP CONSTRAINT IF EXISTS acta_historial_pkey;
ALTER TABLE IF EXISTS public.usuario ALTER COLUMN id_usuario DROP DEFAULT;
ALTER TABLE IF EXISTS public.tipo ALTER COLUMN id_tipo DROP DEFAULT;
ALTER TABLE IF EXISTS public.rol ALTER COLUMN id_rol DROP DEFAULT;
ALTER TABLE IF EXISTS public.marca ALTER COLUMN id_marca DROP DEFAULT;
ALTER TABLE IF EXISTS public.firma_token ALTER COLUMN id_token DROP DEFAULT;
ALTER TABLE IF EXISTS public.evidencia ALTER COLUMN id_evidencia DROP DEFAULT;
ALTER TABLE IF EXISTS public.dispositivo ALTER COLUMN id_dispositivo DROP DEFAULT;
ALTER TABLE IF EXISTS public.asignacion ALTER COLUMN id_asignacion DROP DEFAULT;
DROP SEQUENCE IF EXISTS public.usuario_id_usuario_seq;
DROP TABLE IF EXISTS public.usuario_firma;
DROP TABLE IF EXISTS public.usuario;
DROP SEQUENCE IF EXISTS public.tipo_id_tipo_seq;
DROP TABLE IF EXISTS public.tipo;
DROP SEQUENCE IF EXISTS public.rol_id_rol_seq;
DROP TABLE IF EXISTS public.rol;
DROP SEQUENCE IF EXISTS public.marca_id_marca_seq;
DROP TABLE IF EXISTS public.marca;
DROP TABLE IF EXISTS public.jwt_revocado;
DROP SEQUENCE IF EXISTS public.firma_token_id_token_seq;
DROP TABLE IF EXISTS public.firma_token;
DROP TABLE IF EXISTS public.firma_otp;
DROP SEQUENCE IF EXISTS public.evidencia_id_evidencia_seq;
DROP TABLE IF EXISTS public.evidencia;
DROP SEQUENCE IF EXISTS public.dispositivo_id_dispositivo_seq;
DROP TABLE IF EXISTS public.dispositivo;
DROP TABLE IF EXISTS public.auditoria_sistema;
DROP SEQUENCE IF EXISTS public.asignacion_id_asignacion_seq;
DROP TABLE IF EXISTS public.asignacion;
DROP TABLE IF EXISTS public.acta_historial;
DROP TABLE IF EXISTS public.acta;
-- *not* dropping schema, since initdb creates it
--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS '';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: acta; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.acta (
    id_acta bigint NOT NULL,
    cedula_usuario character varying(20),
    contenido_html text,
    correo_usuario character varying(100),
    descripcion_equipo character varying(255),
    estado character varying(20) NOT NULL,
    fecha_aprobacion timestamp(6) without time zone,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    fecha_envio timestamp(6) without time zone,
    fecha_firma timestamp(6) without time zone,
    id_asignacion bigint,
    id_tecnico bigint NOT NULL,
    nombre_usuario character varying(100),
    observacion_rechazo character varying(500),
    placa_equipo character varying(50),
    ruta_pdf character varying(500),
    serial_equipo character varying(50),
    ticket_glpi bigint,
    tipo_acta character varying(20) NOT NULL,
    accesorios character varying(255),
    cargo character varying(100),
    disco_duro character varying(100),
    empresa character varying(100),
    estado_equipo character varying(50),
    lugar_trabajo character varying(100),
    marca_modelo character varying(100),
    memoria_ram character varying(50),
    monitor character varying(100),
    observaciones character varying(500),
    procesador character varying(100),
    sistema_operativo character varying(100),
    datos_originales text,
    fecha_rechazo timestamp(6) without time zone,
    ruta_pdf_checklist character varying(500),
    ruta_zip character varying(500),
    CONSTRAINT acta_estado_check CHECK (((estado)::text = ANY ((ARRAY['GENERADA'::character varying, 'ENVIADA'::character varying, 'FIRMADA'::character varying, 'APROBADA'::character varying, 'RECHAZADA'::character varying, 'GENERANDO_DOCUMENTOS'::character varying, 'GENERACION_FALLIDA'::character varying])::text[]))),
    CONSTRAINT acta_tipo_acta_check CHECK (((tipo_acta)::text = ANY ((ARRAY['ENTREGA'::character varying, 'DEVOLUCION'::character varying, 'FORMATEO'::character varying])::text[])))
);


ALTER TABLE public.acta OWNER TO postgres;

--
-- Name: acta_historial; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.acta_historial (
    id_historial bigint NOT NULL,
    estado_anterior character varying(20),
    estado_nuevo character varying(20) NOT NULL,
    fecha_cambio timestamp(6) without time zone NOT NULL,
    id_acta bigint NOT NULL,
    observacion character varying(500),
    usuario_accion character varying(100) NOT NULL,
    actor_id bigint,
    actor_nombre character varying(150),
    id_token_firma bigint,
    tipo_evento character varying(40) NOT NULL,
    CONSTRAINT acta_historial_estado_anterior_check CHECK (((estado_anterior IS NULL) OR ((estado_anterior)::text = ANY ((ARRAY['GENERADA'::character varying, 'ENVIADA'::character varying, 'FIRMADA'::character varying, 'APROBADA'::character varying, 'RECHAZADA'::character varying, 'GENERANDO_DOCUMENTOS'::character varying, 'GENERACION_FALLIDA'::character varying])::text[])))),
    CONSTRAINT acta_historial_estado_nuevo_check CHECK (((estado_nuevo IS NULL) OR ((estado_nuevo)::text = ANY ((ARRAY['GENERADA'::character varying, 'ENVIADA'::character varying, 'FIRMADA'::character varying, 'APROBADA'::character varying, 'RECHAZADA'::character varying, 'GENERANDO_DOCUMENTOS'::character varying, 'GENERACION_FALLIDA'::character varying])::text[]))))
);


ALTER TABLE public.acta_historial OWNER TO postgres;

--
-- Name: acta_historial_id_historial_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.acta_historial ALTER COLUMN id_historial ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.acta_historial_id_historial_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: acta_id_acta_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.acta ALTER COLUMN id_acta ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.acta_id_acta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: asignacion; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.asignacion (
    id_asignacion bigint NOT NULL,
    id_dispositivo bigint NOT NULL,
    cedula_usuario character varying(20) NOT NULL,
    nombre_usuario character varying(200) NOT NULL,
    correo_usuario character varying(150) NOT NULL,
    fecha_asignacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_entrega timestamp without time zone,
    fecha_devolucion timestamp without time zone
);


ALTER TABLE public.asignacion OWNER TO postgres;

--
-- Name: asignacion_id_asignacion_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.asignacion_id_asignacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.asignacion_id_asignacion_seq OWNER TO postgres;

--
-- Name: asignacion_id_asignacion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.asignacion_id_asignacion_seq OWNED BY public.asignacion.id_asignacion;


--
-- Name: auditoria_sistema; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.auditoria_sistema (
    id_auditoria bigint NOT NULL,
    detalle character varying(500),
    entidad character varying(50),
    entidad_id character varying(100),
    fecha_evento timestamp(6) without time zone NOT NULL,
    ip_direccion character varying(45),
    recurso character varying(255),
    tipo_evento character varying(40) NOT NULL,
    usuario_id bigint,
    usuario_nombre character varying(150),
    CONSTRAINT auditoria_sistema_tipo_evento_check CHECK (((tipo_evento)::text = ANY ((ARRAY['LOGIN_EXITOSO'::character varying, 'LOGIN_FALLIDO'::character varying, 'LOGOUT'::character varying, 'ACCESO_DENEGADO'::character varying, 'DOCUMENTO_VISTO'::character varying, 'EVIDENCIA_VISTA'::character varying, 'TOKEN_EXPIRADO'::character varying, 'TOKEN_INVALIDO'::character varying, 'OTP_GENERADO'::character varying, 'OTP_ENVIADO'::character varying, 'OTP_ENVIO_FALLIDO'::character varying, 'OTP_VALIDADO'::character varying, 'OTP_INVALIDO'::character varying, 'OTP_BLOQUEADO'::character varying, 'OTP_EXPIRADO'::character varying, 'OTP_REENVIADO'::character varying, 'FIRMA_TECNICO_REGISTRADA'::character varying, 'FIRMA_TECNICO_ACTUALIZADA'::character varying, 'FIRMA_TECNICO_ELIMINADA'::character varying])::text[])))
);


ALTER TABLE public.auditoria_sistema OWNER TO postgres;

--
-- Name: auditoria_sistema_id_auditoria_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.auditoria_sistema ALTER COLUMN id_auditoria ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auditoria_sistema_id_auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: dispositivo; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dispositivo (
    id_dispositivo bigint NOT NULL,
    numero_serie character varying(150) NOT NULL,
    numero_placa character varying(100),
    descripcion text,
    estado character varying(50) NOT NULL,
    id_marca bigint NOT NULL
);


ALTER TABLE public.dispositivo OWNER TO postgres;

--
-- Name: dispositivo_id_dispositivo_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.dispositivo_id_dispositivo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.dispositivo_id_dispositivo_seq OWNER TO postgres;

--
-- Name: dispositivo_id_dispositivo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.dispositivo_id_dispositivo_seq OWNED BY public.dispositivo.id_dispositivo;


--
-- Name: evidencia; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.evidencia (
    id_evidencia bigint NOT NULL,
    id_acta bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    ruta_archivo character varying(500) NOT NULL,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.evidencia OWNER TO postgres;

--
-- Name: evidencia_id_evidencia_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.evidencia_id_evidencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.evidencia_id_evidencia_seq OWNER TO postgres;

--
-- Name: evidencia_id_evidencia_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.evidencia_id_evidencia_seq OWNED BY public.evidencia.id_evidencia;


--
-- Name: firma_otp; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.firma_otp (
    id_otp bigint NOT NULL,
    codigo_hash character varying(60) NOT NULL,
    correo_destino character varying(255) NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    fecha_expiracion timestamp(6) without time zone NOT NULL,
    fecha_validacion timestamp(6) without time zone,
    id_token_firma bigint NOT NULL,
    intentos integer NOT NULL,
    sesion character varying(255),
    usado boolean NOT NULL
);


ALTER TABLE public.firma_otp OWNER TO postgres;

--
-- Name: firma_otp_id_otp_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.firma_otp ALTER COLUMN id_otp ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.firma_otp_id_otp_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: firma_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.firma_token (
    id_token bigint NOT NULL,
    id_acta bigint NOT NULL,
    token character varying(36) NOT NULL,
    utilizado boolean DEFAULT false NOT NULL,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_utilizacion timestamp without time zone,
    fecha_expiracion timestamp(6) without time zone
);


ALTER TABLE public.firma_token OWNER TO postgres;

--
-- Name: firma_token_id_token_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.firma_token_id_token_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.firma_token_id_token_seq OWNER TO postgres;

--
-- Name: firma_token_id_token_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.firma_token_id_token_seq OWNED BY public.firma_token.id_token;


--
-- Name: jwt_revocado; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.jwt_revocado (
    jti character varying(64) NOT NULL,
    fecha_expiracion_token timestamp(6) without time zone NOT NULL,
    fecha_revocacion timestamp(6) without time zone NOT NULL,
    usuario character varying(50) NOT NULL
);


ALTER TABLE public.jwt_revocado OWNER TO postgres;

--
-- Name: marca; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.marca (
    id_marca bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    id_tipo bigint NOT NULL
);


ALTER TABLE public.marca OWNER TO postgres;

--
-- Name: marca_id_marca_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.marca_id_marca_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.marca_id_marca_seq OWNER TO postgres;

--
-- Name: marca_id_marca_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.marca_id_marca_seq OWNED BY public.marca.id_marca;


--
-- Name: rol; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rol (
    id_rol bigint NOT NULL,
    nombre character varying(50) NOT NULL
);


ALTER TABLE public.rol OWNER TO postgres;

--
-- Name: rol_id_rol_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.rol_id_rol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rol_id_rol_seq OWNER TO postgres;

--
-- Name: rol_id_rol_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.rol_id_rol_seq OWNED BY public.rol.id_rol;


--
-- Name: tipo; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tipo (
    id_tipo bigint NOT NULL,
    nombre character varying(100) NOT NULL
);


ALTER TABLE public.tipo OWNER TO postgres;

--
-- Name: tipo_id_tipo_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tipo_id_tipo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tipo_id_tipo_seq OWNER TO postgres;

--
-- Name: tipo_id_tipo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tipo_id_tipo_seq OWNED BY public.tipo.id_tipo;


--
-- Name: usuario; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuario (
    id_usuario bigint NOT NULL,
    cedula character varying(20) NOT NULL,
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    nombre_usuario character varying(50) NOT NULL,
    correo character varying(100) NOT NULL,
    password_hash character varying(255) NOT NULL,
    cargo character varying(100),
    empresa character varying(100),
    lugar_trabajo character varying(150),
    bloqueado boolean DEFAULT false NOT NULL,
    id_rol bigint NOT NULL
);


ALTER TABLE public.usuario OWNER TO postgres;

--
-- Name: usuario_firma; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuario_firma (
    id_firma bigint NOT NULL,
    fecha_actualizacion timestamp(6) without time zone NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    ruta_firma character varying(255) NOT NULL,
    usuario_id bigint NOT NULL
);


ALTER TABLE public.usuario_firma OWNER TO postgres;

--
-- Name: usuario_firma_id_firma_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.usuario_firma ALTER COLUMN id_firma ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.usuario_firma_id_firma_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuario_id_usuario_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.usuario_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuario_id_usuario_seq OWNER TO postgres;

--
-- Name: usuario_id_usuario_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.usuario_id_usuario_seq OWNED BY public.usuario.id_usuario;


--
-- Name: asignacion id_asignacion; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asignacion ALTER COLUMN id_asignacion SET DEFAULT nextval('public.asignacion_id_asignacion_seq'::regclass);


--
-- Name: dispositivo id_dispositivo; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dispositivo ALTER COLUMN id_dispositivo SET DEFAULT nextval('public.dispositivo_id_dispositivo_seq'::regclass);


--
-- Name: evidencia id_evidencia; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.evidencia ALTER COLUMN id_evidencia SET DEFAULT nextval('public.evidencia_id_evidencia_seq'::regclass);


--
-- Name: firma_token id_token; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.firma_token ALTER COLUMN id_token SET DEFAULT nextval('public.firma_token_id_token_seq'::regclass);


--
-- Name: marca id_marca; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.marca ALTER COLUMN id_marca SET DEFAULT nextval('public.marca_id_marca_seq'::regclass);


--
-- Name: rol id_rol; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rol ALTER COLUMN id_rol SET DEFAULT nextval('public.rol_id_rol_seq'::regclass);


--
-- Name: tipo id_tipo; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tipo ALTER COLUMN id_tipo SET DEFAULT nextval('public.tipo_id_tipo_seq'::regclass);


--
-- Name: usuario id_usuario; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario ALTER COLUMN id_usuario SET DEFAULT nextval('public.usuario_id_usuario_seq'::regclass);


--
-- Data for Name: acta; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acta (id_acta, cedula_usuario, contenido_html, correo_usuario, descripcion_equipo, estado, fecha_aprobacion, fecha_creacion, fecha_envio, fecha_firma, id_asignacion, id_tecnico, nombre_usuario, observacion_rechazo, placa_equipo, ruta_pdf, serial_equipo, ticket_glpi, tipo_acta, accesorios, cargo, disco_duro, empresa, estado_equipo, lugar_trabajo, marca_modelo, memoria_ram, monitor, observaciones, procesador, sistema_operativo, datos_originales, fecha_rechazo, ruta_pdf_checklist, ruta_zip) FROM stdin;
13	mmmm	\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-07-30 10:12:25.007725	\N	\N	\N	1	mmmm	\N	mmmm	uploads/pdf/Devolucion_FM16ZW3_mmmm.pdf	FM16ZW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
1	123456789	<h1>Acta de Entrega</h1>	juan@test.com	Portatil Dell Latitude 5520	APROBADA	2026-07-28 08:04:16.959657	2026-07-27 12:56:59.454072	2026-07-27 15:20:30.652307	2026-07-27 15:23:17.455937	\N	1	Juan Perez	\N	PLACA001	C:\\Users\\juanhern\\OneDrive - COMPANIA DE FINANCIAMIENTO COMERCIAL COLTEFINANCIERA S.A\\Documentos\\actas-glpi-Suaco\\backend\\uploads\\pdf\\acta_1.pdf	ABC123	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
16		\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-07-30 14:50:46.00195	\N	\N	\N	1	kkkk	\N	kkkk	uploads/pdf/ActaEntrega_JK16ZW3_kkk.pdf	JK16ZW3	123445	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-30","entregado_a":"kkkk","cargo_recibe":"kkkk","entregado_por":"kkkk","cargo_entrega":"kkkk","asunto":"kkk","hardware":[{"tipo":"kkkk","descripcion":"kkkk","programa":"kkkk"}],"equipos":[{"serial":"JK16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"kkkk"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"123445","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
15		\N	\N	Dell Inc. Latitude 3440 Core i5	APROBADA	2026-07-30 14:49:50.804356	2026-07-30 12:32:00.071326	2026-07-30 12:33:00.565298	2026-07-30 12:33:17.37151	\N	1	yyyy	\N	1234	uploads/pdf/ActaEntrega_JK16ZW3_yyyy.pdf	JK16ZW3	12324	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-30","entregado_a":"yyyy","cargo_recibe":"yyyy","entregado_por":"yyyy","cargo_entrega":"yyyy","asunto":"yyyy","hardware":[{"tipo":"yyyy","descripcion":"yyyy","programa":"yyyy"}],"equipos":[{"serial":"JK16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"1234"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12324","observaciones":"yyyy","sistema_operativo":"Mac OS"}	\N	\N	\N
123	12345678	<p>Entrega equipo</p>	cperez@test.local	Laptop prueba SEC-010	ENVIADA	\N	2026-09-02 10:26:22.587639	2026-09-02 10:34:38.836165	\N	\N	41	Carlos Perez	\N	PL2001	\N	SN20260001	\N	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"ticketGlpi":null,"fecha":"2026-09-02","tipoActa":"ENTREGA","cedulaUsuario":"12345678","nombreUsuario":"Carlos Perez","correoUsuario":"cperez@test.local","serialEquipo":"SN20260001","placaEquipo":"PL2001","descripcionEquipo":"Laptop prueba SEC-010","contenidoHtml":"<p>Entrega equipo</p>","rutaPdf":null,"datosOriginales":null}	\N	\N	\N
4	\N	<div class="acta-document">\n    <div class="acta-header">\n        <div class="acta-header-left">\n            <img class="acta-logo" src="img/LogoC.png" alt="Coltefinanciera">\n            <div class="acta-company">COMPANIA DE FINANCIAMIENTO COMERCIAL COLTEFINANCIERA S.A.</div>\n            <div class="acta-note">NIT 890.000.000-0</div>\n        </div>\n        <div class="acta-header-right">\n            <div class="acta-code">ACTA N° <strong>Pendiente</strong></div>\n            <div class="acta-date">Fecha: <strong>2026-07-29</strong></div>\n        </div>\n    </div>\n\n    <div class="acta-title-section">\n        <h1 class="acta-title">MEMORANDO DE ENTREGA DE DISPOSITIVOS</h1>\n        <p class="acta-subtitle">Por medio del presente documento se deja constancia de la entrega de equipos de computo y/o dispositivos electronicos al colaborador que se menciona a continuacion:</p>\n    </div>\n\n    <div class="acta-body">\n        <table class="acta-table">\n            <tr>\n                <td class="acta-label">FECHA DE ENTREGA:</td>\n                <td class="acta-value">2026-07-29</td>\n            </tr>\n            <tr>\n                <td class="acta-label">FUNCIONARIO QUE RECIBE:</td>\n                <td class="acta-value">aa</td>\n            </tr>\n            <tr>\n                <td class="acta-label">NUMERO DE CEDULA:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">CORREO CORPORATIVO:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">CARGO:</td>\n                <td class="acta-value">aa</td>\n            </tr>\n            <tr>\n                <td class="acta-label">DEPARTAMENTO / SEDE:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">EMPRESA:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">TICKET GLPI:</td>\n                <td class="acta-value">12313</td>\n            </tr>\n        </table>\n\n        <h2 class="acta-subsection">DATOS DEL EQUIPO ENTREGADO</h2>\n\n        <table class="acta-table">\n            <tr>\n                <td class="acta-label">TIPO DE EQUIPO:</td>\n                <td class="acta-value">HP HP Laptop 14-fq1xxx Ryzen 5</td>\n            </tr>\n            <tr>\n                <td class="acta-label">MARCA / MODELO:</td>\n                <td class="acta-value">HP HP Laptop 14-fq1xxx Ryzen 5</td>\n            </tr>\n            <tr>\n                <td class="acta-label">NUMERO DE SERIE:</td>\n                <td class="acta-value">5CD2256W6H</td>\n            </tr>\n            <tr>\n                <td class="acta-label">PLACA INTERNA:</td>\n                <td class="acta-value">12313</td>\n            </tr>\n        </table>\n\n        <h2 class="acta-subsection">ESPECIFICACIONES TECNICAS</h2>\n\n        <table class="acta-table">\n            <tr>\n                <td class="acta-label">PROCESADOR:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">MEMORIA RAM:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">DISCO DURO:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">SISTEMA OPERATIVO:</td>\n                <td class="acta-value">Mac OS</td>\n            </tr>\n            <tr>\n                <td class="acta-label">MONITOR:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n            <tr>\n                <td class="acta-label">ACCESORIOS:</td>\n                <td class="acta-value">________________</td>\n            </tr>\n        </table>\n\n        <h2 class="acta-subsection">OBSERVACIONES</h2>\n        <div class="acta-observations">aaaa</div>\n\n        <div class="acta-clause">\n            <p><strong>CLAUSULA DE RESPONSABILIDAD:</strong> El colaborador declara haber recibido los equipos y dispositivos descritos en el presente documento, en buen estado y funcionamiento, y se compromete a hacer uso adecuado de los mismos, respondiendo por cualquier dano, perdida o deterioro causado por mal uso, negligencia o incumplimiento de las politicas de seguridad informatica establecidas por la compania.</p>\n        </div>\n    </div>\n\n    <div class="acta-footer">\n        <div class="acta-signature-section">\n            <div class="acta-signature-box">\n                <div class="acta-signature-label">ENTREGADO POR:</div>\n                <div class="acta-signature-line"></div>\n                <div class="acta-signature-name">aa</div>\n                <div class="acta-signature-role">Tecnico de Soporte</div>\n            </div>\n            <div class="acta-signature-box">\n                <div class="acta-signature-label">RECIBIDO POR:</div>\n                <div class="acta-signature-line"></div>\n                <div class="acta-signature-name">aa</div>\n                <div class="acta-signature-role">Colaborador</div>\n            </div>\n        </div>\n    </div>\n\n    <div class="acta-watermark">DOCUMENTO DE USO INTERNO - COLTEFINANCIERA</div>\n</div>\n	\N	HP HP Laptop 14-fq1xxx Ryzen 5	GENERADA	\N	2026-07-29 11:26:48.077898	\N	\N	\N	1	aa	\N	12313	\N	5CD2256W6H	12313	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
5		<div class="acta-document" style="font-family:Calibri,sans-serif;font-size:11pt;line-height:1.4;padding:20px 30px;">\n<table style="width:100%;border-collapse:collapse;margin:8px 0;">\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">MEMORANDO DE ENTREGA DE DISPOSITIVOS</td></tr>\n</table>\n<table style="width:100%;border-collapse:collapse;margin:8px 0;">\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Fecha:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Día</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Mes</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Año</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">29</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">07</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">2026</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Entregado a:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">a</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Cargo:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">a</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Entregado por:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Cargo:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">a</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Asunto:</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td></tr>\n</table>\n<p style="margin:0 0 6px 0;">Cordialmente se relaciona el dispositivo que le fue asignado.</p>\n<table style="width:100%;border-collapse:collapse;margin:8px 0;">\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">DESCRIPCION DEL EQUIPO DE COMPUTO</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Marca</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Tipo</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Modelo</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Serial</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Nro. Inventario</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">HP</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Notebook</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">HP Laptop 14-fq1xxx Ryzen 5</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">5CD2256W6H</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">1231</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n</table>\n<p style="margin:0 0 6px 0;">Contenido del Dispositivo</p>\n<table style="width:100%;border-collapse:collapse;margin:8px 0;">\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">HARDWARE</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">SOFTWARE</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Tipo de Hardware</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Descripción</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">Programa</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">aa</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n<tr><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td><td style="border:1px solid #bbb;padding:4px 8px;vertical-align:top;">&nbsp;</td></tr>\n</table>\n<p style="margin:0 0 6px 0;">Atentamente.</p>\n<p style="margin:0 0 6px 0;">                                                                                                  </p>\n<p style="margin:0 0 6px 0;">_______________________                  _____________________</p>\n<p style="margin:0 0 6px 0;">                                                                                                            Recibido por: {{ entregado_a }} </p>\n<p style="margin:0 0 6px 0;">Director de Infraestructura                       Cargo: {{ cargo_recibe }}</p>\n</div>	\N	HP HP Laptop 14-fq1xxx Ryzen 5	GENERADA	\N	2026-07-29 12:08:11.463507	\N	\N	\N	1	a	\N	1231	\N	5CD2256W6H	12313	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
6		\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-07-29 16:09:30.238463	\N	\N	\N	1	ffffffff	\N	123	uploads/pdf/ActaEntrega_123_ffffffff.pdf	123	12314	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
7		\N	\N	HP HP Laptop 14-fq1xxx Ryzen 5	GENERADA	\N	2026-07-30 08:25:51.017041	\N	\N	\N	1	gggg	\N	gggg	C:/Users/juanhern/AppData/Local/Temp/actas_glpi_uploads/pdf/ActaEntrega_5CD2256W6H_gggg.pdf	5CD2256W6H	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
8		\N	\N	HP HP Laptop 14-fq1xxx Ryzen 5	GENERADA	\N	2026-07-30 08:34:54.317344	\N	\N	\N	1	rrrr	\N	rrrrrrrr	uploads/pdf/ActaEntrega_5CD2256W6H_rrrr.pdf	5CD2256W6H	123456	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
14	jjjjj	\N	\N	Dell Inc. Latitude 3440 Core i5	APROBADA	2026-07-30 12:15:24.603441	2026-07-30 10:52:25.500901	2026-07-30 12:14:26.988858	2026-07-30 12:15:14.23315	\N	1	jjjjj	\N	jjjjj	uploads/pdf/acta_14.pdf	FM16ZW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
10	yyyy	\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-30 09:27:17.102024	2026-07-30 09:24:04.63806	2026-07-30 09:24:24.674439	2026-07-30 09:25:10.115704	\N	1	yyyy	\N	yyyy	uploads/pdf/acta_10.pdf	30JZTN3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
9	tttt	\N	\N	Dell Inc. Vostro 3400 Core i5	RECHAZADA	\N	2026-07-30 08:52:58.448416	2026-07-30 08:53:48.859794	2026-07-30 08:55:38.082814	\N	1	tttt	Foto invalida	tttt	uploads/pdf/Devolucion_30JZTN3_tttt.pdf	30JZTN3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
11	oooo	\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-07-30 09:53:04.935123	\N	\N	\N	1	oooo	\N	oooo	uploads/pdf/Devolucion_FM16ZW3_oooo.pdf	FM16ZW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
12	pppp	\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-07-30 10:05:25.903326	\N	\N	\N	1	pppp	\N	pppp	uploads/pdf/Devolucion_FM16ZW3_pppp.pdf	FM16ZW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
21	acta devolucion	\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-31 08:12:46.316471	2026-07-31 07:39:13.057903	2026-07-31 07:39:58.556363	2026-07-31 07:41:49.020383	\N	1	acta devolucion	\N	acta devolucion	uploads/pdf/Devolucion_DYYCQM3_actadevolucion.pdf	DYYCQM3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","recibido_por":"acta devolucion","entregado_por":"acta devolucion","cargo_recibe":"acta devolucion","cargo_entrega":"acta devolucion","cedula":"acta devolucion","area_recibe":"acta devolucion","motivo":"acta devolucion","nombre_jefe":"acta devolucion","cargo_jefe":"acta devolucion","hardware":[{"tipo":"acta devolucion"}],"equipos":[{"serial":"DYYCQM3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"acta devolucion","estado":"acta devolucion"}],"observaciones":"acta devolucion"}	\N	\N	\N
18		\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-31 07:40:11.042654	2026-07-30 15:06:30.682837	2026-07-30 15:06:42.994568	2026-07-30 15:07:10.395731	\N	1	uuuu	\N	uuuu	uploads/pdf/ActaEntrega_DYYCQM3_uuuu.pdf	DYYCQM3	123456	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-30","entregado_a":"uuuu","cargo_recibe":"uuuu","entregado_por":"uuuu","cargo_entrega":"uuuu","asunto":"uuuu","hardware":[{"tipo":"uuuu","descripcion":"uuuu","programa":"uuuu"}],"equipos":[{"serial":"DYYCQM3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"uuuu"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"123456","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
17		\N	\N	Dell Inc. Latitude 3440 Core i5	APROBADA	2026-07-30 15:06:49.38794	2026-07-30 15:00:12.615055	2026-07-30 15:00:39.614111	2026-07-30 15:01:05.623036	\N	1	nnnn	\N	nnnn	uploads/pdf/ActaEntrega_JK16ZW3_nnnn.pdf	JK16ZW3	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-30","entregado_a":"nnnn","cargo_recibe":"nnnn","entregado_por":"nnnn","cargo_entrega":"nnnn","asunto":"nnnn","hardware":[{"tipo":"nnnn","descripcion":"nnnn","programa":"nnnn"}],"equipos":[{"serial":"JK16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"nnnn"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12345","observaciones":"","sistema_operativo":"Windows 11"}	\N	\N	\N
20		\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-31 08:12:47.430054	2026-07-31 07:38:18.693611	2026-07-31 07:39:56.165632	2026-07-31 07:40:54.035757	\N	1	acta entrega	\N	acta entrega	uploads/pdf/ActaEntrega_DYYCQM3_actaentrega.pdf	DYYCQM3	123456	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","entregado_a":"acta entrega","cargo_recibe":"acta entrega","entregado_por":"acta entrega","cargo_entrega":"acta entrega","asunto":"acta entrega","hardware":[{"tipo":"acta entrega","descripcion":"acta entrega","programa":"acta entrega"}],"equipos":[{"serial":"DYYCQM3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"acta entrega"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"123456","observaciones":"acta entrega acta entrega acta entrega","sistema_operativo":"Mac OS"}	\N	\N	\N
124	\N	\N	maria@example.co	Lenovo ThinkCentre M720	GENERADA	\N	2026-09-02 11:34:54.504102	\N	\N	\N	45	Maria Lopez	\N	INV0002	uploads/pdf/ActaEntrega_SERPERF002_Entrega equipo oficina_5cf22891.pdf	SERPERF002	123456789	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","entregado_a":"Maria Lopez","cargo_recibe":"Analista","entregado_por":"Carlos Ruiz","correo":"maria@example.co","cargo_entrega":"Tecnico TI","asunto":"Entrega equipo oficina","hardware":[{"tipo":"Teclado","descripcion":"USB","programa":""}],"equipos":[{"serial":"SERPERF002","marca":"Lenovo","tipo":"Desktop","modelo":"ThinkCentre M720","inventario":"INV0002","estado":"Operativo","gb":"16"}],"checklist":{"manual_usuario":true,"cargador":true,"factura":false},"numero_sac":"123456789","observaciones":"Medicion perf","sistema_operativo":"Windows 11 Pro"}	\N	uploads/pdf/Checklist_SERPERF002_Entrega equipo oficina_decb4d24.pdf	\N
125	98765432	\N	maria@example.co	Lenovo ThinkCentre M720	GENERADA	\N	2026-09-02 11:35:19.513166	\N	\N	\N	45	Maria Lopez	\N	INV0002	uploads/pdf/Devolucion_SERPERF002_Cambio de equipo_a6f6b57e.pdf	SERPERF002	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","recibido_por":"Perf Test Tecnico","entregado_por":"Maria Lopez","correo":"maria@example.co","cargo_recibe":"Tecnico TI","cedula":"98765432","area_recibe":"Mesa de ayuda","motivo":"Cambio de equipo","cargo_entrega":"Analista","nombre_jefe":"Jefe Area","cargo_jefe":"Director","equipos":[{"serial":"SERPERF002","marca":"Lenovo","tipo":"Desktop","modelo":"ThinkCentre M720","inventario":"INV0002","estado":"Entregado","gb":"16"}],"hardware":[{"tipo":"Cargador"}],"observaciones":"Medicion perf"}	\N	\N	\N
127	\N	\N	maria@example.co	Lenovo ThinkCentre M720	GENERADA	\N	2026-09-02 11:35:44.79171	\N	\N	\N	45	Maria Lopez	\N	INV0002	uploads/pdf/FormateoSeguro_SERPERF002_Formateo equipo_44bc6498.pdf	SERPERF002	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","entregado_a":"Maria Lopez","correo":"maria@example.co","cargo_recibe":"Analista","entregado_por":"Carlos Ruiz","cargo_entrega":"Tecnico TI","asunto":"Formateo equipo","equipos":[{"serial":"SERPERF002","marca":"Lenovo","tipo":"Desktop","modelo":"ThinkCentre M720","inventario":"INV0002","estado":"Operativo","gb":"16"}]}	\N	\N	\N
128	\N	\N	maria@example.co	Lenovo ThinkCentre M720	GENERADA	\N	2026-09-02 11:35:52.051983	\N	\N	\N	45	Maria Lopez	\N	INV0002	uploads/pdf/FormateoSeguro_SERPERF002_Formateo equipo_a0e578bb.pdf	SERPERF002	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","entregado_a":"Maria Lopez","correo":"maria@example.co","cargo_recibe":"Analista","entregado_por":"Carlos Ruiz","cargo_entrega":"Tecnico TI","asunto":"Formateo equipo","equipos":[{"serial":"SERPERF002","marca":"Lenovo","tipo":"Desktop","modelo":"ThinkCentre M720","inventario":"INV0002","estado":"Operativo","gb":"16"}]}	\N	\N	\N
19		\N	\N	Microsoft Corporation Virtual Machine Xeon	APROBADA	2026-07-31 07:40:10.29074	2026-07-30 15:11:11.970988	2026-07-30 15:11:29.717132	2026-07-30 15:11:56.729678	\N	1	gggg	\N	gggg	uploads/pdf/ActaEntrega_123_gggg.pdf	123	123455	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-30","entregado_a":"gggg","cargo_recibe":"gggg","entregado_por":"gggg","cargo_entrega":"gggg","asunto":"gggg","hardware":[{"tipo":"gggggggg","descripcion":"gggggggg","programa":"gggg"}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"gggg"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"123455","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
25	wwww	\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-31 09:14:12.369194	2026-07-31 09:07:57.891252	2026-07-31 09:08:17.962033	2026-07-31 09:10:11.658204	\N	1	wwww	\N	wwww	uploads/pdf/Devolucion_30JZTN3_wwww.pdf	30JZTN3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","recibido_por":"wwww","entregado_por":"wwww","cargo_recibe":"wwww","cargo_entrega":"wwww","cedula":"wwww","area_recibe":"wwww","motivo":"wwww","nombre_jefe":"wwww","cargo_jefe":"wwww","hardware":[{"tipo":"wwww"}],"equipos":[{"serial":"30JZTN3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"wwww","estado":"wwww"}],"observaciones":"wwww"}	\N	\N	\N
24		\N	\N	Dell Inc. Vostro 3400 Core i5	RECHAZADA	\N	2026-07-31 09:03:37.788176	2026-07-31 09:03:52.553302	\N	\N	1	pppp	No corresponde a mi equipo	pppp	uploads/pdf/ActaEntrega_30JZTN3_pppp.pdf	30JZTN3	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","entregado_a":"pppp","cargo_recibe":"pppp","entregado_por":"pppp","cargo_entrega":"pppp","asunto":"pppp","hardware":[{"tipo":"pppp","descripcion":"pppp","programa":"pppp"}],"equipos":[{"serial":"30JZTN3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"pppp"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12345","observaciones":"pppp","sistema_operativo":"Mac OS"}	2026-07-31 09:06:45.671764	\N	\N
23	rrrrr	\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-31 08:12:44.664723	2026-07-31 08:10:31.185885	2026-07-31 08:10:48.76069	2026-07-31 08:11:34.595889	\N	1	rrrrr	\N	rrrrr	uploads/pdf/Devolucion_DYYCQM3_rrrrrrrrrr.pdf	DYYCQM3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","recibido_por":"rrrrr","entregado_por":"rrrrr","cargo_recibe":"rrrrr","cargo_entrega":"rrrrr","cedula":"rrrrr","area_recibe":"rrrrr","motivo":"rrrrrrrrrr","nombre_jefe":"rrrrr","cargo_jefe":"rrrrr","hardware":[{"tipo":"rrrrr"}],"equipos":[{"serial":"DYYCQM3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"rrrrr","estado":"rrrrr"}],"observaciones":"rrrrr"}	\N	\N	\N
22	tttt	\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-31 08:12:45.508802	2026-07-31 07:55:26.387727	2026-07-31 07:55:35.092369	2026-07-31 07:56:00.331773	\N	1	tttt	\N	tttt	uploads/pdf/Devolucion_DYYCQM3_tttt.pdf	DYYCQM3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","recibido_por":"tttt","entregado_por":"tttt","cargo_recibe":"tttt","cargo_entrega":"tttt","cedula":"tttt","area_recibe":"tttt","motivo":"tttt","nombre_jefe":"tttt","cargo_jefe":"tttt","hardware":[{"tipo":"tttt"}],"equipos":[{"serial":"DYYCQM3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"tttt","estado":"tttt"}],"observaciones":"tttt"}	\N	\N	\N
77	123	\N	juanhernandez1122876@gmail.com	Dell Inc. Latitude 3440 Core i5	APROBADA	2026-08-25 15:43:32.012333	2026-08-25 10:22:32.588084	2026-08-25 10:22:44.232529	2026-08-25 10:23:30.288447	\N	1	Julian Alejandro Celis Valderrama	\N	aa	uploads/pdf/Devolucion_4K16ZW3_aa_acta77.pdf	4K16ZW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Julian Alejandro Celis Valderrama","cargo_recibe":"q","cedula":"123","area_recibe":"a","motivo":"aa","cargo_entrega":"a","nombre_jefe":"aa","cargo_jefe":"a","equipos":[{"serial":"4K16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"aa","estado":"aaa"}],"hardware":[{"tipo":"aa"}],"observaciones":"aa"}	\N	\N	\N
27	1035417680	\N	\N	Dell Inc. Latitude 3440 Core i5	APROBADA	2026-07-31 10:39:19.460833	2026-07-31 09:21:36.58737	2026-07-31 09:21:42.822667	2026-07-31 09:22:50.048228	\N	1	Juan Hernandez	\N	12345	uploads/pdf/Devolucion_CP16ZW3_Reparacion.pdf	CP16ZW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","recibido_por":"Angie","entregado_por":"Juan Hernandez","cargo_recibe":"Lider","cargo_entrega":"Aprendiz","cedula":"1035417680","area_recibe":"Infraestructura tecnologica","motivo":"Reparacion","nombre_jefe":"Je","cargo_jefe":"Vice","hardware":[{"tipo":"Mouse"}],"equipos":[{"serial":"CP16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"12345","estado":"Malo"}],"observaciones":"Se entrega por lentitud"}	\N	\N	\N
119	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	FIRMADA	\N	2026-09-01 12:31:24.408587	2026-09-01 12:34:49.304978	2026-09-01 12:36:57.097253	\N	1	David Alejandro Guzman Franco	\N	aa	uploads/pdf/ActaEntrega_123_Entrega Nuevo Equipo_eb06eacf_acta119.pdf	123	13154321	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-01","entregado_a":"David Alejandro Guzman Franco","cargo_recibe":"Coord","entregado_por":"Juan Jose Hernandez Correa","correo":"daviguzm@coltefinanciera.com.co","cargo_entrega":"Aprendiz","asunto":"Entrega Nuevo Equipo","hardware":[{"tipo":"aa","descripcion":"aa","programa":"a"}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aa","estado":"","gb":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":false,"chk_35":false,"chk_36":false},"numero_sac":"13154321","observaciones":"","sistema_operativo":"Windows 10"}	\N	uploads/pdf/Checklist_123_Entrega Nuevo Equipo_49f719ed_checklist119.pdf	\N
126	98765432	\N	maria@example.co	Lenovo ThinkCentre M720	GENERADA	\N	2026-09-02 11:35:28.697619	\N	\N	\N	45	Maria Lopez	\N	INV0002	uploads/pdf/Devolucion_SERPERF002_Cambio de equipo_ab4516d4.pdf	SERPERF002	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","recibido_por":"Perf Test Tecnico","entregado_por":"Maria Lopez","correo":"maria@example.co","cargo_recibe":"Tecnico TI","cedula":"98765432","area_recibe":"Mesa de ayuda","motivo":"Cambio de equipo","cargo_entrega":"Analista","nombre_jefe":"Jefe Area","cargo_jefe":"Director","equipos":[{"serial":"SERPERF002","marca":"Lenovo","tipo":"Desktop","modelo":"ThinkCentre M720","inventario":"INV0002","estado":"Entregado","gb":"16"}],"hardware":[{"tipo":"Cargador"}],"observaciones":"Medicion perf"}	\N	\N	\N
132	6586487802	\N	pedro.gomez@test.com	Dell Optiplex	GENERADA	\N	2026-09-02 17:01:28.203892	\N	\N	\N	47	Pedro Gomez	\N	INV-DEV-002	uploads/pdf/Devolucion_SN-DEV-002_Devolucion por cambio_d4523d28.pdf	SN-DEV-002	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","recibido_por":"Ana Recepcion","entregado_por":"Pedro Gomez","correo":"pedro.gomez@test.com","cargo_recibe":"Analista","cedula":"6586487802","area_recibe":"TI","motivo":"Devolucion por cambio","cargo_entrega":"Tecnico","nombre_jefe":"Jorge Jefe","cargo_jefe":"Coordinador","equipos":[{"serial":"SN-DEV-002","marca":"Dell","tipo":"Desktop","modelo":"Optiplex","inventario":"INV-DEV-002","estado":"Bueno","gb":""}],"hardware":[{"tipo":"Teclado"}],"observaciones":"test devolucion async"}	\N	\N	Devolucion_SN-DEV-002_Devolucion por cambio_65271802.zip
33	nnnn	\N	\N	Dell Inc. OptiPlex 3070 Core i5	RECHAZADA	\N	2026-07-31 10:42:38.439791	2026-07-31 10:42:56.694425	\N	\N	1	nnnn	No estoy de acuerdo con el contenido	nnnn	uploads/pdf/Devolucion_CCV3F33_nnnn.pdf	CCV3F33	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","recibido_por":"nnnn","entregado_por":"nnnn","cargo_recibe":"nnnn","cargo_entrega":"nnnn","cedula":"nnnn","area_recibe":"nnnn","motivo":"nnnn","nombre_jefe":"nnnn","cargo_jefe":"nnnn","hardware":[{"tipo":"nnnn"}],"equipos":[{"serial":"CCV3F33","marca":"Dell Inc.","tipo":"Desktop","modelo":"OptiPlex 3070 Core i5","inventario":"nnnn","estado":"nnnn"}],"observaciones":"nnnn"}	2026-07-31 10:47:28.330384	\N	\N
32		\N	\N	Dell Inc. OptiPlex 3070 Core i5	APROBADA	2026-07-31 10:47:44.745223	2026-07-31 10:40:24.93722	2026-07-31 10:42:54.738875	2026-07-31 10:44:20.155974	\N	1	qqqq	\N	qqqq	uploads/pdf/ActaEntrega_CCV3F33_qqqq.pdf	CCV3F33	1234	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","entregado_a":"qqqq","cargo_recibe":"qqqq","entregado_por":"qqqq","cargo_entrega":"qqqq","asunto":"qqqq","hardware":[{"tipo":"qqqq","descripcion":"qqqq","programa":"qqqq"}],"equipos":[{"serial":"CCV3F33","marca":"Dell Inc.","tipo":"Desktop","modelo":"OptiPlex 3070 Core i5","inventario":"qqqq"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"1234","observaciones":"qqqqqqqqqqqqqqqq","sistema_operativo":"Windows 10"}	\N	\N	\N
36	kkkk	\N	\N	Dell Inc. Vostro 3400 Core i5	APROBADA	2026-07-31 11:21:00.951055	2026-07-31 11:19:12.029932	2026-07-31 11:19:29.864029	2026-07-31 11:19:53.764055	\N	1	kkkk	\N	1234	uploads/pdf/Devolucion_30JZTN3_kkkk.pdf	30JZTN3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","recibido_por":"kkkk","entregado_por":"kkkk","cargo_recibe":"kkkk","cargo_entrega":"kkkk","cedula":"kkkk","area_recibe":"kkkk","motivo":"kkkk","nombre_jefe":"kkkk","cargo_jefe":"kkkk","hardware":[{"tipo":"kkkk"}],"equipos":[{"serial":"30JZTN3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"1234","estado":"Bueno"}],"observaciones":"kkkk"}	\N	\N	\N
26		\N	\N	Dell Inc. Latitude 3440 Core i5	APROBADA	2026-07-31 10:39:20.250977	2026-07-31 09:15:29.617326	2026-07-31 09:16:17.153361	2026-07-31 09:18:31.68466	\N	1	Juan Hernandez	\N	14399	uploads/pdf/ActaEntrega_FM16ZW3_NuevoUsuario.pdf	FM16ZW3	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","entregado_a":"Juan Hernandez","cargo_recibe":"Aprendiz","entregado_por":"Angie","cargo_entrega":"Lider","asunto":"Nuevo Usuario","hardware":[{"tipo":"Mouse","descripcion":"12345","programa":"Logitech"}],"equipos":[{"serial":"FM16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"14399"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":false,"chk_34":false,"chk_35":false,"chk_36":false},"numero_sac":"12345","observaciones":"Se le entrega equipo en excelente estado","sistema_operativo":"Windows 10"}	\N	\N	\N
78	131241	\N	juanhernandez1122876@gmail.com	Dell Inc. Vostro 3400 Core i5	RECHAZADA	\N	2026-08-25 11:17:01.596666	2026-08-25 11:18:01.370237	2026-08-25 11:18:47.156206	\N	1	Angie Maritza Diaz Montaño	Firma mal puesta	1234	uploads/pdf/Devolucion_1PG15N3_aaaaaaaaaaaaaaaa_acta78.pdf	1PG15N3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Angie Maritza Diaz Montaño","cargo_recibe":"aaaaaaaaaaaaaaaa","cedula":"131241","area_recibe":"aaaaaaaaaaaaaaaa","motivo":"aaaaaaaaaaaaaaaa","cargo_entrega":"aaaaaaaaaaa","nombre_jefe":"aaaaaaaaaaaaaaaa","cargo_jefe":"aaaaaaaaaaaaaaaa","equipos":[{"serial":"1PG15N3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"1234","estado":"Buenoo"}],"hardware":[{"tipo":"aaaa"}],"observaciones":"aaaaaaaaaaaaaaaa"}	2026-08-25 15:43:30.651054	\N	\N
40		\N	JuanHernandez@coltefinanciera.com.co	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-04 08:35:50.750579	2026-08-04 08:36:46.827521	\N	\N	1	Andres Mauricio Muñoz Tascon	\N	aaaaa	uploads/pdf/ActaEntrega_123_aaaaa.pdf	123	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-04","entregado_a":"Andres Mauricio Muñoz Tascon","cargo_recibe":"aaaaa","entregado_por":"juan","cargo_entrega":"aaaaa","asunto":"aaaaa","hardware":[{"tipo":"aaaaa","descripcion":"aaaaa","programa":"aaaaa"}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aaaaa"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12345","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
38		\N	\N	Microsoft Corporation Virtual Machine Xeon	APROBADA	2026-07-31 16:21:29.461931	2026-07-31 16:18:27.34385	2026-07-31 16:18:39.577662	2026-07-31 16:19:03.263276	\N	1	ssss	\N	ssss	uploads/pdf/ActaEntrega_123_ssss.pdf	123	1235	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","entregado_a":"ssss","cargo_recibe":"ssss","entregado_por":"ssss","cargo_entrega":"ssss","asunto":"ssss","hardware":[{"tipo":"ssss","descripcion":"ssss","programa":"ssss"}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"ssss"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"1235","observaciones":"ssssssss","sistema_operativo":"Mac OS"}	\N	\N	\N
37		\N	\N	Microsoft Corporation Virtual Machine Xeon	RECHAZADA	\N	2026-07-31 15:15:15.860414	2026-07-31 15:15:34.76737	2026-07-31 15:16:27.087081	\N	1	ffff	No firmó bien	ffff	uploads/pdf/ActaEntrega_123_ffff.pdf	123	1234	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","entregado_a":"ffff","cargo_recibe":"ffff","entregado_por":"ffff","cargo_entrega":"ffff","asunto":"ffff","hardware":[{"tipo":"ffff","descripcion":"ffff","programa":"ffff"}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"ffff"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"1234","observaciones":"ffffffffffffffff","sistema_operativo":"Mac OS"}	2026-07-31 16:21:48.718165	\N	\N
39		\N	\N	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-07-31 16:23:52.397161	2026-08-03 14:57:55.828226	\N	\N	1	qqqq	\N	qqqq	uploads/pdf/ActaEntrega_133_qqqq.pdf	133	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-07-31","entregado_a":"qqqq","cargo_recibe":"qqqq","entregado_por":"qqqq","cargo_entrega":"qqqq","asunto":"qqqq","hardware":[{"tipo":"qqqq","descripcion":"qqqq","programa":"qqqq"}],"equipos":[{"serial":"133","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"qqqq"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12345","observaciones":"qqqqqqqqqqqq","sistema_operativo":"Windows 10"}	\N	\N	\N
116	\N	\N	JuliCeli@coltefinanciera.com.co	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-31 13:33:38.807384	\N	\N	\N	1	Julian Alejandro Celis Valderrama	\N	aa	uploads/pdf/ActaEntrega_123_aa_74001722.pdf	123	12312	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-31","entregado_a":"Julian Alejandro Celis Valderrama","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","correo":"JuliCeli@coltefinanciera.com.co","cargo_entrega":"a","asunto":"aa","hardware":[{"tipo":"aa","descripcion":"aa","programa":"aa"}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aa","estado":"","gb":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12312","observaciones":"aaaa","sistema_operativo":"Windows 11"}	\N	\N	\N
103	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-31 08:02:48.792328	2026-08-31 08:03:20.64917	\N	\N	1	Jonathan Camilo Herrera Gallego	\N	a	uploads/pdf/FormateoSeguro_123_a.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-31","entregado_a":"Jonathan Camilo Herrera Gallego","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"a","asunto":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"a","estado":"","gb":"1234"}]}	\N	\N	\N
117	123123	\N	JuliCeli@coltefinanciera.com.co	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-31 13:34:23.650893	\N	\N	\N	1	Julian Alejandro Celis Valderrama	\N	aa	uploads/pdf/Devolucion_123_a_4e761177.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-31","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Julian Alejandro Celis Valderrama","correo":"JuliCeli@coltefinanciera.com.co","cargo_recibe":"a","cedula":"123123","area_recibe":"aa","motivo":"a","cargo_entrega":"aaa","nombre_jefe":"a","cargo_jefe":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aa","estado":"aa","gb":""}],"hardware":[{"tipo":"aaa"}],"observaciones":"a"}	\N	\N	\N
120	12345678	<p>Entrega de equipo</p>	cperez@test.local	Laptop prueba SEC-010	GENERADA	\N	2026-09-02 10:10:37.435012	\N	\N	\N	41	Carlos Perez	\N	PL-SEC011	\N	SN-SEC011	\N	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"ticketGlpi":null,"fecha":"2026-09-02","tipoActa":"ENTREGA","cedulaUsuario":"12345678","nombreUsuario":"Carlos Perez","correoUsuario":"cperez@test.local","serialEquipo":"SN-SEC011","placaEquipo":"PL-SEC011","descripcionEquipo":"Laptop prueba SEC-010","contenidoHtml":"<p>Entrega de equipo</p>","rutaPdf":null,"datosOriginales":null}	\N	\N	\N
47	1	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-21 09:39:09.862612	\N	\N	\N	1	Jhonatan David Rojo Ramos	\N	123	uploads/pdf/Devolucion_123_1.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-21","recibido_por":"1","entregado_por":"Jhonatan David Rojo Ramos","cargo_recibe":"1","cedula":"1","area_recibe":"1","motivo":"1","cargo_entrega":"1","nombre_jefe":"1","cargo_jefe":"1","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"1"}],"hardware":[{"tipo":"1"}],"observaciones":"1"}	\N	\N	\N
41		\N	daviguzm@coltefinanciera.com.co	Microsoft Corporation Virtual Machine Xeon	APROBADA	2026-08-04 14:22:47.454712	2026-08-04 14:14:12.273871	2026-08-04 14:16:47.470501	2026-08-04 14:17:56.004246	\N	1	David Alejandro Guzman Franco	\N	12343	uploads/pdf/ActaEntrega_123_NuevoUsuario.pdf	123	12354	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-04","entregado_a":"David Alejandro Guzman Franco","cargo_recibe":"Coordinador","entregado_por":"Daniel Naranjo","cargo_entrega":"Soporte IT","asunto":"Nuevo Usuario","hardware":[{"tipo":"Mouse","descripcion":"","programa":""}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"12343"}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12354","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
118	\N	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-31 13:36:46.582004	\N	\N	\N	1	Juan Jose Hernandez Correa	\N	123	uploads/pdf/FormateoSeguro_123_aaa_48c28f72.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-31","entregado_a":"Juan Jose Hernandez Correa","correo":"","cargo_recibe":"a","entregado_por":"Julian Alejandro Celis Valderrama","cargo_entrega":"a","asunto":"aaa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"","gb":"aaa"}]}	\N	\N	\N
121	12345678	<p>Entrega equipo</p>	cperez@test.local	Laptop prueba SEC-010	GENERADA	\N	2026-09-02 10:21:53.606909	\N	\N	\N	41	Carlos Perez	\N	PL2001	\N	SN20260001	\N	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"ticketGlpi":null,"fecha":"2026-09-02","tipoActa":"ENTREGA","cedulaUsuario":"12345678","nombreUsuario":"Carlos Perez","correoUsuario":"cperez@test.local","serialEquipo":"SN20260001","placaEquipo":"PL2001","descripcionEquipo":"Laptop prueba SEC-010","contenidoHtml":"<p>Entrega equipo</p>","rutaPdf":null,"datosOriginales":null}	\N	\N	\N
129	\N	\N	maria@example.co	Lenovo ThinkCentre M720	GENERADA	\N	2026-09-02 11:36:44.478151	\N	\N	\N	45	Maria Lopez	\N	INV0002	uploads/pdf/ActaEntrega_SERPERF002_Entrega equipo oficina_9a9902a9.pdf	SERPERF002	123456789	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","entregado_a":"Maria Lopez","cargo_recibe":"Analista","entregado_por":"Carlos Ruiz","correo":"maria@example.co","cargo_entrega":"Tecnico TI","asunto":"Entrega equipo oficina","hardware":[{"tipo":"Teclado","descripcion":"USB","programa":""}],"equipos":[{"serial":"SERPERF002","marca":"Lenovo","tipo":"Desktop","modelo":"ThinkCentre M720","inventario":"INV0002","estado":"Operativo","gb":"16"}],"checklist":{"manual_usuario":true,"cargador":true,"factura":false},"numero_sac":"123456789","observaciones":"Medicion perf","sistema_operativo":"Windows 11 Pro"}	\N	uploads/pdf/Checklist_SERPERF002_Entrega equipo oficina_65cba5ac.pdf	\N
130	\N	\N	maria@example.co	Lenovo ThinkCentre M720	GENERADA	\N	2026-09-02 11:37:15.377102	\N	\N	\N	45	Maria Lopez	\N	INV0002	uploads/pdf/ActaEntrega_SERPERF002_Entrega equipo oficina_996619e7.pdf	SERPERF002	123456789	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","entregado_a":"Maria Lopez","cargo_recibe":"Analista","entregado_por":"Carlos Ruiz","correo":"maria@example.co","cargo_entrega":"Tecnico TI","asunto":"Entrega equipo oficina","hardware":[{"tipo":"Teclado","descripcion":"USB","programa":""}],"equipos":[{"serial":"SERPERF002","marca":"Lenovo","tipo":"Desktop","modelo":"ThinkCentre M720","inventario":"INV0002","estado":"Operativo","gb":"16"}],"checklist":{"manual_usuario":true,"cargador":true,"factura":false},"numero_sac":"123456789","observaciones":"Medicion perf","sistema_operativo":"Windows 11 Pro"}	\N	uploads/pdf/Checklist_SERPERF002_Entrega equipo oficina_3fff4ef9.pdf	\N
131	\N	\N	juan.perez@test.com	HP Probook 450	GENERADA	\N	2026-09-02 16:59:14.391893	\N	\N	\N	47	Juan Perez	\N	INV-ASYNC-001	uploads/pdf/ActaEntrega_SN-ASYNC-001_PRUEBA FASE 1 ASYNC_99dbb94a.pdf	SN-ASYNC-001	123456	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-02","entregado_a":"Juan Perez","cargo_recibe":"Analista","entregado_por":"Pedro Gomez","correo":"juan.perez@test.com","cargo_entrega":"Tecnico","asunto":"PRUEBA FASE 1 ASYNC","hardware":[{"tipo":"Monitor","descripcion":"LG 24","programa":""}],"equipos":[{"serial":"SN-ASYNC-001","marca":"HP","tipo":"Portatil","modelo":"Probook 450","inventario":"INV-ASYNC-001","estado":"","gb":""}],"checklist":{"chk_1":true},"numero_sac":"123456","observaciones":"test async fase 1","sistema_operativo":"WINDOWS_11"}	\N	uploads/pdf/Checklist_SN-ASYNC-001_PRUEBA FASE 1 ASYNC_ffab2af1.pdf	ActaLista_SN-ASYNC-001_PRUEBA FASE 1 ASYNC_496010f2.zip
48	9999	\N	razortxz@gmail.com	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-21 09:41:00.48594	2026-08-21 09:54:54.712982	\N	\N	1	Jhonatan David Rojo Ramos	\N	e	uploads/pdf/Devolucion_123_eeee.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-22","recibido_por":"Julian Alejandro Celis Valderrama","entregado_por":"Jhonatan David Rojo Ramos","cargo_recibe":"a","cedula":"9999","area_recibe":"a","motivo":"eeee","cargo_entrega":"a","nombre_jefe":"e","cargo_jefe":"e","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"e","estado":"e"}],"hardware":[{"tipo":"e"}],"observaciones":"e"}	\N	\N	\N
105	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-31 08:46:43.402034	2026-08-31 08:48:14.811262	\N	\N	1	Julian Alejandro Celis Valderrama	\N	123	uploads/pdf/FormateoSeguro_123_a.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-31","entregado_a":"Julian Alejandro Celis Valderrama","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"a","asunto":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"","gb":"1234"}]}	\N	\N	\N
49	\N	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-21 09:52:25.731526	\N	\N	\N	1	Daniel Fernando Ortiz Pataquiva	\N	yyy	uploads/pdf/ActaEntrega_123_yyyy.pdf	123	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-21","entregado_a":"Daniel Fernando Ortiz Pataquiva","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"a","asunto":"yyyy","hardware":[{"tipo":"yy","descripcion":"y","programa":"yy"}],"equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"yyy","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12345","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
122	12345678	<p>Entrega equipo</p>	cperez@test.local	Laptop prueba SEC-010	GENERADA	\N	2026-09-02 10:25:55.144834	\N	\N	\N	41	Carlos Perez	\N	PL2001	\N	SN20260001	\N	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"ticketGlpi":null,"fecha":"2026-09-02","tipoActa":"ENTREGA","cedulaUsuario":"12345678","nombreUsuario":"Carlos Perez","correoUsuario":"cperez@test.local","serialEquipo":"SN20260001","placaEquipo":"PL2001","descripcionEquipo":"Laptop prueba SEC-010","contenidoHtml":"<p>Entrega equipo</p>","rutaPdf":null,"datosOriginales":null}	\N	\N	\N
50	123	\N	razortxz@gmail.com	Microsoft Corporation Virtual Machine Xeon	APROBADA	2026-08-21 10:19:34.611138	2026-08-21 10:15:47.690312	2026-08-21 10:17:00.401894	2026-08-21 10:17:50.454441	\N	1	Jhonatan David Rojo Ramos	\N	123	uploads/pdf/Devolucion_123_t.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-21","recibido_por":"Julian Alejandro Celis Valderrama","entregado_por":"Jhonatan David Rojo Ramos","cargo_recibe":"t","cedula":"123","area_recibe":"t","motivo":"t","cargo_entrega":"t","nombre_jefe":"t","cargo_jefe":"t","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"123"}],"hardware":[{"tipo":"aaaa"}],"observaciones":"t"}	\N	\N	\N
75	123	\N	juanhernandez1122876@gmail.com	HP HP Laptop 14-fq1xxx Ryzen 5	FIRMADA	\N	2026-08-25 09:59:08.315928	2026-08-25 09:59:52.812243	2026-08-25 10:01:52.910075	\N	1	David Alejandro Guzman Franco	\N	a	uploads/pdf/Devolucion_5CD2256W6H_aaaa_acta75.pdf	5CD2256W6H	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"David Alejandro Guzman Franco","cargo_recibe":"aa","cedula":"123","area_recibe":"aa","motivo":"aaaa","cargo_entrega":"a","nombre_jefe":"a","cargo_jefe":"aa","equipos":[{"serial":"5CD2256W6H","marca":"HP","tipo":"Notebook","modelo":"HP Laptop 14-fq1xxx Ryzen 5","inventario":"a","estado":"a"}],"hardware":[{"tipo":"a"}],"observaciones":"a"}	\N	\N	\N
51	123	\N	razortxz@gmail.com	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-21 10:30:26.05986	2026-08-21 10:55:20.941948	\N	\N	8	Jose Julian Ruiz Bermudez	\N	111	uploads/pdf/Devolucion_11_oo.pdf	11	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-21","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Jose Julian Ruiz Bermudez","cargo_recibe":"oo","cedula":"123","area_recibe":"o","motivo":"oo","cargo_entrega":"oo","nombre_jefe":"o","cargo_jefe":"o","equipos":[{"serial":"11","marca":"Microsoft Corporation","tipo":"WSL","modelo":"Virtual Machine Xeon","inventario":"111","estado":"111"}],"hardware":[{"tipo":"11"}],"observaciones":"o"}	\N	\N	\N
74	a	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	FIRMADA	\N	2026-08-25 08:29:13.78113	2026-08-25 08:30:23.942796	2026-08-25 09:42:03.077858	\N	1	Juan Jose Hernandez Correa	\N	123	uploads/pdf/Devolucion_123_a_acta74.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","recibido_por":"Jhonatan David Rojo Ramos","entregado_por":"Juan Jose Hernandez Correa","cargo_recibe":"a","cedula":"a","area_recibe":"a","motivo":"a","cargo_entrega":"a","nombre_jefe":"a","cargo_jefe":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"a"}],"hardware":[{"tipo":"a"}],"observaciones":"a"}	\N	\N	\N
57	|	\N	razortxz@gmail.com	Microsoft Corporation Virtual Machine Xeon	FIRMADA	\N	2026-08-21 12:07:20.626443	2026-08-21 12:07:45.306075	2026-08-25 10:08:44.980456	\N	8	Juan Bautista Mahecha Villamil	\N	123	uploads/pdf/Devolucion_123_a_acta57.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-21","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Juan Bautista Mahecha Villamil","cargo_recibe":"a","cedula":"|","area_recibe":"a","motivo":"a","cargo_entrega":"|","nombre_jefe":"a","cargo_jefe":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"123"}],"hardware":[{"tipo":"1"}],"observaciones":"a"}	\N	\N	\N
70	a	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	FIRMADA	\N	2026-08-24 16:31:08.412374	2026-08-24 16:32:04.493767	2026-08-25 08:48:24.060506	\N	1	Juan Jose Hernandez Correa	\N	aa	uploads/pdf/Devolucion_123_a.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-24","recibido_por":"David Alejandro Guzman Franco","entregado_por":"Juan Jose Hernandez Correa","cargo_recibe":"a","cedula":"a","area_recibe":"a","motivo":"a","cargo_entrega":"a","nombre_jefe":"a","cargo_jefe":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aa","estado":"aa"}],"hardware":[{"tipo":"aa"}],"observaciones":"a"}	\N	\N	\N
69	1	\N	JuanHernandez@coltefinanciera.com.co	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-24 16:24:52.907177	2026-08-24 16:29:08.914333	\N	\N	1	Juan Jose Hernandez Correa	\N	123	uploads/pdf/Devolucion_123_a.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-24","recibido_por":"David Alejandro Guzman Franco","entregado_por":"Juan Jose Hernandez Correa","cargo_recibe":"a","cedula":"1","area_recibe":"a","motivo":"a","cargo_entrega":"a","nombre_jefe":"a","cargo_jefe":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"11"}],"hardware":[{"tipo":"111"}],"observaciones":"a"}	\N	\N	\N
76	123	\N	juanhernandez1122876@gmail.com	Dell Inc. Latitude 3440 Core i5	FIRMADA	\N	2026-08-25 10:16:14.535508	2026-08-25 10:17:07.91298	2026-08-25 10:17:53.487281	\N	1	Julian Alejandro Celis Valderrama	\N	123	uploads/pdf/Devolucion_4K16ZW3_a_acta76.pdf	4K16ZW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Julian Alejandro Celis Valderrama","cargo_recibe":"a","cedula":"123","area_recibe":"a","motivo":"a","cargo_entrega":"a","nombre_jefe":"a","cargo_jefe":"a","equipos":[{"serial":"4K16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"123","estado":"aaaa"}],"hardware":[{"tipo":"aaa"}],"observaciones":"a"}	\N	\N	\N
68	9876543	\N	razortxz@gmail.com	Prueba de correo real Brevo	FIRMADA	\N	2026-08-24 16:00:08.405855	2026-08-24 16:00:09.515644	2026-08-25 11:09:02.763007	\N	20	Prueba SMTP Real	\N	PL-TEST1	\N	SN-BRVO-TEST1	\N	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
81	aaaaaaa	\N	\N	Dell Inc. Inspiron 15 3520 Core i5	GENERADA	\N	2026-08-25 15:16:50.424745	\N	\N	\N	1	Juan Carlos Acuña Jaraba	\N	123	uploads/pdf/Devolucion_DBV6RW3_aa.pdf	DBV6RW3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Juan Carlos Acuña Jaraba","cargo_recibe":"aa","cedula":"aaaaaaa","area_recibe":"a","motivo":"aa","cargo_entrega":"aaa","nombre_jefe":"aa","cargo_jefe":"aaaa","equipos":[{"serial":"DBV6RW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Inspiron 15 3520 Core i5","inventario":"123","estado":"12313"}],"hardware":[{"tipo":"1"}],"observaciones":"aaa"}	\N	\N	\N
82	\N	\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-08-25 16:01:13.112553	\N	\N	\N	1	Adriana Maria Correa Jaramillo	\N	1234	uploads/pdf/ActaEntrega_6Q16ZW3_aaaa.pdf	6Q16ZW3	123141	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","entregado_a":"Adriana Maria Correa Jaramillo","cargo_recibe":"aaaa","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"aaaa","asunto":"aaaa","hardware":[{"tipo":"a","descripcion":"aa","programa":"a"}],"equipos":[{"serial":"6Q16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"1234","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"123141","observaciones":"aaa","sistema_operativo":"Windows 10"}	\N	\N	\N
83	\N	\N	\N	Dell Inc. Latitude 3400 Core i5	GENERADA	\N	2026-08-25 16:12:11.802325	\N	\N	\N	1	Angelica Marcela Henao Ramirez	\N	123	uploads/pdf/ActaEntrega_GTGWVZ2_aaa.pdf	GTGWVZ2	12345	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","entregado_a":"Angelica Marcela Henao Ramirez","cargo_recibe":"aa","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"aaa","asunto":"aaa","hardware":[{"tipo":"aa","descripcion":"aa","programa":"aa"}],"equipos":[{"serial":"GTGWVZ2","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3400 Core i5","inventario":"123","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"12345","observaciones":"","sistema_operativo":"Windows 11"}	\N	\N	\N
84	\N	\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-08-25 16:20:30.42585	\N	\N	\N	1	Adriana Mahecha Tovar	\N	123	uploads/pdf/ActaEntrega_HK16ZW3_T.pdf	HK16ZW3	123141412	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","entregado_a":"Adriana Mahecha Tovar","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"a","asunto":"T","hardware":[{"tipo":"aa","descripcion":"a","programa":"aa"}],"equipos":[{"serial":"HK16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"123","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"123141412","observaciones":"aaa","sistema_operativo":"Windows 11"}	\N	\N	\N
85	\N	\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-08-25 16:23:01.599437	\N	\N	\N	1	Julian Alejandro Celis Valderrama	\N	aaa	uploads/pdf/ActaEntrega_HK16ZW3_a.pdf	HK16ZW3	1235413	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","entregado_a":"Julian Alejandro Celis Valderrama","cargo_recibe":"aa","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"aaa","asunto":"a","hardware":[{"tipo":"aa","descripcion":"a","programa":"a"}],"equipos":[{"serial":"HK16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"aaa","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"1235413","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
86	\N	\N	\N	Dell Inc. Latitude 3440 Core i5	GENERADA	\N	2026-08-25 16:27:11.839632	\N	\N	\N	1	Dany Juliana Rivera Rivera	\N	123	uploads/pdf/ActaEntrega_HK16ZW3_aaa.pdf	HK16ZW3	123145135	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","entregado_a":"Dany Juliana Rivera Rivera","cargo_recibe":"aaaa","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"aa","asunto":"aaa","hardware":[{"tipo":"aaa","descripcion":"aa","programa":"aaa"}],"equipos":[{"serial":"HK16ZW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Latitude 3440 Core i5","inventario":"123","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"123145135","observaciones":"","sistema_operativo":"Mac OS"}	\N	\N	\N
87	\N	\N	\N	Dell Inc. Inspiron 15 3520 Core i5	GENERADA	\N	2026-08-25 16:35:13.517419	\N	\N	\N	1	Cristian Javier Blanco Romero	\N	123	uploads/pdf/ActaEntrega_DBV6RW3_sdfghj.pdf	DBV6RW3	2311421	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","entregado_a":"Cristian Javier Blanco Romero","cargo_recibe":"aaa","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"a","asunto":"sdfghj","hardware":[{"tipo":"a","descripcion":"a","programa":"a"}],"equipos":[{"serial":"DBV6RW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Inspiron 15 3520 Core i5","inventario":"123","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"2311421","observaciones":"","sistema_operativo":"Windows 11"}	\N	\N	\N
106	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-31 09:11:24.583413	2026-08-31 09:11:46.325967	\N	\N	1	David Alejandro Guzman Franco	\N	1321	uploads/pdf/FormateoSeguro_123_aa.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-31","entregado_a":"David Alejandro Guzman Franco","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"a","asunto":"aa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"1321","estado":"","gb":"123"}]}	\N	\N	\N
88	\N	\N	juanhernandez1122876@gmail.com	Dell Inc. Inspiron 15 3520 Core i5	ENVIADA	\N	2026-08-25 16:39:43.470006	2026-08-25 16:46:55.921328	\N	\N	1	Paula Alejandra Ricardo Cagua	\N	1231	uploads/pdf/ActaEntrega_DBV6RW3_aa.pdf	DBV6RW3	1321451	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-25","entregado_a":"Paula Alejandra Ricardo Cagua","cargo_recibe":"aa","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"aa","asunto":"aa","hardware":[{"tipo":"aa","descripcion":"daas","programa":"aaa"}],"equipos":[{"serial":"DBV6RW3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Inspiron 15 3520 Core i5","inventario":"1231","estado":""}],"checklist":{"chk_1":true,"chk_2":true,"chk_3":true,"chk_4":true,"chk_5":true,"chk_6":true,"chk_7":true,"chk_8":true,"chk_9":true,"chk_10":true,"chk_11":true,"chk_12":true,"chk_13":true,"chk_14":true,"chk_15":true,"chk_16":true,"chk_17":true,"chk_18":true,"chk_19":true,"chk_20":true,"chk_21":true,"chk_22":true,"chk_23":true,"chk_24":true,"chk_25":true,"chk_26":true,"chk_27":true,"chk_28":true,"chk_29":true,"chk_30":true,"chk_31":true,"chk_32":true,"chk_33":true,"chk_34":true,"chk_35":true,"chk_36":true},"numero_sac":"1321451","observaciones":"","sistema_operativo":"Windows 10"}	\N	\N	\N
89	aaa	\N	\N	Dell Inc. Vostro 3400 Core i5	GENERADA	\N	2026-08-26 09:20:36.715694	\N	\N	\N	1	David Alejandro Guzman Franco	\N	12314	uploads/pdf/Devolucion_CL4DQM3_aaa.pdf	CL4DQM3	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-26","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"David Alejandro Guzman Franco","cargo_recibe":"aa","cedula":"aaa","area_recibe":"a","motivo":"aaa","cargo_entrega":"aaa","nombre_jefe":"aa","cargo_jefe":"aa","equipos":[{"serial":"CL4DQM3","marca":"Dell Inc.","tipo":"Notebook","modelo":"Vostro 3400 Core i5","inventario":"12314","estado":"aa"}],"hardware":[{"tipo":"aaaa"}],"observaciones":"aa"}	\N	\N	\N
90	aaa	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-26 11:08:27.620496	\N	\N	\N	1	Jhonatan David Rojo Ramos	\N	a	uploads/pdf/Devolucion_123_a.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-26","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Jhonatan David Rojo Ramos","cargo_recibe":"aa","cedula":"aaa","area_recibe":"aa","motivo":"a","cargo_entrega":"aaa","nombre_jefe":"aa","cargo_jefe":"aa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"a","estado":"aaa"}],"hardware":[{"tipo":"aa"}],"observaciones":"aa"}	\N	\N	\N
91	a	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-26 11:27:40.083277	\N	\N	\N	1	Juan Jose Hernandez Correa	\N	aa	uploads/pdf/Devolucion_123_a.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-26","recibido_por":"Juan Jose Hernandez Correa","entregado_por":"Juan Jose Hernandez Correa","correo":"","cargo_recibe":"a","cedula":"a","area_recibe":"aa","motivo":"a","cargo_entrega":"aa","nombre_jefe":"aa","cargo_jefe":"aa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aa","estado":"a"}],"hardware":[{"tipo":"aa"}],"observaciones":"a"}	\N	\N	\N
92	a	\N	JhonatanRojo@Coltefinanciera.com.co	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-26 11:28:13.179407	\N	\N	\N	1	Jhonatan David Rojo Ramos	\N	aa	uploads/pdf/Devolucion_123_aa.pdf	123	\N	DEVOLUCION	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-26","recibido_por":"Jhonatan David Rojo Ramos","entregado_por":"Jhonatan David Rojo Ramos","correo":"JhonatanRojo@Coltefinanciera.com.co","cargo_recibe":"aa","cedula":"a","area_recibe":"aaa","motivo":"aa","cargo_entrega":"aa","nombre_jefe":"a","cargo_jefe":"aaa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aa","estado":"aaa"}],"hardware":[{"tipo":"aa"}],"observaciones":"aa"}	\N	\N	\N
96	\N	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-27 11:25:58.410686	\N	\N	\N	1	Julian Alejandro Celis Valderrama	\N	1231	uploads/pdf/FormateoSeguro_123_aaa.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-27","entregado_a":"Juan Jose Hernandez Correa","cargo_recibe":"a","entregado_por":"Julian Alejandro Celis Valderrama","cargo_entrega":"aa","asunto":"aaa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"1231","estado":"","gb":"124"}]}	\N	\N	\N
99	\N	\N	destino@test.com	\N	ENVIADA	\N	2026-08-27 14:49:23.229235	2026-08-27 14:50:04.83862	\N	\N	31		\N	\N	uploads/pdf/FormateoSeguro_SinSerial_.pdf	\N	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"27/08/2026","entregado_a":"","cargo_recibe":"","entregado_por":"","cargo_entrega":"","asunto":"","equipos":[]}	\N	\N	\N
97	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	FIRMADA	\N	2026-08-27 11:49:22.787639	2026-08-27 11:50:11.767111	2026-08-27 11:51:24.638975	\N	1	David Alejandro Guzman Franco	\N	aa	uploads/pdf/FormateoSeguro_123_aaa.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-27","entregado_a":"Julian Alejandro Celis Valderrama","cargo_recibe":"aa","entregado_por":"David Alejandro Guzman Franco","cargo_entrega":"aa","asunto":"aaa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"aa","estado":"","gb":"145"}]}	\N	\N	\N
98	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	APROBADA	2026-08-27 14:51:03.98205	2026-08-27 12:08:35.440606	2026-08-27 12:09:08.600816	2026-08-27 12:10:03.471936	\N	1	Juan Jose Hernandez Correa	\N	1231	uploads/pdf/FormateoSeguro_123_aaaaa_acta98.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-27","entregado_a":"Maria Alejandra Cañas Molina","cargo_recibe":"aaaa","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"aaa","asunto":"aaaaa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"1231","estado":"","gb":"123"}]}	\N	\N	\N
100	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-27 16:51:39.8269	2026-08-27 16:53:19.258441	\N	\N	1	Juan Jose Hernandez Correa	\N	123	uploads/pdf/FormateoSeguro_123_a.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-27","entregado_a":"Daniel Alejandro Castrillon Roldan","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"a","asunto":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"123","estado":"","gb":"123"}]}	\N	\N	\N
101	\N	\N	juan.perez@coltefinanciera.com	HP EliteBook	ENVIADA	\N	2026-08-27 16:57:53.553655	2026-08-27 16:58:53.586665	\N	\N	31	Juan Perez	\N	INV-FS-001	uploads/pdf/FormateoSeguro_SN-TEST-FS-2026_PruebaConcepto.pdf	SN-TEST-FS-2026	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-27","entregado_a":"Juan Perez","cargo_recibe":"Analista","entregado_por":"Juan Hernandez","cargo_entrega":"Tecnico","asunto":"PruebaConcepto","equipos":[{"serial":"SN-TEST-FS-2026","marca":"HP","tipo":"","modelo":"EliteBook","inventario":"INV-FS-001","estado":"","gb":""}]}	\N	\N	\N
102	\N	\N	juanhernandez1122876@gmail.com	Microsoft Corporation Virtual Machine Xeon	ENVIADA	\N	2026-08-27 17:02:40.952218	2026-08-27 17:03:19.304541	\N	\N	1	Jhonatan David Rojo Ramos	\N	a	uploads/pdf/FormateoSeguro_123_a.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-27","entregado_a":"Jhonatan David Rojo Ramos","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correaa","cargo_entrega":"a","asunto":"a","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"a","estado":"","gb":"a"}]}	\N	\N	\N
104	\N	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-08-31 08:25:13.482704	\N	\N	\N	1	Mariana Castrillon Soto	\N	121	uploads/pdf/FormateoSeguro_123_aaa.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-08-31","entregado_a":"Mariana Castrillon Soto","cargo_recibe":"a","entregado_por":"Juan Jose Hernandez Correa","cargo_entrega":"aaa","asunto":"aaa","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"121","estado":"","gb":"2323"}]}	\N	\N	\N
133	\N	\N	\N	Microsoft Corporation Virtual Machine Xeon	GENERADA	\N	2026-09-03 07:58:05.823263	\N	\N	\N	1	Juan Jose Hernandez Correa	\N	1232114	uploads/pdf/FormateoSeguro_123_Equipo lento_e5db8549.pdf	123	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-03","entregado_a":"Juan Jose Hernandez Correa","correo":"","cargo_recibe":"aprend","entregado_por":"Julian Alejandro Celis Valderrama","cargo_entrega":"Analista","asunto":"Equipo lento","equipos":[{"serial":"123","marca":"Microsoft Corporation","tipo":"Hyper-V","modelo":"Virtual Machine Xeon","inventario":"1232114","estado":"","gb":"130"}]}	\N	\N	FormateoSeguro_123_Equipo lento_79ad9f03.zip
135	\N	\N	receptor.entrega@test.local	Dell Optiplex	GENERADA	\N	2026-09-03 08:22:07.350051	\N	\N	\N	49	Receptor Entrega	\N	INV-ENT	uploads/pdf/ActaEntrega_SN-ENT-FIX_ENTREGA CHECK_286e7e00.pdf	SN-ENT-FIX	999	ENTREGA	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-03","entregado_a":"Receptor Entrega","cargo_recibe":"Analista","entregado_por":"Tecnico Op","correo":"receptor.entrega@test.local","cargo_entrega":"Tecnico","asunto":"ENTREGA CHECK","hardware":[{"tipo":"Monitor","descripcion":"LG","programa":""}],"equipos":[{"serial":"SN-ENT-FIX","marca":"Dell","tipo":"Desktop","modelo":"Optiplex","inventario":"INV-ENT","estado":"","gb":""}],"checklist":{"chk_1":true},"numero_sac":"999","observaciones":"check","sistema_operativo":"WINDOWS_11"}	\N	uploads/pdf/Checklist_SN-ENT-FIX_ENTREGA CHECK_1d489cbf.pdf	ActaLista_SN-ENT-FIX_ENTREGA CHECK_ad4e3207.zip
134	\N	\N	maria.usuario@test.local	HP EliteBook	ENVIADA	\N	2026-09-03 08:21:42.781202	2026-09-03 08:24:27.187569	\N	\N	49	Maria Usuario	\N	INV-FORM-1	uploads/pdf/FormateoSeguro_SN-FORM-FIX-1_FORMATEO CORRECCION USUARIO_b9929bf1.pdf	SN-FORM-FIX-1	\N	FORMATEO	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"fecha":"2026-09-03","entregado_a":"Tecnico Recibe","correo":"maria.usuario@test.local","cargo_recibe":"Tecnico","entregado_por":"Maria Usuario","cargo_entrega":"Analista","asunto":"FORMATEO CORRECCION USUARIO","equipos":[{"serial":"SN-FORM-FIX-1","marca":"HP","tipo":"Portatil","modelo":"EliteBook","inventario":"INV-FORM-1","estado":"","gb":"512"}]}	\N	\N	FormateoSeguro_SN-FORM-FIX-1_FORMATEO CORRECCION USUARIO_841feddc.zip
\.


--
-- Data for Name: acta_historial; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acta_historial (id_historial, estado_anterior, estado_nuevo, fecha_cambio, id_acta, observacion, usuario_accion, actor_id, actor_nombre, id_token_firma, tipo_evento) FROM stdin;
248	\N	GENERADA	2026-08-21 12:07:20.938331	57	Acta de devolucion generada: uploads/pdf/Devolucion_123_a.pdf	8	8	8	\N	ACTA_GENERADA
278	GENERADA	ENVIADA	2026-08-24 16:29:10.381165	69	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=JuanHernandez@coltefinanciera.com.co; correo_enviado=NO	admin	1	admin	57	ACTA_ENVIADA
288	\N	ENVIADA	2026-08-25 08:32:34.620579	74	El usuario abrio el enlace de firma	Juan Jose Hernandez Correa (CC a)	\N	Juan Jose Hernandez Correa (CC a)	62	ACTA_ABIERTA_USUARIO
294	\N	ENVIADA	2026-08-25 08:48:23.772372	70	El usuario abrio el enlace de firma	Juan Jose Hernandez Correa (CC a)	\N	Juan Jose Hernandez Correa (CC a)	58	ACTA_ABIERTA_USUARIO
295	\N	ENVIADA	2026-08-25 08:48:24.053213	70	Tipo: FIRMA - uploads/firmas/firma_70.png	SISTEMA	\N	SISTEMA	58	EVIDENCIA_CARGADA
296	\N	ENVIADA	2026-08-25 08:48:24.05849	70	Tipo: FOTO - uploads/fotos/foto_70.jpg	SISTEMA	\N	SISTEMA	58	EVIDENCIA_CARGADA
297	ENVIADA	FIRMADA	2026-08-25 08:48:24.060506	70	Firma digital registrada	Juan Jose Hernandez Correa (CC a)	\N	Juan Jose Hernandez Correa (CC a)	58	ACTA_FIRMADA
429	\N	GENERADA	2026-09-02 10:10:37.450883	120	Acta generada	sec011_tc	41	sec011_tc	\N	ACTA_GENERADA
101	\N	GENERADA	2026-07-31 10:42:38.453624	33	Acta generada	admin	1	admin	\N	ACTA_GENERADA
103	GENERADA	ENVIADA	2026-07-31 10:42:56.694941	33	Acta enviada para firma	admin	1	admin	24	ACTA_ENVIADA
104	\N	ENVIADA	2026-07-31 10:43:08.964137	32	El usuario abrio el enlace de firma	qqqq	\N	qqqq	23	ACTA_ABIERTA_USUARIO
110	\N	ENVIADA	2026-07-31 10:47:23.068513	33	El usuario abrio el enlace de firma	nnnn (CC nnnn)	\N	nnnn (CC nnnn)	24	ACTA_ABIERTA_USUARIO
111	ENVIADA	RECHAZADA	2026-07-31 10:47:28.335315	33	No estoy de acuerdo con el contenido	nnnn (CC nnnn)	\N	nnnn (CC nnnn)	24	ACTA_RECHAZADA_USUARIO
112	FIRMADA	APROBADA	2026-07-31 10:47:44.745223	32	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
127	\N	ENVIADA	2026-07-31 11:19:38.769079	36	El usuario abrio el enlace de firma	kkkk (CC kkkk)	\N	kkkk (CC kkkk)	27	ACTA_ABIERTA_USUARIO
151	\N	GENERADA	2026-07-31 15:15:16.052443	37	Acta generada	admin	1	admin	\N	ACTA_GENERADA
152	GENERADA	ENVIADA	2026-07-31 15:15:34.789463	37	Acta enviada para firma	admin	1	admin	31	ACTA_ENVIADA
154	\N	ENVIADA	2026-07-31 15:16:27.070206	37	Tipo: FIRMA - uploads/firmas/firma_37.png	SISTEMA	\N	SISTEMA	31	EVIDENCIA_CARGADA
155	\N	ENVIADA	2026-07-31 15:16:27.078691	37	Tipo: FOTO - uploads/fotos/foto_37.jpg	SISTEMA	\N	SISTEMA	31	EVIDENCIA_CARGADA
156	ENVIADA	FIRMADA	2026-07-31 15:16:27.087081	37	Firma digital registrada	ffff	\N	ffff	31	ACTA_FIRMADA
157	\N	FIRMADA	2026-07-31 15:17:13.32755	37	Tipo: PDF_FINAL - uploads/pdf/ActaEntrega_123_ffff.pdf	SISTEMA	\N	SISTEMA	31	EVIDENCIA_CARGADA
158	\N	FIRMADA	2026-07-31 15:17:13.335241	37	PDF del documento firmado regenerado: uploads/pdf/ActaEntrega_123_ffff.pdf	SISTEMA	\N	SISTEMA	31	PDF_REGENERADO
170	\N	GENERADA	2026-07-31 16:18:27.35226	38	Acta generada	admin	1	admin	\N	ACTA_GENERADA
171	GENERADA	ENVIADA	2026-07-31 16:18:39.579711	38	Acta enviada para firma	admin	1	admin	35	ACTA_ENVIADA
172	\N	ENVIADA	2026-07-31 16:18:45.439304	38	El usuario abrio el enlace de firma	ssss	\N	ssss	35	ACTA_ABIERTA_USUARIO
173	\N	ENVIADA	2026-07-31 16:19:03.253845	38	Tipo: FIRMA - uploads/firmas/firma_38.png	SISTEMA	\N	SISTEMA	35	EVIDENCIA_CARGADA
174	\N	ENVIADA	2026-07-31 16:19:03.259243	38	Tipo: FOTO - uploads/fotos/foto_38.jpg	SISTEMA	\N	SISTEMA	35	EVIDENCIA_CARGADA
175	ENVIADA	FIRMADA	2026-07-31 16:19:03.263276	38	Firma digital registrada	ssss	\N	ssss	35	ACTA_FIRMADA
176	\N	FIRMADA	2026-07-31 16:19:43.13972	38	Tipo: PDF_FINAL - uploads/pdf/ActaEntrega_123_ssss.pdf	SISTEMA	\N	SISTEMA	35	EVIDENCIA_CARGADA
177	\N	FIRMADA	2026-07-31 16:19:43.146511	38	PDF del documento firmado regenerado: uploads/pdf/ActaEntrega_123_ssss.pdf	SISTEMA	\N	SISTEMA	35	PDF_REGENERADO
178	FIRMADA	APROBADA	2026-07-31 16:21:29.462972	38	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
180	\N	GENERADA	2026-07-31 16:23:52.400692	39	Acta generada	admin	1	admin	\N	ACTA_GENERADA
182	\N	ENVIADA	2026-08-03 14:58:05.760241	39	El usuario abrio el enlace de firma	qqqq	\N	qqqq	36	ACTA_ABIERTA_USUARIO
186	\N	ENVIADA	2026-08-04 08:36:55.561626	40	El usuario abrio el enlace de firma	Andres Mauricio Muñoz Tascon	\N	Andres Mauricio Muñoz Tascon	37	ACTA_ABIERTA_USUARIO
188	GENERADA	ENVIADA	2026-08-04 14:16:47.49067	41	Acta enviada para firma; correo_detectado_glpi=daviguzm@coltefinanciera.com.co; correo_utilizado=daviguzm@coltefinanciera.com.co; correo_enviado=NO	admin	1	admin	38	ACTA_ENVIADA
189	\N	ENVIADA	2026-08-04 14:17:02.781961	41	El usuario abrio el enlace de firma	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	38	ACTA_ABIERTA_USUARIO
190	\N	ENVIADA	2026-08-04 14:17:55.989028	41	Tipo: FIRMA - uploads/firmas/firma_41.png	SISTEMA	\N	SISTEMA	38	EVIDENCIA_CARGADA
191	\N	ENVIADA	2026-08-04 14:17:55.99919	41	Tipo: FOTO - uploads/fotos/foto_41.jpg	SISTEMA	\N	SISTEMA	38	EVIDENCIA_CARGADA
192	ENVIADA	FIRMADA	2026-08-04 14:17:56.005247	41	Firma digital registrada	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	38	ACTA_FIRMADA
193	\N	FIRMADA	2026-08-04 14:18:13.212866	41	Tipo: PDF_FINAL - uploads/pdf/ActaEntrega_123_NuevoUsuario.pdf	SISTEMA	\N	SISTEMA	38	EVIDENCIA_CARGADA
194	\N	FIRMADA	2026-08-04 14:18:13.215981	41	PDF del documento firmado regenerado: uploads/pdf/ActaEntrega_123_NuevoUsuario.pdf	SISTEMA	\N	SISTEMA	38	PDF_REGENERADO
195	FIRMADA	APROBADA	2026-08-04 14:22:47.454712	41	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
202	\N	GENERADA	2026-08-21 09:41:00.50902	48	Acta de devolucion generada: uploads/pdf/Devolucion_123_eeee.pdf	1	1	1	\N	ACTA_GENERADA
204	GENERADA	ENVIADA	2026-08-21 09:54:54.722064	48	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=razortxz@gmail.com; correo_enviado=NO	admin	1	admin	39	ACTA_ENVIADA
206	\N	ENVIADA	2026-08-21 10:02:05.188695	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
208	\N	ENVIADA	2026-08-21 10:03:47.542085	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
1	\N	GENERADA	2026-07-27 12:56:59.568308	1	\N	admin	\N	admin	\N	ACTA_GENERADA
249	GENERADA	ENVIADA	2026-08-21 12:07:45.322014	57	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=razortxz@gmail.com; correo_enviado=NO	juantec	8	juantec	45	ACTA_ENVIADA
102	GENERADA	ENVIADA	2026-07-31 10:42:54.739885	32	Acta enviada para firma	admin	1	admin	23	ACTA_ENVIADA
105	\N	ENVIADA	2026-07-31 10:44:20.144636	32	Tipo: FIRMA - uploads/firmas/firma_32.png	SISTEMA	\N	SISTEMA	23	EVIDENCIA_CARGADA
106	\N	ENVIADA	2026-07-31 10:44:20.151086	32	Tipo: FOTO - uploads/fotos/foto_32.jpg	SISTEMA	\N	SISTEMA	23	EVIDENCIA_CARGADA
107	ENVIADA	FIRMADA	2026-07-31 10:44:20.155974	32	Firma digital registrada	qqqq	\N	qqqq	23	ACTA_FIRMADA
108	\N	FIRMADA	2026-07-31 10:45:00.191083	32	Tipo: PDF_FINAL - uploads/pdf/ActaEntrega_CCV3F33_qqqq.pdf	SISTEMA	\N	SISTEMA	23	EVIDENCIA_CARGADA
109	\N	FIRMADA	2026-07-31 10:45:00.26055	32	PDF del documento firmado regenerado: uploads/pdf/ActaEntrega_CCV3F33_qqqq.pdf	SISTEMA	\N	SISTEMA	23	PDF_REGENERADO
123	\N	GENERADA	2026-07-31 11:19:12.034019	36	Acta generada	admin	1	admin	\N	ACTA_GENERADA
124	GENERADA	ENVIADA	2026-07-31 11:19:29.8683	36	Acta enviada para firma	admin	1	admin	27	ACTA_ENVIADA
128	\N	ENVIADA	2026-07-31 11:19:53.748663	36	Tipo: FIRMA - uploads/firmas/firma_36.png	SISTEMA	\N	SISTEMA	27	EVIDENCIA_CARGADA
129	\N	ENVIADA	2026-07-31 11:19:53.754173	36	Tipo: FOTO - uploads/fotos/foto_36.jpg	SISTEMA	\N	SISTEMA	27	EVIDENCIA_CARGADA
130	ENVIADA	FIRMADA	2026-07-31 11:19:53.764703	36	Firma digital registrada	kkkk (CC kkkk)	\N	kkkk (CC kkkk)	27	ACTA_FIRMADA
131	\N	FIRMADA	2026-07-31 11:20:30.626633	36	Tipo: PDF_FINAL - uploads/pdf/Devolucion_30JZTN3_kkkk.pdf	SISTEMA	\N	SISTEMA	27	EVIDENCIA_CARGADA
132	\N	FIRMADA	2026-07-31 11:20:30.632015	36	PDF del documento firmado regenerado: uploads/pdf/Devolucion_30JZTN3_kkkk.pdf	SISTEMA	\N	SISTEMA	27	PDF_REGENERADO
133	FIRMADA	APROBADA	2026-07-31 11:21:00.951055	36	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
279	\N	GENERADA	2026-08-24 16:31:08.415149	70	Acta de devolucion generada: uploads/pdf/Devolucion_123_a.pdf	1	1	1	\N	ACTA_GENERADA
289	\N	ENVIADA	2026-08-25 08:34:06.498491	74	Tipo: FIRMA - uploads/firmas/firma_74.png	SISTEMA	\N	SISTEMA	62	EVIDENCIA_CARGADA
290	\N	ENVIADA	2026-08-25 08:34:06.506248	74	Tipo: FOTO - uploads/fotos/foto_74.jpg	SISTEMA	\N	SISTEMA	62	EVIDENCIA_CARGADA
291	ENVIADA	FIRMADA	2026-08-25 08:34:06.506248	74	Firma digital registrada	Juan Jose Hernandez Correa (CC a)	\N	Juan Jose Hernandez Correa (CC a)	62	ACTA_FIRMADA
153	\N	ENVIADA	2026-07-31 15:15:58.238283	37	El usuario abrio el enlace de firma	ffff	\N	ffff	31	ACTA_ABIERTA_USUARIO
292	\N	FIRMADA	2026-08-25 08:34:33.436597	74	Tipo: PDF_FINAL - uploads/pdf/Devolucion_123_a.pdf	SISTEMA	\N	SISTEMA	62	EVIDENCIA_CARGADA
293	\N	FIRMADA	2026-08-25 08:34:33.444116	74	PDF del documento firmado regenerado: uploads/pdf/Devolucion_123_a.pdf	SISTEMA	\N	SISTEMA	62	PDF_REGENERADO
300	\N	ENVIADA	2026-08-25 09:42:03.051213	74	Tipo: FIRMA - uploads/firmas/firma_74.png	SISTEMA	\N	SISTEMA	62	EVIDENCIA_CARGADA
301	\N	ENVIADA	2026-08-25 09:42:03.062761	74	Tipo: FOTO - uploads/fotos/foto_74.jpg	SISTEMA	\N	SISTEMA	62	EVIDENCIA_CARGADA
302	ENVIADA	FIRMADA	2026-08-25 09:42:03.078865	74	Firma digital registrada	Juan Jose Hernandez Correa (CC a)	\N	Juan Jose Hernandez Correa (CC a)	62	ACTA_FIRMADA
303	\N	FIRMADA	2026-08-25 09:42:23.232151	74	Tipo: PDF_FINAL - uploads/pdf/Devolucion_123_a_acta74.pdf	SISTEMA	\N	SISTEMA	62	EVIDENCIA_CARGADA
304	\N	FIRMADA	2026-08-25 09:42:23.244328	74	PDF del documento firmado regenerado: uploads/pdf/Devolucion_123_a_acta74.pdf	SISTEMA	\N	SISTEMA	62	PDF_REGENERADO
179	FIRMADA	RECHAZADA	2026-07-31 16:21:48.720179	37	No firmó bien	admin	1	admin	\N	ACTA_RECHAZADA_ADMIN
181	GENERADA	ENVIADA	2026-08-03 14:57:55.840447	39	Acta enviada para firma	admin	1	admin	36	ACTA_ENVIADA
183	\N	ENVIADA	2026-08-03 16:17:24.349118	39	El usuario abrio el enlace de firma	qqqq	\N	qqqq	36	ACTA_ABIERTA_USUARIO
184	\N	GENERADA	2026-08-04 08:35:50.925778	40	Acta generada	admin	1	admin	\N	ACTA_GENERADA
185	GENERADA	ENVIADA	2026-08-04 08:36:46.844172	40	Acta enviada para firma; correo_detectado_glpi=andres.munoz@flamingo.com.co; correo_utilizado=JuanHernandez@coltefinanciera.com.co; correo_enviado=NO	admin	1	admin	37	ACTA_ENVIADA
187	\N	GENERADA	2026-08-04 14:14:12.394104	41	Acta generada	admin	1	admin	\N	ACTA_GENERADA
308	\N	ENVIADA	2026-08-25 10:01:52.905024	75	Tipo: FIRMA - uploads/firmas/firma_75.png	SISTEMA	\N	SISTEMA	63	EVIDENCIA_CARGADA
309	\N	ENVIADA	2026-08-25 10:01:52.908066	75	Tipo: FOTO - uploads/fotos/foto_75.jpg	SISTEMA	\N	SISTEMA	63	EVIDENCIA_CARGADA
201	\N	GENERADA	2026-08-21 09:39:10.021528	47	Acta de devolucion generada: uploads/pdf/Devolucion_123_1.pdf	1	1	1	\N	ACTA_GENERADA
203	\N	GENERADA	2026-08-21 09:52:25.744107	49	Acta de entrega generada: uploads/pdf/ActaEntrega_123_yyyy.pdf	1	1	1	\N	ACTA_GENERADA
205	\N	ENVIADA	2026-08-21 09:55:29.536173	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
207	\N	ENVIADA	2026-08-21 10:02:05.875122	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
310	ENVIADA	FIRMADA	2026-08-25 10:01:52.910075	75	Firma digital registrada	David Alejandro Guzman Franco (CC 123)	\N	David Alejandro Guzman Franco (CC 123)	63	ACTA_FIRMADA
311	\N	FIRMADA	2026-08-25 10:02:06.433391	75	Tipo: PDF_FINAL - uploads/pdf/Devolucion_5CD2256W6H_aaaa_acta75.pdf	SISTEMA	\N	SISTEMA	63	EVIDENCIA_CARGADA
314	\N	ENVIADA	2026-08-25 10:08:44.97596	57	Tipo: FIRMA - uploads/firmas/firma_57.png	SISTEMA	\N	SISTEMA	45	EVIDENCIA_CARGADA
2	GENERADA	ENVIADA	2026-07-27 15:20:30.661682	1	\N	admin	\N	admin	\N	ACTA_ENVIADA
3	ENVIADA	FIRMADA	2026-07-27 15:23:17.455937	1	Firma digital registrada	SISTEMA	\N	Juan Perez	\N	ACTA_FIRMADA
4	FIRMADA	APROBADA	2026-07-28 08:04:16.965659	1	\N	admin	\N	admin	\N	ACTA_APROBADA
11	\N	GENERADA	2026-07-29 11:26:48.092815	4	\N	admin	\N	admin	\N	ACTA_GENERADA
12	\N	GENERADA	2026-07-29 12:08:11.481146	5	\N	admin	\N	admin	\N	ACTA_GENERADA
13	\N	GENERADA	2026-07-29 16:09:30.276096	6	\N	admin	\N	admin	\N	ACTA_GENERADA
14	\N	GENERADA	2026-07-30 08:25:51.163297	7	\N	admin	\N	admin	\N	ACTA_GENERADA
15	\N	GENERADA	2026-07-30 08:34:54.37008	8	\N	admin	\N	admin	\N	ACTA_GENERADA
16	\N	GENERADA	2026-07-30 08:52:58.460746	9	\N	admin	\N	admin	\N	ACTA_GENERADA
17	GENERADA	ENVIADA	2026-07-30 08:53:48.889383	9	\N	admin	\N	admin	\N	ACTA_ENVIADA
18	ENVIADA	FIRMADA	2026-07-30 08:55:38.082814	9	Firma digital registrada	SISTEMA	\N	tttt	\N	ACTA_FIRMADA
19	\N	GENERADA	2026-07-30 09:24:04.705058	10	\N	admin	\N	admin	\N	ACTA_GENERADA
20	GENERADA	ENVIADA	2026-07-30 09:24:24.674439	10	\N	admin	\N	admin	\N	ACTA_ENVIADA
21	ENVIADA	FIRMADA	2026-07-30 09:25:10.115704	10	Firma digital registrada	SISTEMA	\N	yyyy	\N	ACTA_FIRMADA
22	FIRMADA	APROBADA	2026-07-30 09:27:17.102024	10	\N	admin	\N	admin	\N	ACTA_APROBADA
23	FIRMADA	RECHAZADA	2026-07-30 09:27:29.52907	9	Foto invalida	admin	\N	admin	\N	ACTA_RECHAZADA_ADMIN
24	\N	GENERADA	2026-07-30 09:53:04.988584	11	\N	admin	\N	admin	\N	ACTA_GENERADA
25	\N	GENERADA	2026-07-30 10:05:25.907054	12	\N	admin	\N	admin	\N	ACTA_GENERADA
26	\N	GENERADA	2026-07-30 10:12:25.017076	13	\N	admin	\N	admin	\N	ACTA_GENERADA
27	\N	GENERADA	2026-07-30 10:52:25.513573	14	\N	admin	\N	admin	\N	ACTA_GENERADA
28	GENERADA	ENVIADA	2026-07-30 12:14:26.996798	14	\N	admin	\N	admin	\N	ACTA_ENVIADA
29	ENVIADA	FIRMADA	2026-07-30 12:15:14.23315	14	Firma digital registrada	SISTEMA	\N	jjjjj	\N	ACTA_FIRMADA
30	FIRMADA	APROBADA	2026-07-30 12:15:24.603441	14	\N	admin	\N	admin	\N	ACTA_APROBADA
31	\N	GENERADA	2026-07-30 12:32:00.098879	15	\N	admin	\N	admin	\N	ACTA_GENERADA
32	GENERADA	ENVIADA	2026-07-30 12:33:00.566299	15	\N	admin	\N	admin	\N	ACTA_ENVIADA
33	ENVIADA	FIRMADA	2026-07-30 12:33:17.37151	15	Firma digital registrada	SISTEMA	\N	yyyy	\N	ACTA_FIRMADA
34	FIRMADA	APROBADA	2026-07-30 14:49:50.807415	15	\N	admin	\N	admin	\N	ACTA_APROBADA
35	\N	GENERADA	2026-07-30 14:50:46.00442	16	\N	admin	\N	admin	\N	ACTA_GENERADA
36	\N	GENERADA	2026-07-30 15:00:12.615557	17	\N	admin	\N	admin	\N	ACTA_GENERADA
37	GENERADA	ENVIADA	2026-07-30 15:00:39.614111	17	\N	admin	\N	admin	\N	ACTA_ENVIADA
38	ENVIADA	FIRMADA	2026-07-30 15:01:05.623036	17	Firma digital registrada	SISTEMA	\N	nnnn	\N	ACTA_FIRMADA
39	\N	GENERADA	2026-07-30 15:06:30.689836	18	\N	admin	\N	admin	\N	ACTA_GENERADA
40	GENERADA	ENVIADA	2026-07-30 15:06:42.994568	18	\N	admin	\N	admin	\N	ACTA_ENVIADA
41	FIRMADA	APROBADA	2026-07-30 15:06:49.38794	17	\N	admin	\N	admin	\N	ACTA_APROBADA
42	ENVIADA	FIRMADA	2026-07-30 15:07:10.395731	18	Firma digital registrada	SISTEMA	\N	uuuu	\N	ACTA_FIRMADA
43	\N	GENERADA	2026-07-30 15:11:11.975161	19	\N	admin	\N	admin	\N	ACTA_GENERADA
44	GENERADA	ENVIADA	2026-07-30 15:11:29.717132	19	\N	admin	\N	admin	\N	ACTA_ENVIADA
45	ENVIADA	FIRMADA	2026-07-30 15:11:56.729678	19	Firma digital registrada	SISTEMA	\N	gggg	\N	ACTA_FIRMADA
46	\N	GENERADA	2026-07-31 07:38:18.711509	20	\N	admin	\N	admin	\N	ACTA_GENERADA
47	\N	GENERADA	2026-07-31 07:39:13.074171	21	\N	admin	\N	admin	\N	ACTA_GENERADA
48	GENERADA	ENVIADA	2026-07-31 07:39:56.167032	20	\N	admin	\N	admin	\N	ACTA_ENVIADA
49	GENERADA	ENVIADA	2026-07-31 07:39:58.556363	21	\N	admin	\N	admin	\N	ACTA_ENVIADA
50	FIRMADA	APROBADA	2026-07-31 07:40:10.29074	19	\N	admin	\N	admin	\N	ACTA_APROBADA
51	FIRMADA	APROBADA	2026-07-31 07:40:11.042654	18	\N	admin	\N	admin	\N	ACTA_APROBADA
52	ENVIADA	FIRMADA	2026-07-31 07:40:54.035757	20	Firma digital registrada	SISTEMA	\N	acta entrega	\N	ACTA_FIRMADA
53	ENVIADA	FIRMADA	2026-07-31 07:41:49.020383	21	Firma digital registrada	SISTEMA	\N	acta devolucion	\N	ACTA_FIRMADA
54	\N	GENERADA	2026-07-31 07:55:26.393807	22	\N	admin	\N	admin	\N	ACTA_GENERADA
55	GENERADA	ENVIADA	2026-07-31 07:55:35.092369	22	\N	admin	\N	admin	\N	ACTA_ENVIADA
56	ENVIADA	FIRMADA	2026-07-31 07:56:00.331773	22	Firma digital registrada	SISTEMA	\N	tttt	\N	ACTA_FIRMADA
57	\N	GENERADA	2026-07-31 08:10:31.204896	23	\N	admin	\N	admin	\N	ACTA_GENERADA
58	GENERADA	ENVIADA	2026-07-31 08:10:48.76069	23	\N	admin	\N	admin	\N	ACTA_ENVIADA
59	ENVIADA	FIRMADA	2026-07-31 08:11:34.595889	23	Firma digital registrada	SISTEMA	\N	rrrrr	\N	ACTA_FIRMADA
60	FIRMADA	APROBADA	2026-07-31 08:12:44.665712	23	\N	admin	\N	admin	\N	ACTA_APROBADA
61	FIRMADA	APROBADA	2026-07-31 08:12:45.508802	22	\N	admin	\N	admin	\N	ACTA_APROBADA
62	FIRMADA	APROBADA	2026-07-31 08:12:46.317443	21	\N	admin	\N	admin	\N	ACTA_APROBADA
63	FIRMADA	APROBADA	2026-07-31 08:12:47.430054	20	\N	admin	\N	admin	\N	ACTA_APROBADA
65	\N	GENERADA	2026-07-31 09:03:37.973082	24	\N	admin	\N	admin	\N	ACTA_GENERADA
66	GENERADA	ENVIADA	2026-07-31 09:03:52.563108	24	\N	admin	\N	admin	\N	ACTA_ENVIADA
67	ENVIADA	RECHAZADA	2026-07-31 09:06:45.671764	24	No corresponde a mi equipo	SISTEMA	\N	pppp	\N	ACTA_RECHAZADA_USUARIO
68	\N	GENERADA	2026-07-31 09:07:57.896933	25	\N	admin	\N	admin	\N	ACTA_GENERADA
69	GENERADA	ENVIADA	2026-07-31 09:08:17.962033	25	\N	admin	\N	admin	\N	ACTA_ENVIADA
70	ENVIADA	FIRMADA	2026-07-31 09:10:11.658204	25	Firma digital registrada	SISTEMA	\N	wwww	\N	ACTA_FIRMADA
71	FIRMADA	APROBADA	2026-07-31 09:14:12.369194	25	\N	admin	\N	admin	\N	ACTA_APROBADA
72	\N	GENERADA	2026-07-31 09:15:29.621372	26	\N	admin	\N	admin	\N	ACTA_GENERADA
73	GENERADA	ENVIADA	2026-07-31 09:16:17.153361	26	\N	admin	\N	admin	\N	ACTA_ENVIADA
74	ENVIADA	FIRMADA	2026-07-31 09:18:31.68466	26	Firma digital registrada	SISTEMA	\N	Juan Hernandez	\N	ACTA_FIRMADA
75	\N	GENERADA	2026-07-31 09:21:36.596181	27	\N	admin	\N	admin	\N	ACTA_GENERADA
76	GENERADA	ENVIADA	2026-07-31 09:21:42.822667	27	\N	admin	\N	admin	\N	ACTA_ENVIADA
77	ENVIADA	FIRMADA	2026-07-31 09:22:50.048228	27	Firma digital registrada	SISTEMA	\N	Angie	\N	ACTA_FIRMADA
98	FIRMADA	APROBADA	2026-07-31 10:39:19.522365	27	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
99	FIRMADA	APROBADA	2026-07-31 10:39:20.251966	26	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
100	\N	GENERADA	2026-07-31 10:40:24.980716	32	Acta generada	admin	1	admin	\N	ACTA_GENERADA
209	\N	ENVIADA	2026-08-21 10:04:38.25878	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
210	\N	ENVIADA	2026-08-21 10:05:32.170699	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
211	\N	ENVIADA	2026-08-21 10:05:33.253644	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
212	\N	ENVIADA	2026-08-21 10:06:30.026266	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
213	\N	ENVIADA	2026-08-21 10:15:39.955896	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
214	\N	ENVIADA	2026-08-21 10:15:40.487471	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
215	\N	ENVIADA	2026-08-21 10:15:46.58641	48	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 9999)	\N	Julian Alejandro Celis Valderrama (CC 9999)	39	ACTA_ABIERTA_USUARIO
216	\N	GENERADA	2026-08-21 10:15:47.699528	50	Acta de devolucion generada: uploads/pdf/Devolucion_123_t.pdf	1	1	1	\N	ACTA_GENERADA
217	GENERADA	ENVIADA	2026-08-21 10:17:00.407471	50	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=razortxz@gmail.com; correo_enviado=NO	admin	1	admin	40	ACTA_ENVIADA
218	\N	ENVIADA	2026-08-21 10:17:06.512239	50	El usuario abrio el enlace de firma	Jhonatan David Rojo Ramos (CC 123)	\N	Jhonatan David Rojo Ramos (CC 123)	40	ACTA_ABIERTA_USUARIO
219	\N	ENVIADA	2026-08-21 10:17:50.446108	50	Tipo: FIRMA - uploads/firmas/firma_50.png	SISTEMA	\N	SISTEMA	40	EVIDENCIA_CARGADA
220	\N	ENVIADA	2026-08-21 10:17:50.446108	50	Tipo: FOTO - uploads/fotos/foto_50.jpg	SISTEMA	\N	SISTEMA	40	EVIDENCIA_CARGADA
221	ENVIADA	FIRMADA	2026-08-21 10:17:50.454441	50	Firma digital registrada	Jhonatan David Rojo Ramos (CC 123)	\N	Jhonatan David Rojo Ramos (CC 123)	40	ACTA_FIRMADA
222	\N	FIRMADA	2026-08-21 10:18:32.948035	50	Tipo: PDF_FINAL - uploads/pdf/Devolucion_123_t.pdf	SISTEMA	\N	SISTEMA	40	EVIDENCIA_CARGADA
223	\N	FIRMADA	2026-08-21 10:18:32.949589	50	PDF del documento firmado regenerado: uploads/pdf/Devolucion_123_t.pdf	SISTEMA	\N	SISTEMA	40	PDF_REGENERADO
224	FIRMADA	APROBADA	2026-08-21 10:19:34.612146	50	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
225	\N	GENERADA	2026-08-21 10:30:26.069295	51	Acta de devolucion generada: uploads/pdf/Devolucion_11_oo.pdf	8	8	8	\N	ACTA_GENERADA
250	\N	ENVIADA	2026-08-21 13:09:49.499731	57	El usuario abrio el enlace de firma	Juan Bautista Mahecha Villamil (CC |)	\N	Juan Bautista Mahecha Villamil (CC |)	45	ACTA_ABIERTA_USUARIO
231	GENERADA	ENVIADA	2026-08-21 10:55:20.955103	51	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=razortxz@gmail.com; correo_enviado=NO	juantec	8	juantec	42	ACTA_ENVIADA
274	\N	GENERADA	2026-08-24 16:00:08.417451	68	Acta generada	otptest	20	otptest	\N	ACTA_GENERADA
275	GENERADA	ENVIADA	2026-08-24 16:00:11.877671	68	Acta enviada para firma; correo_detectado_glpi=razortxz@gmail.com; correo_utilizado=razortxz@gmail.com; correo_enviado=SI	otptest	20	otptest	56	ACTA_ENVIADA
276	\N	ENVIADA	2026-08-24 16:04:38.310649	68	El usuario abrio el enlace de firma	Prueba SMTP Real (CC 9876543)	\N	Prueba SMTP Real (CC 9876543)	56	ACTA_ABIERTA_USUARIO
277	\N	GENERADA	2026-08-24 16:24:52.910753	69	Acta de devolucion generada: uploads/pdf/Devolucion_123_a.pdf	1	1	1	\N	ACTA_GENERADA
280	GENERADA	ENVIADA	2026-08-24 16:32:05.738957	70	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=NO	admin	1	admin	58	ACTA_ENVIADA
286	\N	GENERADA	2026-08-25 08:29:13.966768	74	Acta de devolucion generada: uploads/pdf/Devolucion_123_a.pdf	1	1	1	\N	ACTA_GENERADA
287	GENERADA	ENVIADA	2026-08-25 08:30:27.843427	74	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	62	ACTA_ENVIADA
298	\N	FIRMADA	2026-08-25 08:48:38.590333	70	Tipo: PDF_FINAL - uploads/pdf/Devolucion_123_a.pdf	SISTEMA	\N	SISTEMA	58	EVIDENCIA_CARGADA
299	\N	FIRMADA	2026-08-25 08:48:38.59322	70	PDF del documento firmado regenerado: uploads/pdf/Devolucion_123_a.pdf	SISTEMA	\N	SISTEMA	58	PDF_REGENERADO
305	\N	GENERADA	2026-08-25 09:59:08.341045	75	Acta de devolucion generada: uploads/pdf/Devolucion_5CD2256W6H_aaaa.pdf	1	1	1	\N	ACTA_GENERADA
306	GENERADA	ENVIADA	2026-08-25 09:59:56.07996	75	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	63	ACTA_ENVIADA
307	\N	ENVIADA	2026-08-25 10:01:04.209498	75	El usuario abrio el enlace de firma	David Alejandro Guzman Franco (CC 123)	\N	David Alejandro Guzman Franco (CC 123)	63	ACTA_ABIERTA_USUARIO
312	\N	FIRMADA	2026-08-25 10:02:06.438958	75	PDF del documento firmado regenerado: uploads/pdf/Devolucion_5CD2256W6H_aaaa_acta75.pdf	SISTEMA	\N	SISTEMA	63	PDF_REGENERADO
313	\N	ENVIADA	2026-08-25 10:08:44.211659	57	El usuario abrio el enlace de firma	Juan Bautista Mahecha Villamil (CC |)	\N	Juan Bautista Mahecha Villamil (CC |)	45	ACTA_ABIERTA_USUARIO
315	\N	ENVIADA	2026-08-25 10:08:44.980456	57	Tipo: FOTO - uploads/fotos/foto_57.jpg	SISTEMA	\N	SISTEMA	45	EVIDENCIA_CARGADA
316	ENVIADA	FIRMADA	2026-08-25 10:08:44.980456	57	Firma digital registrada	Juan Bautista Mahecha Villamil (CC |)	\N	Juan Bautista Mahecha Villamil (CC |)	45	ACTA_FIRMADA
317	\N	FIRMADA	2026-08-25 10:09:02.690627	57	Tipo: PDF_FINAL - uploads/pdf/Devolucion_123_a_acta57.pdf	SISTEMA	\N	SISTEMA	45	EVIDENCIA_CARGADA
318	\N	FIRMADA	2026-08-25 10:09:02.695124	57	PDF del documento firmado regenerado: uploads/pdf/Devolucion_123_a_acta57.pdf	SISTEMA	\N	SISTEMA	45	PDF_REGENERADO
319	\N	GENERADA	2026-08-25 10:16:14.540574	76	Acta de devolucion generada: uploads/pdf/Devolucion_4K16ZW3_a.pdf	1	1	1	\N	ACTA_GENERADA
320	GENERADA	ENVIADA	2026-08-25 10:17:09.900028	76	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	64	ACTA_ENVIADA
321	\N	ENVIADA	2026-08-25 10:17:31.958907	76	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 123)	\N	Julian Alejandro Celis Valderrama (CC 123)	64	ACTA_ABIERTA_USUARIO
322	\N	ENVIADA	2026-08-25 10:17:53.481141	76	Tipo: FIRMA - uploads/firmas/firma_76.png	SISTEMA	\N	SISTEMA	64	EVIDENCIA_CARGADA
323	\N	ENVIADA	2026-08-25 10:17:53.484258	76	Tipo: FOTO - uploads/fotos/foto_76.jpg	SISTEMA	\N	SISTEMA	64	EVIDENCIA_CARGADA
324	ENVIADA	FIRMADA	2026-08-25 10:17:53.487281	76	Firma digital registrada	Julian Alejandro Celis Valderrama (CC 123)	\N	Julian Alejandro Celis Valderrama (CC 123)	64	ACTA_FIRMADA
325	\N	FIRMADA	2026-08-25 10:18:10.259782	76	Tipo: PDF_FINAL - uploads/pdf/Devolucion_4K16ZW3_a_acta76.pdf	SISTEMA	\N	SISTEMA	64	EVIDENCIA_CARGADA
326	\N	FIRMADA	2026-08-25 10:18:10.259782	76	PDF del documento firmado regenerado: uploads/pdf/Devolucion_4K16ZW3_a_acta76.pdf	SISTEMA	\N	SISTEMA	64	PDF_REGENERADO
327	\N	GENERADA	2026-08-25 10:22:32.588084	77	Acta de devolucion generada: uploads/pdf/Devolucion_4K16ZW3_aa.pdf	1	1	1	\N	ACTA_GENERADA
328	GENERADA	ENVIADA	2026-08-25 10:22:46.533726	77	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	65	ACTA_ENVIADA
329	\N	ENVIADA	2026-08-25 10:23:09.546128	77	El usuario abrio el enlace de firma	Julian Alejandro Celis Valderrama (CC 123)	\N	Julian Alejandro Celis Valderrama (CC 123)	65	ACTA_ABIERTA_USUARIO
330	\N	ENVIADA	2026-08-25 10:23:30.288447	77	Tipo: FIRMA - uploads/firmas/firma_77.png	SISTEMA	\N	SISTEMA	65	EVIDENCIA_CARGADA
331	\N	ENVIADA	2026-08-25 10:23:30.288447	77	Tipo: FOTO - uploads/fotos/foto_77.jpg	SISTEMA	\N	SISTEMA	65	EVIDENCIA_CARGADA
332	ENVIADA	FIRMADA	2026-08-25 10:23:30.288447	77	Firma digital registrada	Julian Alejandro Celis Valderrama (CC 123)	\N	Julian Alejandro Celis Valderrama (CC 123)	65	ACTA_FIRMADA
333	\N	FIRMADA	2026-08-25 10:23:40.323237	77	Tipo: PDF_FINAL - uploads/pdf/Devolucion_4K16ZW3_aa_acta77.pdf	SISTEMA	\N	SISTEMA	65	EVIDENCIA_CARGADA
334	\N	FIRMADA	2026-08-25 10:23:40.323237	77	PDF del documento firmado regenerado: uploads/pdf/Devolucion_4K16ZW3_aa_acta77.pdf	SISTEMA	\N	SISTEMA	65	PDF_REGENERADO
335	\N	ENVIADA	2026-08-25 10:52:03.094222	69	El usuario abrio el enlace de firma	Juan Jose Hernandez Correa (CC 1)	\N	Juan Jose Hernandez Correa (CC 1)	57	ACTA_ABIERTA_USUARIO
336	\N	ENVIADA	2026-08-25 10:57:41.074688	68	El usuario abrio el enlace de firma	Prueba SMTP Real (CC 9876543)	\N	Prueba SMTP Real (CC 9876543)	56	ACTA_ABIERTA_USUARIO
337	\N	ENVIADA	2026-08-25 11:09:00.855455	68	El usuario abrio el enlace de firma	Prueba SMTP Real (CC 9876543)	\N	Prueba SMTP Real (CC 9876543)	56	ACTA_ABIERTA_USUARIO
338	\N	ENVIADA	2026-08-25 11:09:02.744886	68	Tipo: FIRMA - uploads/firmas/firma_68.png	SISTEMA	\N	SISTEMA	56	EVIDENCIA_CARGADA
339	\N	ENVIADA	2026-08-25 11:09:02.752454	68	Tipo: FOTO - uploads/fotos/foto_68.jpg	SISTEMA	\N	SISTEMA	56	EVIDENCIA_CARGADA
340	ENVIADA	FIRMADA	2026-08-25 11:09:02.765892	68	Firma digital registrada	Prueba SMTP Real (CC 9876543)	\N	Prueba SMTP Real (CC 9876543)	56	ACTA_FIRMADA
341	\N	GENERADA	2026-08-25 11:17:01.623216	78	Acta de devolucion generada: uploads/pdf/Devolucion_1PG15N3_aaaaaaaaaaaaaaaa.pdf	1	1	1	\N	ACTA_GENERADA
342	GENERADA	ENVIADA	2026-08-25 11:18:04.05139	78	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	66	ACTA_ENVIADA
343	\N	ENVIADA	2026-08-25 11:18:27.77832	78	El usuario abrio el enlace de firma	Angie Maritza Diaz Montaño (CC 131241)	\N	Angie Maritza Diaz Montaño (CC 131241)	66	ACTA_ABIERTA_USUARIO
344	\N	ENVIADA	2026-08-25 11:18:47.145741	78	Tipo: FIRMA - uploads/firmas/firma_78.png	SISTEMA	\N	SISTEMA	66	EVIDENCIA_CARGADA
345	\N	ENVIADA	2026-08-25 11:18:47.150868	78	Tipo: FOTO - uploads/fotos/foto_78.jpg	SISTEMA	\N	SISTEMA	66	EVIDENCIA_CARGADA
346	ENVIADA	FIRMADA	2026-08-25 11:18:47.156206	78	Firma digital registrada	Angie Maritza Diaz Montaño (CC 131241)	\N	Angie Maritza Diaz Montaño (CC 131241)	66	ACTA_FIRMADA
347	\N	FIRMADA	2026-08-25 11:19:06.944565	78	Tipo: PDF_FINAL - uploads/pdf/Devolucion_1PG15N3_aaaaaaaaaaaaaaaa_acta78.pdf	SISTEMA	\N	SISTEMA	66	EVIDENCIA_CARGADA
348	\N	FIRMADA	2026-08-25 11:19:06.948501	78	PDF del documento firmado regenerado: uploads/pdf/Devolucion_1PG15N3_aaaaaaaaaaaaaaaa_acta78.pdf	SISTEMA	\N	SISTEMA	66	PDF_REGENERADO
351	\N	GENERADA	2026-08-25 15:16:50.450953	81	Acta de devolucion generada: uploads/pdf/Devolucion_DBV6RW3_aa.pdf	1	1	1	\N	ACTA_GENERADA
352	FIRMADA	RECHAZADA	2026-08-25 15:43:30.659285	78	Firma mal puesta	admin	1	admin	\N	ACTA_RECHAZADA_ADMIN
353	FIRMADA	APROBADA	2026-08-25 15:43:32.012333	77	Acta aprobada	admin	1	admin	\N	ACTA_APROBADA
354	\N	GENERADA	2026-08-25 16:01:13.12263	82	Acta de entrega generada: uploads/pdf/ActaEntrega_6Q16ZW3_aaaa.pdf	1	1	1	\N	ACTA_GENERADA
355	\N	GENERADA	2026-08-25 16:12:11.804368	83	Acta de entrega generada: uploads/pdf/ActaEntrega_GTGWVZ2_aaa.pdf	1	1	1	\N	ACTA_GENERADA
356	\N	GENERADA	2026-08-25 16:20:30.42585	84	Acta de entrega generada: uploads/pdf/ActaEntrega_HK16ZW3_T.pdf	1	1	1	\N	ACTA_GENERADA
357	\N	GENERADA	2026-08-25 16:23:01.648005	85	Acta de entrega generada: uploads/pdf/ActaEntrega_HK16ZW3_a.pdf	1	1	1	\N	ACTA_GENERADA
358	\N	GENERADA	2026-08-25 16:27:11.908841	86	Acta de entrega generada: uploads/pdf/ActaEntrega_HK16ZW3_aaa.pdf	1	1	1	\N	ACTA_GENERADA
359	\N	GENERADA	2026-08-25 16:35:13.591932	87	Acta de entrega generada: uploads/pdf/ActaEntrega_DBV6RW3_sdfghj.pdf	1	1	1	\N	ACTA_GENERADA
360	\N	GENERADA	2026-08-25 16:39:43.533211	88	Acta de entrega generada: uploads/pdf/ActaEntrega_DBV6RW3_aa.pdf	1	1	1	\N	ACTA_GENERADA
361	GENERADA	ENVIADA	2026-08-25 16:46:58.76422	88	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	67	ACTA_ENVIADA
362	\N	ENVIADA	2026-08-25 16:47:30.276361	88	El usuario abrio el enlace de firma	Paula Alejandra Ricardo Cagua	\N	Paula Alejandra Ricardo Cagua	67	ACTA_ABIERTA_USUARIO
363	\N	ENVIADA	2026-08-25 16:56:41.186789	88	El usuario abrio el enlace de firma	Paula Alejandra Ricardo Cagua	\N	Paula Alejandra Ricardo Cagua	67	ACTA_ABIERTA_USUARIO
364	\N	ENVIADA	2026-08-25 16:56:41.972178	88	El usuario abrio el enlace de firma	Paula Alejandra Ricardo Cagua	\N	Paula Alejandra Ricardo Cagua	67	ACTA_ABIERTA_USUARIO
365	\N	GENERADA	2026-08-26 09:20:36.756399	89	Acta de devolucion generada: uploads/pdf/Devolucion_CL4DQM3_aaa.pdf	1	1	1	\N	ACTA_GENERADA
366	\N	GENERADA	2026-08-26 11:08:27.712933	90	Acta de devolucion generada: uploads/pdf/Devolucion_123_a.pdf	1	1	1	\N	ACTA_GENERADA
367	\N	GENERADA	2026-08-26 11:27:40.198354	91	Acta de devolucion generada: uploads/pdf/Devolucion_123_a.pdf	1	1	1	\N	ACTA_GENERADA
368	\N	GENERADA	2026-08-26 11:28:13.188468	92	Acta de devolucion generada: uploads/pdf/Devolucion_123_aa.pdf	1	1	1	\N	ACTA_GENERADA
369	\N	GENERADA	2026-08-27 11:25:58.515933	96	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_aaa.pdf	1	1	1	\N	ACTA_GENERADA
370	\N	GENERADA	2026-08-27 11:49:22.835587	97	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_aaa.pdf	1	1	1	\N	ACTA_GENERADA
371	GENERADA	ENVIADA	2026-08-27 11:50:14.36402	97	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	68	ACTA_ENVIADA
372	\N	ENVIADA	2026-08-27 11:50:52.934117	97	El usuario abrio el enlace de firma	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	68	ACTA_ABIERTA_USUARIO
373	\N	ENVIADA	2026-08-27 11:51:24.630984	97	Tipo: FIRMA - uploads/firmas/firma_97.png	SISTEMA	\N	SISTEMA	68	EVIDENCIA_CARGADA
374	\N	ENVIADA	2026-08-27 11:51:24.636243	97	Tipo: FOTO - uploads/fotos/foto_97.jpg	SISTEMA	\N	SISTEMA	68	EVIDENCIA_CARGADA
375	ENVIADA	FIRMADA	2026-08-27 11:51:24.638975	97	Firma digital registrada	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	68	ACTA_FIRMADA
376	\N	GENERADA	2026-08-27 12:08:35.481436	98	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_aaaaa.pdf	1	1	1	\N	ACTA_GENERADA
377	GENERADA	ENVIADA	2026-08-27 12:09:11.179049	98	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	69	ACTA_ENVIADA
378	\N	ENVIADA	2026-08-27 12:09:28.448555	98	El usuario abrio el enlace de firma	Juan Jose Hernandez Correa	\N	Juan Jose Hernandez Correa	69	ACTA_ABIERTA_USUARIO
379	\N	ENVIADA	2026-08-27 12:10:03.463746	98	Tipo: FIRMA - uploads/firmas/firma_98.png	SISTEMA	\N	SISTEMA	69	EVIDENCIA_CARGADA
380	\N	ENVIADA	2026-08-27 12:10:03.463746	98	Tipo: FOTO - uploads/fotos/foto_98.jpg	SISTEMA	\N	SISTEMA	69	EVIDENCIA_CARGADA
381	ENVIADA	FIRMADA	2026-08-27 12:10:03.471936	98	Firma digital registrada	Juan Jose Hernandez Correa	\N	Juan Jose Hernandez Correa	69	ACTA_FIRMADA
382	\N	FIRMADA	2026-08-27 12:10:34.74571	98	Tipo: PDF_FINAL - uploads/pdf/FormateoSeguro_123_aaaaa_acta98.pdf	SISTEMA	\N	SISTEMA	69	EVIDENCIA_CARGADA
383	\N	FIRMADA	2026-08-27 12:10:34.748165	98	PDF del documento firmado regenerado: uploads/pdf/FormateoSeguro_123_aaaaa_acta98.pdf	SISTEMA	\N	SISTEMA	69	PDF_REGENERADO
384	\N	GENERADA	2026-08-27 14:49:23.241752	99	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_SinSerial_.pdf	31	31	31	\N	ACTA_GENERADA
385	GENERADA	ENVIADA	2026-08-27 14:50:06.346555	99	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=destino@test.com; correo_enviado=SI	tecnicoev	31	tecnicoev	70	ACTA_ENVIADA
386	FIRMADA	APROBADA	2026-08-27 14:51:03.98205	98	Acta aprobada	adminev	32	adminev	\N	ACTA_APROBADA
387	\N	GENERADA	2026-08-27 16:51:40.03595	100	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_a.pdf	1	1	1	\N	ACTA_GENERADA
388	GENERADA	ENVIADA	2026-08-27 16:53:22.692879	100	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	71	ACTA_ENVIADA
389	\N	GENERADA	2026-08-27 16:57:53.639935	101	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_SN-TEST-FS-2026_PruebaConcepto.pdf	31	31	31	\N	ACTA_GENERADA
390	GENERADA	ENVIADA	2026-08-27 16:58:55.858794	101	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juan.perez@coltefinanciera.com; correo_enviado=SI	tecnicoev	31	tecnicoev	72	ACTA_ENVIADA
391	\N	GENERADA	2026-08-27 17:02:40.968042	102	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_a.pdf	1	1	1	\N	ACTA_GENERADA
392	GENERADA	ENVIADA	2026-08-27 17:03:20.920162	102	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	73	ACTA_ENVIADA
393	\N	GENERADA	2026-08-31 08:02:48.800989	103	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_a.pdf	1	1	1	\N	ACTA_GENERADA
394	GENERADA	ENVIADA	2026-08-31 08:03:23.564514	103	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	74	ACTA_ENVIADA
395	\N	GENERADA	2026-08-31 08:25:13.60914	104	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_aaa.pdf	1	1	1	\N	ACTA_GENERADA
396	\N	GENERADA	2026-08-31 08:46:43.485156	105	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_a.pdf	1	1	1	\N	ACTA_GENERADA
397	GENERADA	ENVIADA	2026-08-31 08:48:17.86721	105	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	83	ACTA_ENVIADA
398	\N	GENERADA	2026-08-31 09:11:24.756836	106	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_aa.pdf	1	1	1	\N	ACTA_GENERADA
399	GENERADA	ENVIADA	2026-08-31 09:11:49.617798	106	Acta enviada para firma; correo_detectado_glpi=; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	84	ACTA_ENVIADA
413	\N	GENERADA	2026-08-31 13:33:38.825369	116	Acta de entrega generada: uploads/pdf/ActaEntrega_123_aa_74001722.pdf	1	1	1	\N	ACTA_GENERADA
414	\N	GENERADA	2026-08-31 13:34:23.658936	117	Acta de devolucion generada: uploads/pdf/Devolucion_123_a_4e761177.pdf	1	1	1	\N	ACTA_GENERADA
415	\N	GENERADA	2026-08-31 13:36:46.588881	118	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_123_aaa_48c28f72.pdf	1	1	1	\N	ACTA_GENERADA
416	\N	GENERADA	2026-09-01 12:31:24.499924	119	Acta de entrega generada: uploads/pdf/ActaEntrega_123_Entrega Nuevo Equipo_e9495d24.pdf ; checklist de entrega: uploads/pdf/Checklist_123_Entrega Nuevo Equipo_ad521b41.pdf	1	1	1	\N	ACTA_GENERADA
417	GENERADA	ENVIADA	2026-09-01 12:34:51.909919	119	Acta enviada para firma; correo_detectado_glpi=daviguzm@coltefinanciera.com.co; correo_utilizado=juanhernandez1122876@gmail.com; correo_enviado=SI	admin	1	admin	87	ACTA_ENVIADA
418	\N	ENVIADA	2026-09-01 12:35:41.669017	119	El usuario abrio el enlace de firma	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	87	ACTA_ABIERTA_USUARIO
419	\N	ENVIADA	2026-09-01 12:36:14.444398	119	El usuario abrio el enlace de firma	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	87	ACTA_ABIERTA_USUARIO
420	\N	ENVIADA	2026-09-01 12:36:15.62158	119	El usuario abrio el enlace de firma	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	87	ACTA_ABIERTA_USUARIO
421	\N	ENVIADA	2026-09-01 12:36:16.946997	119	El usuario abrio el enlace de firma	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	87	ACTA_ABIERTA_USUARIO
422	\N	ENVIADA	2026-09-01 12:36:57.093441	119	Tipo: FIRMA - uploads/firmas/firma_119.png	SISTEMA	\N	SISTEMA	87	EVIDENCIA_CARGADA
423	\N	ENVIADA	2026-09-01 12:36:57.097253	119	Tipo: FOTO - uploads/fotos/foto_119.jpg	SISTEMA	\N	SISTEMA	87	EVIDENCIA_CARGADA
424	ENVIADA	FIRMADA	2026-09-01 12:36:57.097253	119	Firma digital registrada	David Alejandro Guzman Franco	\N	David Alejandro Guzman Franco	87	ACTA_FIRMADA
425	\N	FIRMADA	2026-09-01 12:37:17.937544	119	Tipo: PDF_FINAL - uploads/pdf/ActaEntrega_123_Entrega Nuevo Equipo_eb06eacf_acta119.pdf	SISTEMA	\N	SISTEMA	87	EVIDENCIA_CARGADA
426	\N	FIRMADA	2026-09-01 12:37:17.946561	119	PDF del documento firmado regenerado: uploads/pdf/ActaEntrega_123_Entrega Nuevo Equipo_eb06eacf_acta119.pdf	SISTEMA	\N	SISTEMA	87	PDF_REGENERADO
427	\N	FIRMADA	2026-09-01 12:37:53.883682	119	Tipo: CHECKLIST_FINAL - uploads/pdf/Checklist_123_Entrega Nuevo Equipo_49f719ed_checklist119.pdf	SISTEMA	\N	SISTEMA	87	EVIDENCIA_CARGADA
428	\N	FIRMADA	2026-09-01 12:37:53.885391	119	PDF del checklist de entrega firmado regenerado: uploads/pdf/Checklist_123_Entrega Nuevo Equipo_49f719ed_checklist119.pdf	SISTEMA	\N	SISTEMA	87	PDF_REGENERADO
430	\N	GENERADA	2026-09-02 10:21:53.615589	121	Acta generada	sec011_tc	41	sec011_tc	\N	ACTA_GENERADA
431	\N	GENERADA	2026-09-02 10:25:55.155225	122	Acta generada	sec011_tc	41	sec011_tc	\N	ACTA_GENERADA
432	\N	GENERADA	2026-09-02 10:26:22.591123	123	Acta generada	sec011_tc	41	sec011_tc	\N	ACTA_GENERADA
433	GENERADA	ENVIADA	2026-09-02 10:34:41.574817	123	Acta enviada para firma; correo_detectado_glpi=cperez@test.local; correo_utilizado=cperez@test.local; correo_enviado=SI	sec011_tc	41	sec011_tc	88	ACTA_ENVIADA
434	\N	GENERADA	2026-09-02 11:34:54.524239	124	Acta de entrega generada: uploads/pdf/ActaEntrega_SERPERF002_Entrega equipo oficina_5cf22891.pdf ; checklist de entrega: uploads/pdf/Checklist_SERPERF002_Entrega equipo oficina_decb4d24.pdf	45	45	45	\N	ACTA_GENERADA
435	\N	GENERADA	2026-09-02 11:35:19.518745	125	Acta de devolucion generada: uploads/pdf/Devolucion_SERPERF002_Cambio de equipo_a6f6b57e.pdf	45	45	45	\N	ACTA_GENERADA
436	\N	GENERADA	2026-09-02 11:35:28.713669	126	Acta de devolucion generada: uploads/pdf/Devolucion_SERPERF002_Cambio de equipo_ab4516d4.pdf	45	45	45	\N	ACTA_GENERADA
437	\N	GENERADA	2026-09-02 11:35:44.795078	127	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_SERPERF002_Formateo equipo_44bc6498.pdf	45	45	45	\N	ACTA_GENERADA
438	\N	GENERADA	2026-09-02 11:35:52.054528	128	Acta de formateo seguro generada: uploads/pdf/FormateoSeguro_SERPERF002_Formateo equipo_a0e578bb.pdf	45	45	45	\N	ACTA_GENERADA
439	\N	GENERADA	2026-09-02 11:36:44.485196	129	Acta de entrega generada: uploads/pdf/ActaEntrega_SERPERF002_Entrega equipo oficina_9a9902a9.pdf ; checklist de entrega: uploads/pdf/Checklist_SERPERF002_Entrega equipo oficina_65cba5ac.pdf	45	45	45	\N	ACTA_GENERADA
440	\N	GENERADA	2026-09-02 11:37:15.383837	130	Acta de entrega generada: uploads/pdf/ActaEntrega_SERPERF002_Entrega equipo oficina_996619e7.pdf ; checklist de entrega: uploads/pdf/Checklist_SERPERF002_Entrega equipo oficina_3fff4ef9.pdf	45	45	45	\N	ACTA_GENERADA
441	GENERANDO_DOCUMENTOS	GENERADA	2026-09-02 17:00:12.489103	131	Acta generada en segundo plano: uploads/pdf/ActaEntrega_SN-ASYNC-001_PRUEBA FASE 1 ASYNC_99dbb94a.pdf ; checklist: uploads/pdf/Checklist_SN-ASYNC-001_PRUEBA FASE 1 ASYNC_ffab2af1.pdf	47	47	47	\N	ACTA_GENERADA
442	GENERANDO_DOCUMENTOS	GENERADA	2026-09-02 17:01:57.290064	132	Acta generada en segundo plano: uploads/pdf/Devolucion_SN-DEV-002_Devolucion por cambio_d4523d28.pdf	47	47	47	\N	ACTA_GENERADA
443	GENERANDO_DOCUMENTOS	GENERADA	2026-09-03 07:58:18.381011	133	Acta generada en segundo plano: uploads/pdf/FormateoSeguro_123_Equipo lento_e5db8549.pdf	1	1	1	\N	ACTA_GENERADA
444	GENERANDO_DOCUMENTOS	GENERADA	2026-09-03 08:22:04.40843	134	Acta generada en segundo plano: uploads/pdf/FormateoSeguro_SN-FORM-FIX-1_FORMATEO CORRECCION USUARIO_b9929bf1.pdf	49	49	49	\N	ACTA_GENERADA
445	GENERANDO_DOCUMENTOS	GENERADA	2026-09-03 08:22:26.369739	135	Acta generada en segundo plano: uploads/pdf/ActaEntrega_SN-ENT-FIX_ENTREGA CHECK_286e7e00.pdf ; checklist: uploads/pdf/Checklist_SN-ENT-FIX_ENTREGA CHECK_1d489cbf.pdf	49	49	49	\N	ACTA_GENERADA
446	GENERADA	ENVIADA	2026-09-03 08:24:29.197486	134	Acta enviada para firma; correo_detectado_glpi=maria.usuario@test.local; correo_utilizado=maria.usuario@test.local; correo_enviado=SI	form_41701453	49	form_41701453	89	ACTA_ENVIADA
\.


--
-- Data for Name: asignacion; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.asignacion (id_asignacion, id_dispositivo, cedula_usuario, nombre_usuario, correo_usuario, fecha_asignacion, fecha_entrega, fecha_devolucion) FROM stdin;
\.


--
-- Data for Name: auditoria_sistema; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.auditoria_sistema (id_auditoria, detalle, entidad, entidad_id, fecha_evento, ip_direccion, recurso, tipo_evento, usuario_id, usuario_nombre) FROM stdin;
18	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 09:56:30.645315	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	8	juantec
19	Cierre de sesion	AUTENTICACION	\N	2026-08-24 10:12:12.61717	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	8	juantec
20	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-24 10:12:17.948536	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	admin
21	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 10:12:21.295035	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
22	Token inexistente o alterado	FIRMA_TOKEN	xxx	2026-08-24 10:49:25.280337	0:0:0:0:0:0:0:1	/firma/xxx	TOKEN_INVALIDO	\N	PORTAL_FIRMA
23	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-24 10:54:18.421987	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	admin
24	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 10:57:41.248565	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	20	otptest
26	Codigo OTP emitido para firma_token id=48	FIRMA_TOKEN	e452a4b9-a37d-4a86-a1c9-ac48df09bf55	2026-08-24 11:00:33.036416	0:0:0:0:0:0:0:1	/firma/e452a4b9-a37d-4a86-a1c9-ac48df09bf55/otp	OTP_GENERADO	\N	PORTAL_FIRMA
27	Fallo el envio de correo OTP a c***@example.com	FIRMA_TOKEN	e452a4b9-a37d-4a86-a1c9-ac48df09bf55	2026-08-24 11:00:33.206708	0:0:0:0:0:0:0:1	/firma/e452a4b9-a37d-4a86-a1c9-ac48df09bf55/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
28	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 11:05:38.077341	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	20	otptest
29	Codigo OTP emitido para firma_token id=49	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:39.043793	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp	OTP_GENERADO	\N	PORTAL_FIRMA
30	Correo OTP enviado a c***@example.com	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:39.192841	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
31	Codigo OTP incorrecto, intento 1/5 (c***@example.com)	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:40.900434	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
32	Codigo OTP incorrecto, intento 1/5 (c***@example.com)	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:41.487735	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
33	Codigo OTP incorrecto, intento 1/5 (c***@example.com)	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:42.049936	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
34	Codigo OTP incorrecto, intento 1/5 (c***@example.com)	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:42.437575	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
35	Codigo OTP incorrecto, intento 1/5 (c***@example.com)	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:42.795037	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
36	Codigo OTP incorrecto, intento 1/5 (c***@example.com)	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:43.233295	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
37	Codigo OTP incorrecto, intento 1/5 (c***@example.com)	FIRMA_TOKEN	ce571653-8a51-4024-be78-2f55a5c5d621	2026-08-24 11:05:43.744527	0:0:0:0:0:0:0:1	/firma/ce571653-8a51-4024-be78-2f55a5c5d621/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
38	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 11:07:06.41991	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	20	otptest
39	Codigo OTP emitido para firma_token id=50	FIRMA_TOKEN	5053f9f6-df93-4c79-9a06-72bfeb8cc977	2026-08-24 11:07:07.597297	0:0:0:0:0:0:0:1	/firma/5053f9f6-df93-4c79-9a06-72bfeb8cc977/otp	OTP_GENERADO	\N	PORTAL_FIRMA
40	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	5053f9f6-df93-4c79-9a06-72bfeb8cc977	2026-08-24 11:07:07.615206	0:0:0:0:0:0:0:1	/firma/5053f9f6-df93-4c79-9a06-72bfeb8cc977/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
41	Codigo OTP validado, sesion 68dc9170... (o***@example.com)	FIRMA_TOKEN	5053f9f6-df93-4c79-9a06-72bfeb8cc977	2026-08-24 11:07:08.572608	0:0:0:0:0:0:0:1	/firma/5053f9f6-df93-4c79-9a06-72bfeb8cc977/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
42	Codigo OTP validado, sesion 4e6329a1... (o***@example.com)	FIRMA_TOKEN	5053f9f6-df93-4c79-9a06-72bfeb8cc977	2026-08-24 11:07:09.321579	0:0:0:0:0:0:0:1	/firma/5053f9f6-df93-4c79-9a06-72bfeb8cc977/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
43	Codigo OTP emitido para firma_token id=51	FIRMA_TOKEN	4c7e3f26-4102-4060-bce9-027f9e70346a	2026-08-24 11:07:10.895207	0:0:0:0:0:0:0:1	/firma/4c7e3f26-4102-4060-bce9-027f9e70346a/otp	OTP_GENERADO	\N	PORTAL_FIRMA
44	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	4c7e3f26-4102-4060-bce9-027f9e70346a	2026-08-24 11:07:10.917194	0:0:0:0:0:0:0:1	/firma/4c7e3f26-4102-4060-bce9-027f9e70346a/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
45	Codigo OTP vencido (o***@example.com)	FIRMA_TOKEN	4c7e3f26-4102-4060-bce9-027f9e70346a	2026-08-24 11:07:12.145147	0:0:0:0:0:0:0:1	/firma/4c7e3f26-4102-4060-bce9-027f9e70346a/otp/validar	OTP_EXPIRADO	\N	PORTAL_FIRMA
46	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 11:09:59.602547	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	20	otptest
47	Codigo OTP emitido para firma_token id=52	FIRMA_TOKEN	b6f89bb3-e110-40c0-bdf2-ca6d238ddda8	2026-08-24 11:10:00.616215	0:0:0:0:0:0:0:1	/firma/b6f89bb3-e110-40c0-bdf2-ca6d238ddda8/otp	OTP_GENERADO	\N	PORTAL_FIRMA
48	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	b6f89bb3-e110-40c0-bdf2-ca6d238ddda8	2026-08-24 11:10:00.75569	0:0:0:0:0:0:0:1	/firma/b6f89bb3-e110-40c0-bdf2-ca6d238ddda8/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
49	Codigo OTP validado, sesion ed52b75f... (o***@example.com)	FIRMA_TOKEN	b6f89bb3-e110-40c0-bdf2-ca6d238ddda8	2026-08-24 11:10:01.552213	0:0:0:0:0:0:0:1	/firma/b6f89bb3-e110-40c0-bdf2-ca6d238ddda8/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
50	Token ya utilizado	FIRMA_TOKEN	b6f89bb3-e110-40c0-bdf2-ca6d238ddda8	2026-08-24 11:10:02.576368	0:0:0:0:0:0:0:1	/firma/b6f89bb3-e110-40c0-bdf2-ca6d238ddda8	TOKEN_INVALIDO	\N	PORTAL_FIRMA
51	Codigo OTP emitido para firma_token id=53	FIRMA_TOKEN	a6a13c6e-df92-44ab-b5af-eca143f2539d	2026-08-24 11:10:03.428597	0:0:0:0:0:0:0:1	/firma/a6a13c6e-df92-44ab-b5af-eca143f2539d/otp	OTP_GENERADO	\N	PORTAL_FIRMA
52	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	a6a13c6e-df92-44ab-b5af-eca143f2539d	2026-08-24 11:10:03.441897	0:0:0:0:0:0:0:1	/firma/a6a13c6e-df92-44ab-b5af-eca143f2539d/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
53	Codigo OTP vencido (o***@example.com)	FIRMA_TOKEN	a6a13c6e-df92-44ab-b5af-eca143f2539d	2026-08-24 11:10:04.753289	0:0:0:0:0:0:0:1	/firma/a6a13c6e-df92-44ab-b5af-eca143f2539d/otp/validar	OTP_EXPIRADO	\N	PORTAL_FIRMA
54	Token inexistente o alterado	FIRMA_TOKEN	none	2026-08-24 11:15:25.182798	127.0.0.1	/firma/none	TOKEN_INVALIDO	\N	PORTAL_FIRMA
55	Token inexistente o alterado	FIRMA_TOKEN	x	2026-08-24 11:17:19.437592	127.0.0.1	/firma/x	TOKEN_INVALIDO	\N	PORTAL_FIRMA
56	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 11:18:20.765216	127.0.0.1	/auth/login	LOGIN_EXITOSO	20	otptest
57	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 11:19:37.425249	127.0.0.1	/auth/login	LOGIN_EXITOSO	20	otptest
58	Evidencia FOTO (foto_50.jpg) visualizada	ACTA	50	2026-08-24 11:50:53.957269	0:0:0:0:0:0:0:1	/actas/50/foto	EVIDENCIA_VISTA	1	admin
59	PDF del acta visualizado/descargado	ACTA	50	2026-08-24 11:50:53.957269	0:0:0:0:0:0:0:1	/actas/50/pdf	DOCUMENTO_VISTO	1	admin
60	Evidencia FIRMA (firma_50.png) visualizada	ACTA	50	2026-08-24 11:50:53.957718	0:0:0:0:0:0:0:1	/actas/50/firma	EVIDENCIA_VISTA	1	admin
61	Codigo OTP emitido para firma_token id=42	FIRMA_TOKEN	6160669f-edd3-4b41-9543-71bb9c927380	2026-08-24 11:51:16.216505	0:0:0:0:0:0:0:1	/firma/6160669f-edd3-4b41-9543-71bb9c927380/otp	OTP_GENERADO	\N	PORTAL_FIRMA
62	Correo OTP enviado a r***@gmail.com	FIRMA_TOKEN	6160669f-edd3-4b41-9543-71bb9c927380	2026-08-24 11:51:16.487457	0:0:0:0:0:0:0:1	/firma/6160669f-edd3-4b41-9543-71bb9c927380/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
63	Codigo OTP emitido para firma_token id=42	FIRMA_TOKEN	6160669f-edd3-4b41-9543-71bb9c927380	2026-08-24 11:51:42.156827	0:0:0:0:0:0:0:1	/firma/6160669f-edd3-4b41-9543-71bb9c927380/otp	OTP_GENERADO	\N	PORTAL_FIRMA
64	Correo OTP enviado a r***@gmail.com	FIRMA_TOKEN	6160669f-edd3-4b41-9543-71bb9c927380	2026-08-24 11:51:42.190876	0:0:0:0:0:0:0:1	/firma/6160669f-edd3-4b41-9543-71bb9c927380/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
65	Codigo OTP reenviado (r***@gmail.com)	FIRMA_TOKEN	6160669f-edd3-4b41-9543-71bb9c927380	2026-08-24 11:51:42.190876	0:0:0:0:0:0:0:1	/firma/6160669f-edd3-4b41-9543-71bb9c927380/otp/reenviar	OTP_REENVIADO	\N	PORTAL_FIRMA
66	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 11:56:20.68272	127.0.0.1	/auth/login	LOGIN_EXITOSO	20	otptest
67	Codigo OTP emitido para firma_token id=54	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:21.918227	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp	OTP_GENERADO	\N	PORTAL_FIRMA
68	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:21.950431	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
69	Codigo OTP incorrecto, intento 1/5 (o***@example.com)	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:23.028404	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
70	Codigo OTP incorrecto, intento 2/5 (o***@example.com)	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:23.299083	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
71	Codigo OTP incorrecto, intento 3/5 (o***@example.com)	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:23.596997	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
72	Codigo OTP incorrecto, intento 4/5 (o***@example.com)	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:23.886041	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
73	Codigo OTP incorrecto, intento 5/5 (o***@example.com)	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:24.182068	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
74	Codigo OTP bloqueado por alcanzar el maximo de intentos (o***@example.com)	FIRMA_TOKEN	48fa106b-8f65-47f7-98e9-78274a9b3004	2026-08-24 11:56:24.198021	127.0.0.1	/firma/48fa106b-8f65-47f7-98e9-78274a9b3004/otp/validar	OTP_BLOQUEADO	\N	PORTAL_FIRMA
75	Codigo OTP emitido para firma_token id=55	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:25.706013	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_GENERADO	\N	PORTAL_FIRMA
76	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:25.753619	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
77	Codigo OTP emitido para firma_token id=55	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:29.91907	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_GENERADO	\N	PORTAL_FIRMA
78	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:29.956196	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
83	Codigo OTP emitido para firma_token id=55	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:36.519158	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_GENERADO	\N	PORTAL_FIRMA
79	Codigo OTP reenviado (o***@example.com)	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:29.956196	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp/reenviar	OTP_REENVIADO	\N	PORTAL_FIRMA
80	Codigo OTP emitido para firma_token id=55	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:33.235889	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_GENERADO	\N	PORTAL_FIRMA
81	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:33.269334	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
82	Codigo OTP reenviado (o***@example.com)	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:33.270401	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp/reenviar	OTP_REENVIADO	\N	PORTAL_FIRMA
84	Correo OTP enviado a o***@example.com	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:36.535156	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
85	Codigo OTP reenviado (o***@example.com)	FIRMA_TOKEN	62335d07-f28d-4a9e-bc74-029c6a71e8aa	2026-08-24 11:56:36.544105	127.0.0.1	/firma/62335d07-f28d-4a9e-bc74-029c6a71e8aa/otp/reenviar	OTP_REENVIADO	\N	PORTAL_FIRMA
86	Token inexistente o alterado	FIRMA_TOKEN	x	2026-08-24 15:59:25.239473	127.0.0.1	/firma/x	TOKEN_INVALIDO	\N	PORTAL_FIRMA
87	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 16:00:08.14302	127.0.0.1	/auth/login	LOGIN_EXITOSO	20	otptest
88	Codigo OTP emitido para firma_token id=56	FIRMA_TOKEN	df3b79b5-b7e1-4ceb-9946-05820d7fe899	2026-08-24 16:00:09.675714	127.0.0.1	/firma/df3b79b5-b7e1-4ceb-9946-05820d7fe899/otp	OTP_GENERADO	\N	PORTAL_FIRMA
89	Correo OTP enviado a r***@gmail.com	FIRMA_TOKEN	df3b79b5-b7e1-4ceb-9946-05820d7fe899	2026-08-24 16:00:11.874919	127.0.0.1	/firma/df3b79b5-b7e1-4ceb-9946-05820d7fe899/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
90	Codigo OTP validado, sesion 2de5364f... (r***@gmail.com)	FIRMA_TOKEN	df3b79b5-b7e1-4ceb-9946-05820d7fe899	2026-08-24 16:04:38.279032	0:0:0:0:0:0:0:1	/firma/df3b79b5-b7e1-4ceb-9946-05820d7fe899/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
91	PDF del acta visualizado/descargado	ACTA	69	2026-08-24 16:24:56.363914	0:0:0:0:0:0:0:1	/actas/69/pdf	DOCUMENTO_VISTO	1	admin
92	Codigo OTP emitido para firma_token id=57	FIRMA_TOKEN	68987659-3693-4136-a098-8a93bd1133fe	2026-08-24 16:29:09.067519	0:0:0:0:0:0:0:1	/firma/68987659-3693-4136-a098-8a93bd1133fe/otp	OTP_GENERADO	\N	PORTAL_FIRMA
93	Fallo el envio de correo OTP a J***@coltefinanciera.com.co	FIRMA_TOKEN	68987659-3693-4136-a098-8a93bd1133fe	2026-08-24 16:29:10.377773	0:0:0:0:0:0:0:1	/firma/68987659-3693-4136-a098-8a93bd1133fe/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
94	PDF del acta visualizado/descargado	ACTA	69	2026-08-24 16:29:10.473386	0:0:0:0:0:0:0:1	/actas/69/pdf	DOCUMENTO_VISTO	1	admin
95	Codigo OTP emitido para firma_token id=58	FIRMA_TOKEN	73105897-930d-431c-ad83-915b89ee435b	2026-08-24 16:32:04.570925	0:0:0:0:0:0:0:1	/firma/73105897-930d-431c-ad83-915b89ee435b/otp	OTP_GENERADO	\N	PORTAL_FIRMA
96	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	73105897-930d-431c-ad83-915b89ee435b	2026-08-24 16:32:05.733207	0:0:0:0:0:0:0:1	/firma/73105897-930d-431c-ad83-915b89ee435b/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
97	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 16:50:28.212929	127.0.0.1	/auth/login	LOGIN_EXITOSO	20	otptest
98	Codigo OTP emitido para firma_token id=59	FIRMA_TOKEN	15f083ab-7337-419a-b0e4-6b125a9f61c4	2026-08-24 16:50:30.414132	127.0.0.1	/firma/15f083ab-7337-419a-b0e4-6b125a9f61c4/otp	OTP_GENERADO	\N	PORTAL_FIRMA
99	Correo OTP enviado a r***@gmail.com	FIRMA_TOKEN	15f083ab-7337-419a-b0e4-6b125a9f61c4	2026-08-24 16:50:33.287867	127.0.0.1	/firma/15f083ab-7337-419a-b0e4-6b125a9f61c4/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
100	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 16:52:04.732727	127.0.0.1	/auth/login	LOGIN_EXITOSO	20	otptest
101	Codigo OTP emitido para firma_token id=60	FIRMA_TOKEN	457f08b4-3bb5-47ff-86d5-c8feb71de937	2026-08-24 16:52:05.815047	127.0.0.1	/firma/457f08b4-3bb5-47ff-86d5-c8feb71de937/otp	OTP_GENERADO	\N	PORTAL_FIRMA
102	Fallo el envio de correo OTP a c***@ejemplo.com	FIRMA_TOKEN	457f08b4-3bb5-47ff-86d5-c8feb71de937	2026-08-24 16:52:05.834623	127.0.0.1	/firma/457f08b4-3bb5-47ff-86d5-c8feb71de937/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
103	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-24 16:56:59.027343	127.0.0.1	/auth/login	LOGIN_EXITOSO	20	otptest
104	Codigo OTP emitido para firma_token id=61	FIRMA_TOKEN	601dba94-e669-44fc-a6b2-0e4b2cb478bb	2026-08-24 16:57:00.304432	127.0.0.1	/firma/601dba94-e669-44fc-a6b2-0e4b2cb478bb/otp	OTP_GENERADO	\N	PORTAL_FIRMA
105	Correo OTP enviado a r***@gmail.com	FIRMA_TOKEN	601dba94-e669-44fc-a6b2-0e4b2cb478bb	2026-08-24 16:57:02.673684	127.0.0.1	/firma/601dba94-e669-44fc-a6b2-0e4b2cb478bb/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
106	PDF del acta visualizado/descargado	ACTA	74	2026-08-25 08:29:22.596219	0:0:0:0:0:0:0:1	/actas/74/pdf	DOCUMENTO_VISTO	1	admin
107	Codigo OTP emitido para firma_token id=62	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:30:24.315186	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d/otp	OTP_GENERADO	\N	PORTAL_FIRMA
108	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:30:27.839432	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
109	PDF del acta visualizado/descargado	ACTA	74	2026-08-25 08:30:27.946619	0:0:0:0:0:0:0:1	/actas/74/pdf	DOCUMENTO_VISTO	1	admin
110	Codigo OTP validado, sesion 84e52740... (j***@gmail.com)	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:32:34.595795	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
111	El firmante visualizo el PDF del acta	ACTA	74	2026-08-25 08:32:34.666504	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
112	Token ya utilizado	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:34:06.769909	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d	TOKEN_INVALIDO	\N	PORTAL_FIRMA
113	Token ya utilizado	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:34:06.79335	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d	TOKEN_INVALIDO	\N	PORTAL_FIRMA
114	Token ya utilizado	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:34:14.068636	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d	TOKEN_INVALIDO	\N	PORTAL_FIRMA
115	Token ya utilizado	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:34:31.651683	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d	TOKEN_INVALIDO	\N	PORTAL_FIRMA
116	Token ya utilizado	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 08:34:32.165149	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d	TOKEN_INVALIDO	\N	PORTAL_FIRMA
117	Token ya utilizado	FIRMA_TOKEN	73105897-930d-431c-ad83-915b89ee435b	2026-08-25 08:48:38.802125	127.0.0.1	/firma/73105897-930d-431c-ad83-915b89ee435b	TOKEN_INVALIDO	\N	PORTAL_FIRMA
118	Token ya utilizado	FIRMA_TOKEN	795cd5c5-f4a7-4d84-b371-b39800684e3d	2026-08-25 09:25:47.563642	0:0:0:0:0:0:0:1	/firma/795cd5c5-f4a7-4d84-b371-b39800684e3d	TOKEN_INVALIDO	\N	PORTAL_FIRMA
119	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 09:59:30.065171	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
120	Codigo OTP emitido para firma_token id=63	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 09:59:53.12225	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5/otp	OTP_GENERADO	\N	PORTAL_FIRMA
121	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 09:59:56.076429	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
122	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 09:59:56.14789	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
123	Codigo OTP incorrecto, intento 1/5 (j***@gmail.com)	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:00:46.901869	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5/otp/validar	OTP_INVALIDO	\N	PORTAL_FIRMA
127	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:01:53.368356	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
129	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:01:53.422844	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
131	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:02:01.732836	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
133	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:02:01.844625	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
136	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:02:06.387013	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
140	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:02:06.868281	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
124	Codigo OTP validado, sesion 105890cc... (j***@gmail.com)	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:01:04.186373	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
138	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:02:06.746661	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
125	El firmante visualizo el PDF del acta	ACTA	75	2026-08-25 10:01:04.239225	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
126	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:01:53.316469	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
128	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:01:53.417992	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
130	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:01:53.425211	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
132	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:02:01.843212	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
134	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:02:06.324496	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
135	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:02:06.387013	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
137	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:02:06.39086	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
139	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:02:06.868281	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
141	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:02:06.868281	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
142	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:08:45.473779	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
143	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:08:45.613972	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
144	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:08:45.620345	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
145	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:08:45.636948	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
146	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:08:51.910255	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
147	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:08:52.013434	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
148	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:08:52.015518	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
149	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:08:52.020135	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
150	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:09:01.345813	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
151	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:09:01.490178	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
152	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:09:01.490178	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
153	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:09:01.490178	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
154	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:09:01.762505	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
156	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:09:01.845389	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
155	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:09:01.845389	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
157	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:09:01.850445	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
158	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:09:03.066246	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
159	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:09:03.182122	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
160	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:09:03.182122	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
161	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:09:03.190662	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
162	Token ya utilizado	FIRMA_TOKEN	32fddcea-0f19-4240-b1a6-3b24f1824b56	2026-08-25 10:11:04.699197	127.0.0.1	/firma/32fddcea-0f19-4240-b1a6-3b24f1824b56	TOKEN_INVALIDO	\N	PORTAL_FIRMA
163	Token ya utilizado	FIRMA_TOKEN	32fddcea-0f19-4240-b1a6-3b24f1824b56	2026-08-25 10:11:04.859425	127.0.0.1	/firma/32fddcea-0f19-4240-b1a6-3b24f1824b56	TOKEN_INVALIDO	\N	PORTAL_FIRMA
164	Token ya utilizado	FIRMA_TOKEN	32fddcea-0f19-4240-b1a6-3b24f1824b56	2026-08-25 10:11:05.018658	127.0.0.1	/firma/32fddcea-0f19-4240-b1a6-3b24f1824b56	TOKEN_INVALIDO	\N	PORTAL_FIRMA
165	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:12:02.793509	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
166	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:12:02.893204	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
167	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:12:02.896206	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
168	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:12:02.89773	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
169	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:12:11.714611	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
171	PDF del acta visualizado/descargado	ACTA	75	2026-08-25 10:12:11.809449	0:0:0:0:0:0:0:1	/actas/75/pdf	DOCUMENTO_VISTO	1	admin
170	Evidencia FIRMA (firma_75.png) visualizada	ACTA	75	2026-08-25 10:12:11.809449	0:0:0:0:0:0:0:1	/actas/75/firma	EVIDENCIA_VISTA	1	admin
172	Evidencia FOTO (foto_75.jpg) visualizada	ACTA	75	2026-08-25 10:12:11.812967	0:0:0:0:0:0:0:1	/actas/75/foto	EVIDENCIA_VISTA	1	admin
173	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 10:15:25.811095	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
174	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:16:07.957093	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
175	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:16:13.30182	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
176	Token ya utilizado	FIRMA_TOKEN	d81882db-c831-4813-98f9-592f94df38f5	2026-08-25 10:16:13.718659	0:0:0:0:0:0:0:1	/firma/d81882db-c831-4813-98f9-592f94df38f5	TOKEN_INVALIDO	\N	PORTAL_FIRMA
177	PDF del acta visualizado/descargado	ACTA	76	2026-08-25 10:16:42.07043	0:0:0:0:0:0:0:1	/actas/76/pdf	DOCUMENTO_VISTO	1	admin
178	Codigo OTP emitido para firma_token id=64	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:17:08.057652	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b/otp	OTP_GENERADO	\N	PORTAL_FIRMA
179	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:17:09.897984	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
186	Evidencia FIRMA (firma_76.png) visualizada	ACTA	76	2026-08-25 10:17:54.059434	0:0:0:0:0:0:0:1	/actas/76/firma	EVIDENCIA_VISTA	1	admin
189	Evidencia FIRMA (firma_76.png) visualizada	ACTA	76	2026-08-25 10:18:03.247052	0:0:0:0:0:0:0:1	/actas/76/firma	EVIDENCIA_VISTA	1	admin
191	Token ya utilizado	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:18:09.407502	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b	TOKEN_INVALIDO	\N	PORTAL_FIRMA
192	Evidencia FIRMA (firma_76.png) visualizada	ACTA	76	2026-08-25 10:18:09.520669	0:0:0:0:0:0:0:1	/actas/76/firma	EVIDENCIA_VISTA	1	admin
195	Token ya utilizado	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:18:10.676863	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b	TOKEN_INVALIDO	\N	PORTAL_FIRMA
196	Evidencia FIRMA (firma_76.png) visualizada	ACTA	76	2026-08-25 10:18:10.725375	0:0:0:0:0:0:0:1	/actas/76/firma	EVIDENCIA_VISTA	1	admin
200	Codigo OTP emitido para firma_token id=65	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:22:44.33116	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796/otp	OTP_GENERADO	\N	PORTAL_FIRMA
201	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:22:46.533726	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
202	PDF del acta visualizado/descargado	ACTA	77	2026-08-25 10:22:46.580353	0:0:0:0:0:0:0:1	/actas/77/pdf	DOCUMENTO_VISTO	1	admin
203	Codigo OTP validado, sesion 35963bf5... (j***@gmail.com)	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:23:09.529374	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
208	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:23:39.736189	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
210	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:29:12.50465	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
180	PDF del acta visualizado/descargado	ACTA	76	2026-08-25 10:17:09.944545	0:0:0:0:0:0:0:1	/actas/76/pdf	DOCUMENTO_VISTO	1	admin
182	El firmante visualizo el PDF del acta	ACTA	76	2026-08-25 10:17:31.976901	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
204	El firmante visualizo el PDF del acta	ACTA	77	2026-08-25 10:23:09.591801	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
181	Codigo OTP validado, sesion ea6e4335... (j***@gmail.com)	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:17:31.935818	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
185	PDF del acta visualizado/descargado	ACTA	76	2026-08-25 10:17:54.059434	0:0:0:0:0:0:0:1	/actas/76/pdf	DOCUMENTO_VISTO	1	admin
190	Evidencia FOTO (foto_76.jpg) visualizada	ACTA	76	2026-08-25 10:18:03.25012	0:0:0:0:0:0:0:1	/actas/76/foto	EVIDENCIA_VISTA	1	admin
194	Evidencia FOTO (foto_76.jpg) visualizada	ACTA	76	2026-08-25 10:18:09.529234	0:0:0:0:0:0:0:1	/actas/76/foto	EVIDENCIA_VISTA	1	admin
197	PDF del acta visualizado/descargado	ACTA	76	2026-08-25 10:18:10.727002	0:0:0:0:0:0:0:1	/actas/76/pdf	DOCUMENTO_VISTO	1	admin
207	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:23:35.461341	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
183	Token ya utilizado	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:17:53.982491	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b	TOKEN_INVALIDO	\N	PORTAL_FIRMA
184	Token ya utilizado	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:17:53.994154	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b	TOKEN_INVALIDO	\N	PORTAL_FIRMA
188	Token ya utilizado	FIRMA_TOKEN	f258381a-a584-4361-aa26-a2df6d38528b	2026-08-25 10:18:03.108158	0:0:0:0:0:0:0:1	/firma/f258381a-a584-4361-aa26-a2df6d38528b	TOKEN_INVALIDO	\N	PORTAL_FIRMA
193	PDF del acta visualizado/descargado	ACTA	76	2026-08-25 10:18:09.520669	0:0:0:0:0:0:0:1	/actas/76/pdf	DOCUMENTO_VISTO	1	admin
198	Evidencia FOTO (foto_76.jpg) visualizada	ACTA	76	2026-08-25 10:18:10.731153	0:0:0:0:0:0:0:1	/actas/76/foto	EVIDENCIA_VISTA	1	admin
187	Evidencia FOTO (foto_76.jpg) visualizada	ACTA	76	2026-08-25 10:17:54.062455	0:0:0:0:0:0:0:1	/actas/76/foto	EVIDENCIA_VISTA	1	admin
199	PDF del acta visualizado/descargado	ACTA	77	2026-08-25 10:22:35.874502	0:0:0:0:0:0:0:1	/actas/77/pdf	DOCUMENTO_VISTO	1	admin
205	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:23:30.662644	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
206	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:23:30.67088	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
209	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:23:40.617015	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
211	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:29:13.441921	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
212	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:48:11.521595	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
213	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:48:12.103714	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
214	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 10:48:13.132018	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
215	Codigo OTP validado, sesion 811f3a08... (J***@coltefinanciera.com.co)	FIRMA_TOKEN	68987659-3693-4136-a098-8a93bd1133fe	2026-08-25 10:52:03.018049	0:0:0:0:0:0:0:1	/firma/68987659-3693-4136-a098-8a93bd1133fe/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
216	Codigo OTP validado, sesion 09477448... (r***@gmail.com)	FIRMA_TOKEN	df3b79b5-b7e1-4ceb-9946-05820d7fe899	2026-08-25 10:57:41.02447	0:0:0:0:0:0:0:1	/firma/df3b79b5-b7e1-4ceb-9946-05820d7fe899/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
217	Codigo OTP validado, sesion 6e42373c... (r***@gmail.com)	FIRMA_TOKEN	df3b79b5-b7e1-4ceb-9946-05820d7fe899	2026-08-25 11:09:00.77801	0:0:0:0:0:0:0:1	/firma/df3b79b5-b7e1-4ceb-9946-05820d7fe899/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
218	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 11:09:03.151235	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
219	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 11:13:24.532936	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
220	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 11:16:45.334853	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
221	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 11:16:48.408673	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
222	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 11:16:49.063349	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
223	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 11:16:59.864528	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
224	Token ya utilizado	FIRMA_TOKEN	0474363b-7d54-47db-9061-f031622ff796	2026-08-25 11:17:00.422156	0:0:0:0:0:0:0:1	/firma/0474363b-7d54-47db-9061-f031622ff796	TOKEN_INVALIDO	\N	PORTAL_FIRMA
225	PDF del acta visualizado/descargado	ACTA	78	2026-08-25 11:17:05.02349	0:0:0:0:0:0:0:1	/actas/78/pdf	DOCUMENTO_VISTO	1	admin
226	Codigo OTP emitido para firma_token id=66	FIRMA_TOKEN	46e03189-1a76-4dfa-a39b-bef2af9f58c3	2026-08-25 11:18:01.515395	0:0:0:0:0:0:0:1	/firma/46e03189-1a76-4dfa-a39b-bef2af9f58c3/otp	OTP_GENERADO	\N	PORTAL_FIRMA
227	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	46e03189-1a76-4dfa-a39b-bef2af9f58c3	2026-08-25 11:18:04.05139	0:0:0:0:0:0:0:1	/firma/46e03189-1a76-4dfa-a39b-bef2af9f58c3/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
228	PDF del acta visualizado/descargado	ACTA	78	2026-08-25 11:18:04.121893	0:0:0:0:0:0:0:1	/actas/78/pdf	DOCUMENTO_VISTO	1	admin
229	Codigo OTP validado, sesion 67ef571d... (j***@gmail.com)	FIRMA_TOKEN	46e03189-1a76-4dfa-a39b-bef2af9f58c3	2026-08-25 11:18:27.732682	0:0:0:0:0:0:0:1	/firma/46e03189-1a76-4dfa-a39b-bef2af9f58c3/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
230	El firmante visualizo el PDF del acta	ACTA	78	2026-08-25 11:18:27.820431	0:0:0:0:0:0:0:1	/firma/46e03189-1a76-4dfa-a39b-bef2af9f58c3/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
231	Token ya utilizado	FIRMA_TOKEN	46e03189-1a76-4dfa-a39b-bef2af9f58c3	2026-08-25 11:18:59.504689	0:0:0:0:0:0:0:1	/firma/46e03189-1a76-4dfa-a39b-bef2af9f58c3	TOKEN_INVALIDO	\N	PORTAL_FIRMA
232	PDF del acta visualizado/descargado	ACTA	78	2026-08-25 11:19:09.295659	0:0:0:0:0:0:0:1	/actas/78/pdf	DOCUMENTO_VISTO	1	admin
233	Evidencia FIRMA (firma_78.png) visualizada	ACTA	78	2026-08-25 11:19:09.29953	0:0:0:0:0:0:0:1	/actas/78/firma	EVIDENCIA_VISTA	1	admin
234	Evidencia FOTO (foto_78.jpg) visualizada	ACTA	78	2026-08-25 11:19:09.3037	0:0:0:0:0:0:0:1	/actas/78/foto	EVIDENCIA_VISTA	1	admin
245	Evidencia FOTO (foto_78.jpg) visualizada	ACTA	78	2026-08-25 15:05:07.104049	0:0:0:0:0:0:0:1	/actas/78/foto	EVIDENCIA_VISTA	1	admin
244	Evidencia FIRMA (firma_78.png) visualizada	ACTA	78	2026-08-25 15:05:07.104049	0:0:0:0:0:0:0:1	/actas/78/firma	EVIDENCIA_VISTA	1	admin
246	PDF del acta visualizado/descargado	ACTA	78	2026-08-25 15:05:07.114493	0:0:0:0:0:0:0:1	/actas/78/pdf	DOCUMENTO_VISTO	1	admin
247	Firma permanente del tecnico registrada (1)	USUARIO	1	2026-08-25 15:05:45.315524	0:0:0:0:0:0:0:1	uploads/firmas_tecnico/firma_tecnico_1.png	FIRMA_TECNICO_REGISTRADA	1	admin
248	PDF del acta visualizado/descargado	ACTA	81	2026-08-25 15:16:53.116999	0:0:0:0:0:0:0:1	/actas/81/pdf	DOCUMENTO_VISTO	1	admin
249	PDF del acta visualizado/descargado	ACTA	81	2026-08-25 15:17:47.685585	0:0:0:0:0:0:0:1	/actas/81/pdf	DOCUMENTO_VISTO	1	admin
254	Firma permanente del tecnico eliminada (1)	USUARIO	1	2026-08-25 15:20:34.169371	0:0:0:0:0:0:0:1	uploads/firmas_tecnico/firma_tecnico_1.png	FIRMA_TECNICO_ELIMINADA	1	admin
255	Firma permanente del tecnico registrada (1)	USUARIO	1	2026-08-25 15:20:44.142427	0:0:0:0:0:0:0:1	uploads/firmas_tecnico/firma_tecnico_1.png	FIRMA_TECNICO_REGISTRADA	1	admin
260	Firma permanente del tecnico actualizada (22)	USUARIO	22	2026-08-25 15:26:54.322474	127.0.0.1	uploads/firmas_tecnico/firma_tecnico_22.png	FIRMA_TECNICO_ACTUALIZADA	22	previewtest
250	PDF del acta visualizado/descargado	ACTA	81	2026-08-25 15:17:48.515113	0:0:0:0:0:0:0:1	/actas/81/pdf	DOCUMENTO_VISTO	1	admin
251	PDF del acta visualizado/descargado	ACTA	81	2026-08-25 15:18:20.001067	0:0:0:0:0:0:0:1	/actas/81/pdf	DOCUMENTO_VISTO	1	admin
252	PDF del acta visualizado/descargado	ACTA	81	2026-08-25 15:18:20.697445	0:0:0:0:0:0:0:1	/actas/81/pdf	DOCUMENTO_VISTO	1	admin
253	Firma permanente del tecnico actualizada (1)	USUARIO	1	2026-08-25 15:20:29.100766	0:0:0:0:0:0:0:1	uploads/firmas_tecnico/firma_tecnico_1.png	FIRMA_TECNICO_ACTUALIZADA	1	admin
256	Firma permanente del tecnico actualizada (1)	USUARIO	1	2026-08-25 15:23:57.778995	0:0:0:0:0:0:0:1	uploads/firmas_tecnico/firma_tecnico_1.png	FIRMA_TECNICO_ACTUALIZADA	1	admin
257	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:26:33.688996	127.0.0.1	/auth/login	LOGIN_EXITOSO	22	previewtest
258	Firma permanente del tecnico registrada (22)	USUARIO	22	2026-08-25 15:26:33.738484	127.0.0.1	uploads/firmas_tecnico/firma_tecnico_22.png	FIRMA_TECNICO_REGISTRADA	22	previewtest
259	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:26:54.273777	127.0.0.1	/auth/login	LOGIN_EXITOSO	22	previewtest
261	PDF del acta visualizado/descargado	ACTA	81	2026-08-25 15:31:53.823643	0:0:0:0:0:0:0:1	/actas/81/pdf	DOCUMENTO_VISTO	1	admin
262	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:35:58.868342	127.0.0.1	/auth/login	LOGIN_EXITOSO	22	previewtest
263	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:36:21.264877	127.0.0.1	/auth/login	LOGIN_EXITOSO	22	previewtest
264	Firma permanente del tecnico actualizada (22)	USUARIO	22	2026-08-25 15:36:21.317779	127.0.0.1	uploads/firmas_tecnico/firma_tecnico_22.png	FIRMA_TECNICO_ACTUALIZADA	22	previewtest
265	Firma permanente del tecnico eliminada (22)	USUARIO	22	2026-08-25 15:36:21.419591	127.0.0.1	uploads/firmas_tecnico/firma_tecnico_22.png	FIRMA_TECNICO_ELIMINADA	22	previewtest
266	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:36:37.273197	127.0.0.1	/auth/login	LOGIN_EXITOSO	22	previewtest
267	Firma permanente del tecnico registrada (22)	USUARIO	22	2026-08-25 15:36:37.322301	127.0.0.1	uploads/firmas_tecnico/firma_tecnico_22.png	FIRMA_TECNICO_REGISTRADA	22	previewtest
268	Firma permanente del tecnico eliminada (22)	USUARIO	22	2026-08-25 15:36:37.380633	127.0.0.1	uploads/firmas_tecnico/firma_tecnico_22.png	FIRMA_TECNICO_ELIMINADA	22	previewtest
269	Firma permanente del tecnico registrada (22)	USUARIO	22	2026-08-25 15:36:37.46354	127.0.0.1	uploads/firmas_tecnico/firma_tecnico_22.png	FIRMA_TECNICO_REGISTRADA	22	previewtest
270	Firma permanente del tecnico actualizada (1)	USUARIO	1	2026-08-25 15:38:30.007995	0:0:0:0:0:0:0:1	uploads/firmas_tecnico/firma_tecnico_1.png	FIRMA_TECNICO_ACTUALIZADA	1	admin
271	Cierre de sesion	AUTENTICACION	\N	2026-08-25 15:39:10.377846	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
272	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:39:14.873449	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	8	juantec
273	Cierre de sesion	AUTENTICACION	\N	2026-08-25 15:39:16.212345	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	8	juantec
274	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:39:24.357351	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
275	Cierre de sesion	AUTENTICACION	\N	2026-08-25 15:39:36.179921	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
276	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-25 15:39:44.834478	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	juantec
277	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-25 15:42:25.35025	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	admin
278	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:42:31.26736	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
279	PDF del acta visualizado/descargado	ACTA	78	2026-08-25 15:44:30.415022	0:0:0:0:0:0:0:1	/actas/78/pdf	DOCUMENTO_VISTO	1	admin
280	Evidencia FIRMA (firma_78.png) visualizada	ACTA	78	2026-08-25 15:44:30.415022	0:0:0:0:0:0:0:1	/actas/78/firma	EVIDENCIA_VISTA	1	admin
281	Evidencia FOTO (foto_78.jpg) visualizada	ACTA	78	2026-08-25 15:44:30.422527	0:0:0:0:0:0:0:1	/actas/78/foto	EVIDENCIA_VISTA	1	admin
282	PDF del acta visualizado/descargado	ACTA	77	2026-08-25 15:44:38.916727	0:0:0:0:0:0:0:1	/actas/77/pdf	DOCUMENTO_VISTO	1	admin
283	Evidencia FIRMA (firma_77.png) visualizada	ACTA	77	2026-08-25 15:44:38.920346	0:0:0:0:0:0:0:1	/actas/77/firma	EVIDENCIA_VISTA	1	admin
284	Evidencia FOTO (foto_77.jpg) visualizada	ACTA	77	2026-08-25 15:44:38.92429	0:0:0:0:0:0:0:1	/actas/77/foto	EVIDENCIA_VISTA	1	admin
285	PDF del acta visualizado/descargado	ACTA	81	2026-08-25 15:44:49.836989	0:0:0:0:0:0:0:1	/actas/81/pdf	DOCUMENTO_VISTO	1	admin
286	Evidencia FIRMA (firma_78.png) visualizada	ACTA	78	2026-08-25 15:45:58.769454	0:0:0:0:0:0:0:1	/actas/78/firma	EVIDENCIA_VISTA	1	admin
287	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:52:54.089362	127.0.0.1	/auth/login	LOGIN_EXITOSO	23	admtest2
288	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:53:39.369114	127.0.0.1	/auth/login	LOGIN_EXITOSO	23	admtest2
289	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 15:54:01.221209	127.0.0.1	/auth/login	LOGIN_EXITOSO	23	admtest2
290	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-25 15:54:01.315596	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	blocktest2
291	Evidencia FIRMA (firma_78.png) visualizada	ACTA	78	2026-08-25 15:54:01.385771	127.0.0.1	/actas/78/firma	EVIDENCIA_VISTA	23	admtest2
292	Evidencia FOTO (foto_78.jpg) visualizada	ACTA	78	2026-08-25 15:54:01.404191	127.0.0.1	/actas/78/foto	EVIDENCIA_VISTA	23	admtest2
293	PDF del acta visualizado/descargado	ACTA	78	2026-08-25 15:54:01.421411	127.0.0.1	/actas/78/pdf	DOCUMENTO_VISTO	23	admtest2
294	Evidencia FIRMA (firma_78.png) visualizada	ACTA	78	2026-08-25 15:59:33.434951	0:0:0:0:0:0:0:1	/actas/78/firma	EVIDENCIA_VISTA	1	admin
295	Evidencia FOTO (foto_78.jpg) visualizada	ACTA	78	2026-08-25 15:59:33.440564	0:0:0:0:0:0:0:1	/actas/78/foto	EVIDENCIA_VISTA	1	admin
296	Evidencia FOTO (foto_78.jpg) visualizada	ACTA	78	2026-08-25 15:59:36.462008	0:0:0:0:0:0:0:1	/actas/78/foto	EVIDENCIA_VISTA	1	admin
297	Evidencia FIRMA (firma_78.png) visualizada	ACTA	78	2026-08-25 15:59:36.462008	0:0:0:0:0:0:0:1	/actas/78/firma	EVIDENCIA_VISTA	1	admin
298	PDF del acta visualizado/descargado	ACTA	78	2026-08-25 15:59:36.469711	0:0:0:0:0:0:0:1	/actas/78/pdf	DOCUMENTO_VISTO	1	admin
299	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:01:21.456663	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
300	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:16.964441	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
301	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:17.533826	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
302	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:32.533381	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
305	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:39.998753	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
306	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:54.590459	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
310	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:22.683996	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
313	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:27.890035	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
314	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:30.331547	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
317	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:57.202123	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
318	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:07:00.125296	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
319	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:07:00.9034	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
326	PDF del acta visualizado/descargado	ACTA	83	2026-08-25 16:14:14.396343	0:0:0:0:0:0:0:1	/actas/83/pdf	DOCUMENTO_VISTO	1	admin
327	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-25 16:14:54.972257	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
303	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:33.049784	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
308	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:01.917055	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
309	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:02.49141	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
312	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:27.340158	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
316	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:56.434247	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
320	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:07:30.295009	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
321	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:07:30.89384	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
322	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:10:51.646698	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
323	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:10:52.374028	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
328	PDF del acta visualizado/descargado	ACTA	84	2026-08-25 16:20:37.631623	0:0:0:0:0:0:0:1	/actas/84/pdf	DOCUMENTO_VISTO	1	admin
330	PDF del acta visualizado/descargado	ACTA	84	2026-08-25 16:21:03.764555	0:0:0:0:0:0:0:1	/actas/84/pdf	DOCUMENTO_VISTO	1	admin
304	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:39.382354	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
307	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:03:55.195884	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
311	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:23.299222	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
315	PDF del acta visualizado/descargado	ACTA	82	2026-08-25 16:04:30.982502	0:0:0:0:0:0:0:1	/actas/82/pdf	DOCUMENTO_VISTO	1	admin
324	PDF del acta visualizado/descargado	ACTA	83	2026-08-25 16:12:50.566605	0:0:0:0:0:0:0:1	/actas/83/pdf	DOCUMENTO_VISTO	1	admin
325	PDF del acta visualizado/descargado	ACTA	83	2026-08-25 16:14:13.716173	0:0:0:0:0:0:0:1	/actas/83/pdf	DOCUMENTO_VISTO	1	admin
329	PDF del acta visualizado/descargado	ACTA	84	2026-08-25 16:21:03.194022	0:0:0:0:0:0:0:1	/actas/84/pdf	DOCUMENTO_VISTO	1	admin
331	PDF del acta visualizado/descargado	ACTA	85	2026-08-25 16:23:06.41962	0:0:0:0:0:0:0:1	/actas/85/pdf	DOCUMENTO_VISTO	1	admin
332	PDF del acta visualizado/descargado	ACTA	86	2026-08-25 16:27:20.699318	0:0:0:0:0:0:0:1	/actas/86/pdf	DOCUMENTO_VISTO	1	admin
333	PDF del acta visualizado/descargado	ACTA	86	2026-08-25 16:28:55.012022	0:0:0:0:0:0:0:1	/actas/86/pdf	DOCUMENTO_VISTO	1	admin
334	PDF del acta visualizado/descargado	ACTA	86	2026-08-25 16:28:55.610223	0:0:0:0:0:0:0:1	/actas/86/pdf	DOCUMENTO_VISTO	1	admin
335	PDF del acta visualizado/descargado	ACTA	86	2026-08-25 16:29:57.109941	0:0:0:0:0:0:0:1	/actas/86/pdf	DOCUMENTO_VISTO	1	admin
336	PDF del acta visualizado/descargado	ACTA	86	2026-08-25 16:29:57.842419	0:0:0:0:0:0:0:1	/actas/86/pdf	DOCUMENTO_VISTO	1	admin
337	PDF del acta visualizado/descargado	ACTA	86	2026-08-25 16:30:47.939597	0:0:0:0:0:0:0:1	/actas/86/pdf	DOCUMENTO_VISTO	1	admin
338	PDF del acta visualizado/descargado	ACTA	86	2026-08-25 16:30:48.49185	0:0:0:0:0:0:0:1	/actas/86/pdf	DOCUMENTO_VISTO	1	admin
339	PDF del acta visualizado/descargado	ACTA	87	2026-08-25 16:35:16.231858	0:0:0:0:0:0:0:1	/actas/87/pdf	DOCUMENTO_VISTO	1	admin
340	PDF del acta visualizado/descargado	ACTA	87	2026-08-25 16:36:50.449842	0:0:0:0:0:0:0:1	/actas/87/pdf	DOCUMENTO_VISTO	1	admin
341	PDF del acta visualizado/descargado	ACTA	87	2026-08-25 16:36:51.128368	0:0:0:0:0:0:0:1	/actas/87/pdf	DOCUMENTO_VISTO	1	admin
342	PDF del acta visualizado/descargado	ACTA	88	2026-08-25 16:39:49.297561	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
343	PDF del acta visualizado/descargado	ACTA	88	2026-08-25 16:45:52.487892	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
344	PDF del acta visualizado/descargado	ACTA	88	2026-08-25 16:46:19.759024	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
345	PDF del acta visualizado/descargado	ACTA	88	2026-08-25 16:46:31.505527	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
346	Codigo OTP emitido para firma_token id=67	FIRMA_TOKEN	ec085287-8e60-4ea3-af14-9dddb00d2903	2026-08-25 16:46:56.174357	0:0:0:0:0:0:0:1	/firma/ec085287-8e60-4ea3-af14-9dddb00d2903/otp	OTP_GENERADO	\N	PORTAL_FIRMA
347	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	ec085287-8e60-4ea3-af14-9dddb00d2903	2026-08-25 16:46:58.76422	0:0:0:0:0:0:0:1	/firma/ec085287-8e60-4ea3-af14-9dddb00d2903/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
348	PDF del acta visualizado/descargado	ACTA	88	2026-08-25 16:46:58.85095	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
349	PDF del acta visualizado/descargado	ACTA	88	2026-08-25 16:47:03.545076	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
350	Codigo OTP validado, sesion 001716ae... (j***@gmail.com)	FIRMA_TOKEN	ec085287-8e60-4ea3-af14-9dddb00d2903	2026-08-25 16:47:30.243499	0:0:0:0:0:0:0:1	/firma/ec085287-8e60-4ea3-af14-9dddb00d2903/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
351	El firmante visualizo el PDF del acta	ACTA	88	2026-08-25 16:47:30.336575	0:0:0:0:0:0:0:1	/firma/ec085287-8e60-4ea3-af14-9dddb00d2903/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
352	El firmante visualizo el PDF del acta	ACTA	88	2026-08-25 16:56:41.227258	0:0:0:0:0:0:0:1	/firma/ec085287-8e60-4ea3-af14-9dddb00d2903/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
353	El firmante visualizo el PDF del acta	ACTA	88	2026-08-25 16:56:42.017482	0:0:0:0:0:0:0:1	/firma/ec085287-8e60-4ea3-af14-9dddb00d2903/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
355	Evidencia FOTO (foto_77.jpg) visualizada	ACTA	77	2026-08-26 08:26:18.859172	0:0:0:0:0:0:0:1	/actas/77/foto	EVIDENCIA_VISTA	1	admin
354	Evidencia FIRMA (firma_77.png) visualizada	ACTA	77	2026-08-26 08:26:18.859172	0:0:0:0:0:0:0:1	/actas/77/firma	EVIDENCIA_VISTA	1	admin
356	Cierre de sesion	AUTENTICACION	\N	2026-08-26 08:27:09.714703	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
357	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-26 08:27:14.901141	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	juantec
358	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-26 08:27:27.448735	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
359	Cierre de sesion	AUTENTICACION	\N	2026-08-26 08:27:48.819672	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
360	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-26 08:27:54.171742	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	8	juantec
361	Firma permanente del tecnico registrada (8)	USUARIO	8	2026-08-26 08:28:20.321411	0:0:0:0:0:0:0:1	uploads/firmas_tecnico/firma_tecnico_8.png	FIRMA_TECNICO_REGISTRADA	8	juantec
362	Cierre de sesion	AUTENTICACION	\N	2026-08-26 08:30:08.873497	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	8	juantec
363	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-26 08:30:16.324907	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
364	PDF del acta visualizado/descargado	ACTA	88	2026-08-26 08:31:10.453724	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
365	PDF del acta visualizado/descargado	ACTA	88	2026-08-26 08:31:45.425701	0:0:0:0:0:0:0:1	/actas/88/pdf	DOCUMENTO_VISTO	1	admin
366	PDF del acta visualizado/descargado	ACTA	89	2026-08-26 09:20:43.577028	0:0:0:0:0:0:0:1	/actas/89/pdf	DOCUMENTO_VISTO	1	admin
367	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-26 09:28:30.36478	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
368	PDF del acta visualizado/descargado	ACTA	90	2026-08-26 11:08:29.555972	0:0:0:0:0:0:0:1	/actas/90/pdf	DOCUMENTO_VISTO	1	admin
369	PDF del acta visualizado/descargado	ACTA	90	2026-08-26 11:08:44.747398	0:0:0:0:0:0:0:1	/actas/90/pdf	DOCUMENTO_VISTO	1	admin
370	PDF del acta visualizado/descargado	ACTA	90	2026-08-26 11:09:19.993457	0:0:0:0:0:0:0:1	/actas/90/pdf	DOCUMENTO_VISTO	1	admin
371	PDF del acta visualizado/descargado	ACTA	92	2026-08-26 11:28:17.771398	0:0:0:0:0:0:0:1	/actas/92/pdf	DOCUMENTO_VISTO	1	admin
379	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-26 16:27:40.790616	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
380	PDF del acta visualizado/descargado	ACTA	90	2026-08-26 16:45:09.764411	0:0:0:0:0:0:0:1	/actas/90/pdf	DOCUMENTO_VISTO	1	admin
381	PDF del acta visualizado/descargado	ACTA	89	2026-08-26 16:45:34.726994	0:0:0:0:0:0:0:1	/actas/89/pdf	DOCUMENTO_VISTO	1	admin
382	Cierre de sesion	AUTENTICACION	\N	2026-08-26 16:48:18.476087	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
383	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-26 16:48:25.702738	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	29	auditor
384	PDF del acta visualizado/descargado	ACTA	92	2026-08-26 16:48:33.152234	0:0:0:0:0:0:0:1	/actas/92/pdf	DOCUMENTO_VISTO	29	auditor
385	PDF del acta visualizado/descargado	ACTA	92	2026-08-26 16:49:01.195811	0:0:0:0:0:0:0:1	/actas/92/pdf	DOCUMENTO_VISTO	29	auditor
386	Cierre de sesion	AUTENTICACION	\N	2026-08-27 08:16:29.530256	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	29	auditor
387	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-27 08:16:35.170612	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	admin
388	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 08:16:38.233809	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
389	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:26:02.190274	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
390	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:29:39.409196	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
391	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:29:40.347206	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
392	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:29:48.690624	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
393	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:29:49.308457	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
394	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:20.468684	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
395	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:21.386738	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
396	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:29.283606	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
397	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:29.534882	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
398	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:29.961222	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
399	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:30.841495	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
400	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:36.827283	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
401	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:46.803159	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
402	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:47.629431	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
403	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:56.327425	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
404	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:30:56.889923	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
405	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:31:01.502687	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
406	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:31:02.437408	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
407	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:31:09.497548	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
408	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:31:09.654962	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
409	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:31:10.126702	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
410	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:32:11.46359	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
411	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:32:12.482866	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
412	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:32:20.279788	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
413	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:32:20.789119	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
414	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:03.233316	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
415	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:04.05126	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
416	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:11.283839	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
417	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:11.843029	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
418	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:49.696339	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
419	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:50.498654	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
420	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:58.134547	127.0.0.1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
421	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:35:58.450811	127.0.0.1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
422	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:42:41.356068	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
423	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:42:42.409749	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
424	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:42:50.932958	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
425	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:42:51.370557	0:0:0:0:0:0:0:1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
426	PDF del acta visualizado/descargado	ACTA	96	2026-08-27 11:46:39.404174	127.0.0.1	/actas/96/pdf	DOCUMENTO_VISTO	1	admin
427	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 11:49:28.973774	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
428	Codigo OTP emitido para firma_token id=68	FIRMA_TOKEN	a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b	2026-08-27 11:50:12.173778	0:0:0:0:0:0:0:1	/firma/a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b/otp	OTP_GENERADO	\N	PORTAL_FIRMA
429	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b	2026-08-27 11:50:14.361376	0:0:0:0:0:0:0:1	/firma/a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
430	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 11:50:14.501813	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
431	Codigo OTP validado, sesion cf6c7cba... (j***@gmail.com)	FIRMA_TOKEN	a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b	2026-08-27 11:50:52.890531	0:0:0:0:0:0:0:1	/firma/a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
432	El firmante visualizo el PDF del acta	ACTA	97	2026-08-27 11:50:52.968795	0:0:0:0:0:0:0:1	/firma/a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
433	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 11:51:29.094911	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
434	Evidencia FOTO (foto_97.jpg) visualizada	ACTA	97	2026-08-27 11:51:29.101433	0:0:0:0:0:0:0:1	/actas/97/foto	EVIDENCIA_VISTA	1	admin
435	Evidencia FIRMA (firma_97.png) visualizada	ACTA	97	2026-08-27 11:51:29.101433	0:0:0:0:0:0:0:1	/actas/97/firma	EVIDENCIA_VISTA	1	admin
436	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 11:55:56.169413	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
437	Evidencia FIRMA (firma_97.png) visualizada	ACTA	97	2026-08-27 11:55:56.171261	0:0:0:0:0:0:0:1	/actas/97/firma	EVIDENCIA_VISTA	1	admin
438	Evidencia FOTO (foto_97.jpg) visualizada	ACTA	97	2026-08-27 11:55:56.19937	0:0:0:0:0:0:0:1	/actas/97/foto	EVIDENCIA_VISTA	1	admin
439	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 12:01:51.630103	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
440	Evidencia FIRMA (firma_97.png) visualizada	ACTA	97	2026-08-27 12:01:51.648242	0:0:0:0:0:0:0:1	/actas/97/firma	EVIDENCIA_VISTA	1	admin
441	Evidencia FOTO (foto_97.jpg) visualizada	ACTA	97	2026-08-27 12:01:51.657789	0:0:0:0:0:0:0:1	/actas/97/foto	EVIDENCIA_VISTA	1	admin
442	Evidencia FOTO (foto_97.jpg) visualizada	ACTA	97	2026-08-27 12:02:05.945154	0:0:0:0:0:0:0:1	/actas/97/foto	EVIDENCIA_VISTA	1	admin
443	Evidencia FIRMA (firma_97.png) visualizada	ACTA	97	2026-08-27 12:02:05.943014	0:0:0:0:0:0:0:1	/actas/97/firma	EVIDENCIA_VISTA	1	admin
444	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 12:02:05.943014	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
446	Evidencia FIRMA (firma_97.png) visualizada	ACTA	97	2026-08-27 12:02:16.211654	0:0:0:0:0:0:0:1	/actas/97/firma	EVIDENCIA_VISTA	1	admin
445	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 12:02:16.211654	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
447	Evidencia FOTO (foto_97.jpg) visualizada	ACTA	97	2026-08-27 12:02:16.253593	127.0.0.1	/actas/97/foto	EVIDENCIA_VISTA	1	admin
448	Evidencia FIRMA (firma_97.png) visualizada	ACTA	97	2026-08-27 12:02:19.323118	0:0:0:0:0:0:0:1	/actas/97/firma	EVIDENCIA_VISTA	1	admin
449	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 12:02:19.323118	127.0.0.1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
450	Evidencia FOTO (foto_97.jpg) visualizada	ACTA	97	2026-08-27 12:02:19.328613	0:0:0:0:0:0:0:1	/actas/97/foto	EVIDENCIA_VISTA	1	admin
453	Evidencia FIRMA (firma_97.png) visualizada	ACTA	97	2026-08-27 12:06:53.488269	0:0:0:0:0:0:0:1	/actas/97/firma	EVIDENCIA_VISTA	1	admin
452	Evidencia FOTO (foto_97.jpg) visualizada	ACTA	97	2026-08-27 12:06:53.4909	0:0:0:0:0:0:0:1	/actas/97/foto	EVIDENCIA_VISTA	1	admin
451	PDF del acta visualizado/descargado	ACTA	97	2026-08-27 12:06:53.488269	0:0:0:0:0:0:0:1	/actas/97/pdf	DOCUMENTO_VISTO	1	admin
454	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:08:38.065994	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
455	Codigo OTP emitido para firma_token id=69	FIRMA_TOKEN	77a95c3a-a46f-42f8-8826-48fb1b3121ee	2026-08-27 12:09:08.9981	0:0:0:0:0:0:0:1	/firma/77a95c3a-a46f-42f8-8826-48fb1b3121ee/otp	OTP_GENERADO	\N	PORTAL_FIRMA
456	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	77a95c3a-a46f-42f8-8826-48fb1b3121ee	2026-08-27 12:09:11.171297	0:0:0:0:0:0:0:1	/firma/77a95c3a-a46f-42f8-8826-48fb1b3121ee/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
457	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:09:11.291391	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
458	Codigo OTP validado, sesion b97ca181... (j***@gmail.com)	FIRMA_TOKEN	77a95c3a-a46f-42f8-8826-48fb1b3121ee	2026-08-27 12:09:28.407352	0:0:0:0:0:0:0:1	/firma/77a95c3a-a46f-42f8-8826-48fb1b3121ee/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
459	El firmante visualizo el PDF del acta	ACTA	98	2026-08-27 12:09:28.49036	0:0:0:0:0:0:0:1	/firma/77a95c3a-a46f-42f8-8826-48fb1b3121ee/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
460	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:10:12.041418	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
461	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:12.045736	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
462	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:12.050357	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
463	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:10:13.157467	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
464	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:13.158399	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
465	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:13.171501	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
466	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:16.440041	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
467	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:16.443771	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
468	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:10:16.440041	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
469	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:18.343778	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
470	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:18.345767	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
471	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:20.426145	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
472	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:20.428004	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
473	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:10:33.159504	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
478	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:33.504969	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
481	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:35.245103	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
483	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:54.409944	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
474	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:33.163138	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
476	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:33.502514	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
484	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 12:10:54.410975	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
475	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:33.163138	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
477	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:10:33.502514	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
479	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:10:35.242935	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
482	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 12:10:54.409944	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
480	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 12:10:35.242935	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
485	Cierre de sesion	AUTENTICACION	\N	2026-08-27 14:30:55.432322	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
486	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-27 14:31:08.015914	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	auditor
487	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 14:31:11.632614	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	29	auditor
488	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 14:47:46.817048	127.0.0.1	/auth/login	LOGIN_EXITOSO	30	auditorem
489	Access Denied	RECURSO	/actas/98/enviar	2026-08-27 14:47:47.762135	127.0.0.1	/actas/98/enviar	ACCESO_DENEGADO	30	auditorem
490	Access Denied	RECURSO	/actas/98/aprobar	2026-08-27 14:47:47.976161	127.0.0.1	/actas/98/aprobar	ACCESO_DENEGADO	30	auditorem
491	Access Denied	RECURSO	/actas/98/rechazar	2026-08-27 14:47:48.198842	127.0.0.1	/actas/98/rechazar	ACCESO_DENEGADO	30	auditorem
492	Access Denied	RECURSO	/generar-formateo-seguro	2026-08-27 14:48:17.737999	127.0.0.1	/generar-formateo-seguro	ACCESO_DENEGADO	30	auditorem
493	Access Denied	RECURSO	/generar-devolucion	2026-08-27 14:48:18.044549	127.0.0.1	/generar-devolucion	ACCESO_DENEGADO	30	auditorem
494	Access Denied	RECURSO	/generar-acta	2026-08-27 14:48:28.744945	127.0.0.1	/generar-acta	ACCESO_DENEGADO	30	auditorem
495	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 14:49:04.833185	127.0.0.1	/auth/login	LOGIN_EXITOSO	31	tecnicoev
496	Codigo OTP emitido para firma_token id=70	FIRMA_TOKEN	3131f844-13dc-4cb1-9f31-d018fa874c7a	2026-08-27 14:50:05.010076	127.0.0.1	/firma/3131f844-13dc-4cb1-9f31-d018fa874c7a/otp	OTP_GENERADO	\N	PORTAL_FIRMA
497	Correo OTP enviado a d***@test.com	FIRMA_TOKEN	3131f844-13dc-4cb1-9f31-d018fa874c7a	2026-08-27 14:50:06.342545	127.0.0.1	/firma/3131f844-13dc-4cb1-9f31-d018fa874c7a/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
498	No tiene permisos para acceder a esta acta (pertenece a otro tecnico)	RECURSO	/actas/98/enviar	2026-08-27 14:50:06.467106	127.0.0.1	/actas/98/enviar	ACCESO_DENEGADO	31	tecnicoev
499	No tiene permisos para acceder a esta acta (pertenece a otro tecnico)	RECURSO	/actas/98/aprobar	2026-08-27 14:50:06.5721	127.0.0.1	/actas/98/aprobar	ACCESO_DENEGADO	31	tecnicoev
500	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 14:51:03.412182	127.0.0.1	/auth/login	LOGIN_EXITOSO	32	adminev
501	Access Denied	RECURSO	/generar-formateo-seguro	2026-08-27 14:52:34.411887	127.0.0.1	/generar-formateo-seguro	ACCESO_DENEGADO	\N	\N
502	Access Denied	RECURSO	/generar-devolucion	2026-08-27 14:52:34.526495	127.0.0.1	/generar-devolucion	ACCESO_DENEGADO	\N	\N
503	PDF del acta visualizado/descargado	ACTA	99	2026-08-27 14:54:00.864608	0:0:0:0:0:0:0:1	/actas/99/pdf	DOCUMENTO_VISTO	29	auditor
504	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 14:54:11.45244	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	29	auditor
505	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 14:54:11.457488	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	29	auditor
506	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 14:54:11.457488	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	29	auditor
507	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 15:04:04.027527	127.0.0.1	/auth/login	LOGIN_EXITOSO	30	auditorem
508	Access Denied	RECURSO	/usuarios/me/firma	2026-08-27 15:04:05.215743	127.0.0.1	/usuarios/me/firma	ACCESO_DENEGADO	30	auditorem
509	Access Denied	RECURSO	/usuarios/me/firma/archivo	2026-08-27 15:04:05.601669	127.0.0.1	/usuarios/me/firma/archivo	ACCESO_DENEGADO	30	auditorem
510	Access Denied	RECURSO	/usuarios/me/firma	2026-08-27 15:04:05.945786	127.0.0.1	/usuarios/me/firma	ACCESO_DENEGADO	30	auditorem
511	Access Denied	RECURSO	/usuarios/me/firma	2026-08-27 15:04:06.308999	127.0.0.1	/usuarios/me/firma	ACCESO_DENEGADO	30	auditorem
512	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 15:26:18.713769	127.0.0.1	/auth/login	LOGIN_EXITOSO	30	auditorem
513	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 16:22:56.045425	127.0.0.1	/auth/login	LOGIN_EXITOSO	32	adminev
514	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 16:22:57.527939	127.0.0.1	/auth/login	LOGIN_EXITOSO	31	tecnicoev
515	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 16:22:58.560272	127.0.0.1	/auth/login	LOGIN_EXITOSO	30	auditorem
516	Cierre de sesion	AUTENTICACION	\N	2026-08-27 16:25:05.830723	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	29	auditor
517	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-27 16:25:14.094351	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	admin
518	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-27 16:25:21.947189	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
519	Evidencia FIRMA (firma_98.png) visualizada	ACTA	98	2026-08-27 16:30:22.740821	0:0:0:0:0:0:0:1	/actas/98/firma	EVIDENCIA_VISTA	1	admin
520	PDF del acta visualizado/descargado	ACTA	98	2026-08-27 16:30:22.740821	0:0:0:0:0:0:0:1	/actas/98/pdf	DOCUMENTO_VISTO	1	admin
521	Evidencia FOTO (foto_98.jpg) visualizada	ACTA	98	2026-08-27 16:30:22.740821	0:0:0:0:0:0:0:1	/actas/98/foto	EVIDENCIA_VISTA	1	admin
522	PDF del acta visualizado/descargado	ACTA	100	2026-08-27 16:51:42.732587	0:0:0:0:0:0:0:1	/actas/100/pdf	DOCUMENTO_VISTO	1	admin
523	PDF del acta visualizado/descargado	ACTA	100	2026-08-27 16:53:09.442314	0:0:0:0:0:0:0:1	/actas/100/pdf	DOCUMENTO_VISTO	1	admin
524	Codigo OTP emitido para firma_token id=71	FIRMA_TOKEN	2957f6e7-92c6-459f-b523-a01f3283410b	2026-08-27 16:53:19.596265	0:0:0:0:0:0:0:1	/firma/2957f6e7-92c6-459f-b523-a01f3283410b/otp	OTP_GENERADO	\N	PORTAL_FIRMA
525	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	2957f6e7-92c6-459f-b523-a01f3283410b	2026-08-27 16:53:22.690765	0:0:0:0:0:0:0:1	/firma/2957f6e7-92c6-459f-b523-a01f3283410b/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
526	PDF del acta visualizado/descargado	ACTA	100	2026-08-27 16:53:22.821391	0:0:0:0:0:0:0:1	/actas/100/pdf	DOCUMENTO_VISTO	1	admin
527	Codigo OTP emitido para firma_token id=72	FIRMA_TOKEN	ca7b3ae3-87cc-4551-9b76-6771940ac8c2	2026-08-27 16:58:53.794469	127.0.0.1	/firma/ca7b3ae3-87cc-4551-9b76-6771940ac8c2/otp	OTP_GENERADO	\N	PORTAL_FIRMA
528	Correo OTP enviado a j***@coltefinanciera.com	FIRMA_TOKEN	ca7b3ae3-87cc-4551-9b76-6771940ac8c2	2026-08-27 16:58:55.8497	127.0.0.1	/firma/ca7b3ae3-87cc-4551-9b76-6771940ac8c2/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
529	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:02:55.101379	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
530	Codigo OTP emitido para firma_token id=73	FIRMA_TOKEN	40087e9b-22eb-405f-838d-f71083cc2d9c	2026-08-27 17:03:19.387948	0:0:0:0:0:0:0:1	/firma/40087e9b-22eb-405f-838d-f71083cc2d9c/otp	OTP_GENERADO	\N	PORTAL_FIRMA
531	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	40087e9b-22eb-405f-838d-f71083cc2d9c	2026-08-27 17:03:20.892788	0:0:0:0:0:0:0:1	/firma/40087e9b-22eb-405f-838d-f71083cc2d9c/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
532	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:03:21.046937	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
533	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:04:21.605537	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
534	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:07:15.716239	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
535	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:07:17.736808	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
536	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:07:17.963265	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
537	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:07:23.18674	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
538	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:07:28.648897	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
539	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:09:47.259632	127.0.0.1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
540	PDF del acta visualizado/descargado	ACTA	102	2026-08-27 17:12:16.414859	0:0:0:0:0:0:0:1	/actas/102/pdf	DOCUMENTO_VISTO	1	admin
541	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 08:01:44.979361	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
542	PDF del acta visualizado/descargado	ACTA	103	2026-08-31 08:02:56.415629	0:0:0:0:0:0:0:1	/actas/103/pdf	DOCUMENTO_VISTO	1	admin
543	Codigo OTP emitido para firma_token id=74	FIRMA_TOKEN	39096d26-0cfb-4b26-b4b3-8fb108b3b40e	2026-08-31 08:03:20.905171	0:0:0:0:0:0:0:1	/firma/39096d26-0cfb-4b26-b4b3-8fb108b3b40e/otp	OTP_GENERADO	\N	PORTAL_FIRMA
544	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	39096d26-0cfb-4b26-b4b3-8fb108b3b40e	2026-08-31 08:03:23.564514	0:0:0:0:0:0:0:1	/firma/39096d26-0cfb-4b26-b4b3-8fb108b3b40e/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
545	PDF del acta visualizado/descargado	ACTA	104	2026-08-31 08:25:17.935128	0:0:0:0:0:0:0:1	/actas/104/pdf	DOCUMENTO_VISTO	1	admin
546	Codigo OTP emitido para firma_token id=75	FIRMA_TOKEN	479c9ee2-5112-4b80-b330-39e4e364e5d4	2026-08-31 08:25:35.235355	0:0:0:0:0:0:0:1	/firma/479c9ee2-5112-4b80-b330-39e4e364e5d4/otp	OTP_GENERADO	\N	PORTAL_FIRMA
547	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	479c9ee2-5112-4b80-b330-39e4e364e5d4	2026-08-31 08:25:35.266208	0:0:0:0:0:0:0:1	/firma/479c9ee2-5112-4b80-b330-39e4e364e5d4/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
548	Codigo OTP emitido para firma_token id=76	FIRMA_TOKEN	143ccd6a-c641-4b8a-9cd9-4cdecbd7e30d	2026-08-31 08:25:41.108838	0:0:0:0:0:0:0:1	/firma/143ccd6a-c641-4b8a-9cd9-4cdecbd7e30d/otp	OTP_GENERADO	\N	PORTAL_FIRMA
549	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	143ccd6a-c641-4b8a-9cd9-4cdecbd7e30d	2026-08-31 08:25:41.117648	0:0:0:0:0:0:0:1	/firma/143ccd6a-c641-4b8a-9cd9-4cdecbd7e30d/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
550	Codigo OTP emitido para firma_token id=77	FIRMA_TOKEN	3aa15127-6fe9-42a1-8757-b40eeac6a6de	2026-08-31 08:25:45.19032	0:0:0:0:0:0:0:1	/firma/3aa15127-6fe9-42a1-8757-b40eeac6a6de/otp	OTP_GENERADO	\N	PORTAL_FIRMA
551	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	3aa15127-6fe9-42a1-8757-b40eeac6a6de	2026-08-31 08:25:45.20974	0:0:0:0:0:0:0:1	/firma/3aa15127-6fe9-42a1-8757-b40eeac6a6de/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
552	Codigo OTP emitido para firma_token id=78	FIRMA_TOKEN	ef62e44a-a436-44c5-909d-51860665e029	2026-08-31 08:26:00.496156	0:0:0:0:0:0:0:1	/firma/ef62e44a-a436-44c5-909d-51860665e029/otp	OTP_GENERADO	\N	PORTAL_FIRMA
553	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	ef62e44a-a436-44c5-909d-51860665e029	2026-08-31 08:26:00.499768	0:0:0:0:0:0:0:1	/firma/ef62e44a-a436-44c5-909d-51860665e029/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
554	Codigo OTP emitido para firma_token id=79	FIRMA_TOKEN	cf423a8d-21af-48d0-a8da-978bb8489eaf	2026-08-31 08:26:07.985091	0:0:0:0:0:0:0:1	/firma/cf423a8d-21af-48d0-a8da-978bb8489eaf/otp	OTP_GENERADO	\N	PORTAL_FIRMA
555	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	cf423a8d-21af-48d0-a8da-978bb8489eaf	2026-08-31 08:26:08.006951	0:0:0:0:0:0:0:1	/firma/cf423a8d-21af-48d0-a8da-978bb8489eaf/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
556	Cierre de sesion	AUTENTICACION	\N	2026-08-31 08:26:30.122166	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
557	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 08:26:35.914946	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
558	Codigo OTP emitido para firma_token id=80	FIRMA_TOKEN	b7384b37-69a5-4db8-bc78-0f4c6ac6673f	2026-08-31 08:26:47.136382	0:0:0:0:0:0:0:1	/firma/b7384b37-69a5-4db8-bc78-0f4c6ac6673f/otp	OTP_GENERADO	\N	PORTAL_FIRMA
559	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	b7384b37-69a5-4db8-bc78-0f4c6ac6673f	2026-08-31 08:26:47.152223	0:0:0:0:0:0:0:1	/firma/b7384b37-69a5-4db8-bc78-0f4c6ac6673f/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
560	Codigo OTP emitido para firma_token id=81	FIRMA_TOKEN	dc1a4fbf-2b58-47da-9e61-73f68769225c	2026-08-31 08:27:45.85628	0:0:0:0:0:0:0:1	/firma/dc1a4fbf-2b58-47da-9e61-73f68769225c/otp	OTP_GENERADO	\N	PORTAL_FIRMA
561	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	dc1a4fbf-2b58-47da-9e61-73f68769225c	2026-08-31 08:27:45.860629	0:0:0:0:0:0:0:1	/firma/dc1a4fbf-2b58-47da-9e61-73f68769225c/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
562	Codigo OTP emitido para firma_token id=82	FIRMA_TOKEN	ea45c844-68d6-484f-8922-bca52f1ba19d	2026-08-31 08:28:09.157872	0:0:0:0:0:0:0:1	/firma/ea45c844-68d6-484f-8922-bca52f1ba19d/otp	OTP_GENERADO	\N	PORTAL_FIRMA
563	Fallo el envio de correo OTP a j***@gmail.com	FIRMA_TOKEN	ea45c844-68d6-484f-8922-bca52f1ba19d	2026-08-31 08:28:09.157872	0:0:0:0:0:0:0:1	/firma/ea45c844-68d6-484f-8922-bca52f1ba19d/otp	OTP_ENVIO_FALLIDO	\N	PORTAL_FIRMA
564	PDF del acta visualizado/descargado	ACTA	105	2026-08-31 08:48:01.60385	0:0:0:0:0:0:0:1	/actas/105/pdf	DOCUMENTO_VISTO	1	admin
565	Codigo OTP emitido para firma_token id=83	FIRMA_TOKEN	11618a88-98ae-41a0-924a-400bf02fc1ab	2026-08-31 08:48:15.090653	0:0:0:0:0:0:0:1	/firma/11618a88-98ae-41a0-924a-400bf02fc1ab/otp	OTP_GENERADO	\N	PORTAL_FIRMA
566	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	11618a88-98ae-41a0-924a-400bf02fc1ab	2026-08-31 08:48:17.86721	0:0:0:0:0:0:0:1	/firma/11618a88-98ae-41a0-924a-400bf02fc1ab/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
567	PDF del acta visualizado/descargado	ACTA	105	2026-08-31 08:48:17.964105	0:0:0:0:0:0:0:1	/actas/105/pdf	DOCUMENTO_VISTO	1	admin
568	PDF del acta visualizado/descargado	ACTA	106	2026-08-31 09:11:28.219226	0:0:0:0:0:0:0:1	/actas/106/pdf	DOCUMENTO_VISTO	1	admin
569	Codigo OTP emitido para firma_token id=84	FIRMA_TOKEN	55bce696-b806-45f4-b9d0-b780bbbc8327	2026-08-31 09:11:46.688911	0:0:0:0:0:0:0:1	/firma/55bce696-b806-45f4-b9d0-b780bbbc8327/otp	OTP_GENERADO	\N	PORTAL_FIRMA
571	PDF del acta visualizado/descargado	ACTA	106	2026-08-31 09:11:49.76145	0:0:0:0:0:0:0:1	/actas/106/pdf	DOCUMENTO_VISTO	1	admin
570	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	55bce696-b806-45f4-b9d0-b780bbbc8327	2026-08-31 09:11:49.596756	0:0:0:0:0:0:0:1	/firma/55bce696-b806-45f4-b9d0-b780bbbc8327/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
572	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 10:38:09.570123	127.0.0.1	/auth/login	LOGIN_EXITOSO	33	qasecurity1_1788190688
573	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 10:47:56.9319	127.0.0.1	/auth/login	LOGIN_EXITOSO	33	qasecurity1_1788190688
574	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 10:48:06.948379	127.0.0.1	/auth/login	LOGIN_EXITOSO	34	qasecurity2_1788191286
575	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-31 10:49:03.115935	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	zzz
576	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-31 10:55:10.282881	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	qatech_1788191709
577	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 10:55:37.426882	127.0.0.1	/auth/login	LOGIN_EXITOSO	35	qatech_1788191736
578	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 10:55:37.764501	127.0.0.1	/auth/login	LOGIN_EXITOSO	35	qatech_1788191736
579	Access Denied	RECURSO	/usuario/diagnostico	2026-08-31 10:55:38.109232	127.0.0.1	/usuario/diagnostico	ACCESO_DENEGADO	35	qatech_1788191736
580	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-31 11:43:21.959181	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	qadocumental
581	Intento de autenticacion fallido	AUTENTICACION	\N	2026-08-31 11:43:32.051156	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	qadocumental
582	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 11:43:45.219559	127.0.0.1	/auth/login	LOGIN_EXITOSO	36	qadocumental
583	Access Denied	RECURSO	/generar-acta	2026-08-31 11:44:03.55243	127.0.0.1	/generar-acta	ACCESO_DENEGADO	\N	\N
584	Access Denied	RECURSO	/generar-acta	2026-08-31 11:44:03.698606	127.0.0.1	/generar-acta	ACCESO_DENEGADO	\N	\N
585	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 11:44:17.434364	127.0.0.1	/auth/login	LOGIN_EXITOSO	36	qadocumental
586	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 11:45:29.05086	127.0.0.1	/auth/login	LOGIN_EXITOSO	36	qadocumental
587	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 11:46:02.001395	127.0.0.1	/auth/login	LOGIN_EXITOSO	36	qadocumental
588	Access Denied	RECURSO	/generar-acta	2026-08-31 12:17:26.666686	127.0.0.1	/generar-acta	ACCESO_DENEGADO	\N	\N
589	Access Denied	RECURSO	/generar-formateo-seguro	2026-08-31 12:17:46.220939	127.0.0.1	/generar-formateo-seguro	ACCESO_DENEGADO	\N	\N
590	Access Denied	RECURSO	/generar-acta	2026-08-31 12:17:46.394231	127.0.0.1	/generar-acta	ACCESO_DENEGADO	\N	\N
591	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:18:34.672673	127.0.0.1	/auth/login	LOGIN_EXITOSO	37	qa_formateo
592	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:19:24.031003	127.0.0.1	/auth/login	LOGIN_EXITOSO	1	admin
593	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:24:28.635785	127.0.0.1	/auth/login	LOGIN_EXITOSO	1	admin
594	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:26:51.099423	127.0.0.1	/auth/login	LOGIN_EXITOSO	1	admin
595	Codigo OTP emitido para firma_token id=86	FIRMA_TOKEN	98b318a6-fdfc-41fc-b105-3e2b88ed2a9d	2026-08-31 12:29:09.513548	127.0.0.1	/firma/98b318a6-fdfc-41fc-b105-3e2b88ed2a9d/otp	OTP_GENERADO	\N	PORTAL_FIRMA
596	Correo OTP enviado a m***@empresa.com	FIRMA_TOKEN	98b318a6-fdfc-41fc-b105-3e2b88ed2a9d	2026-08-31 12:29:11.797966	127.0.0.1	/firma/98b318a6-fdfc-41fc-b105-3e2b88ed2a9d/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
597	PDF del acta visualizado/descargado	ACTA	106	2026-08-31 12:36:33.218748	0:0:0:0:0:0:0:1	/actas/106/pdf	DOCUMENTO_VISTO	1	admin
598	Cierre de sesion	AUTENTICACION	\N	2026-08-31 12:36:51.576938	0:0:0:0:0:0:0:1	/auth/logout	LOGOUT	1	admin
599	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:36:56.135322	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
600	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:45:17.878049	127.0.0.1	/auth/login	LOGIN_EXITOSO	1	admin
601	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:45:41.221054	127.0.0.1	/auth/login	LOGIN_EXITOSO	1	admin
602	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:45:57.861954	127.0.0.1	/auth/login	LOGIN_EXITOSO	39	tecprueba
603	Inicio de sesion exitoso	AUTENTICACION	\N	2026-08-31 12:46:08.3289	127.0.0.1	/auth/login	LOGIN_EXITOSO	38	audprueba
604	Access Denied	RECURSO	/usuario	2026-08-31 12:46:08.514618	127.0.0.1	/usuario	ACCESO_DENEGADO	38	audprueba
605	PDF del acta visualizado/descargado	ACTA	118	2026-08-31 13:36:54.321337	0:0:0:0:0:0:0:1	/actas/118/pdf	DOCUMENTO_VISTO	1	admin
606	PDF del acta visualizado/descargado	ACTA	118	2026-08-31 13:40:03.041729	0:0:0:0:0:0:0:1	/actas/118/pdf	DOCUMENTO_VISTO	1	admin
607	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:31:45.572523	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
609	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:31:59.868235	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
611	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:32:06.633111	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
612	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:32:47.49999	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
613	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:32:48.268896	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
614	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:33:19.695539	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
615	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:34:09.855675	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
616	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:34:10.417989	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
618	Codigo OTP emitido para firma_token id=87	FIRMA_TOKEN	024c72cc-2477-4c84-b2a9-265751ff65dc	2026-09-01 12:34:49.518437	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/otp	OTP_GENERADO	\N	PORTAL_FIRMA
619	Correo OTP enviado a j***@gmail.com	FIRMA_TOKEN	024c72cc-2477-4c84-b2a9-265751ff65dc	2026-09-01 12:34:51.904149	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
620	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:34:51.997449	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
621	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:35:24.571683	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
623	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:35:41.703604	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
625	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:35:46.20308	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
627	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:35:50.362779	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
631	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:35:54.328735	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
632	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:36:14.485819	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
633	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:36:14.518554	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
636	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:36:16.984738	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
646	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:36:51.460509	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
624	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:35:45.138208	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
626	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:35:49.962467	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
628	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:35:50.710648	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
630	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:35:51.505483	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
639	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:36:40.661058	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
643	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:36:42.752602	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
645	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:36:46.057413	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
622	Codigo OTP validado, sesion c09c371a... (j***@gmail.com)	FIRMA_TOKEN	024c72cc-2477-4c84-b2a9-265751ff65dc	2026-09-01 12:35:41.646035	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/otp/validar	OTP_VALIDADO	\N	PORTAL_FIRMA
629	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:35:51.132249	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
634	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:36:15.654793	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
635	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:36:15.747246	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
637	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:36:17.004339	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
640	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:36:41.868199	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
642	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:36:42.459537	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
644	El firmante visualizo el PDF del acta	ACTA	119	2026-09-01 12:36:43.001591	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
648	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-01 12:37:24.89136	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
649	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:37:30.25838	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
650	Evidencia FIRMA (firma_119.png) visualizada	ACTA	119	2026-09-01 12:37:30.286751	0:0:0:0:0:0:0:1	/actas/119/firma	EVIDENCIA_VISTA	1	admin
651	Evidencia FOTO (foto_119.jpg) visualizada	ACTA	119	2026-09-01 12:37:30.286751	0:0:0:0:0:0:0:1	/actas/119/foto	EVIDENCIA_VISTA	1	admin
653	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:37:41.641918	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
654	Evidencia FIRMA (firma_119.png) visualizada	ACTA	119	2026-09-01 12:37:44.283315	0:0:0:0:0:0:0:1	/actas/119/firma	EVIDENCIA_VISTA	1	admin
655	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:37:44.283315	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
656	Evidencia FOTO (foto_119.jpg) visualizada	ACTA	119	2026-09-01 12:37:44.285317	0:0:0:0:0:0:0:1	/actas/119/foto	EVIDENCIA_VISTA	1	admin
657	Evidencia FOTO (foto_119.jpg) visualizada	ACTA	119	2026-09-01 12:37:47.929087	0:0:0:0:0:0:0:1	/actas/119/foto	EVIDENCIA_VISTA	1	admin
658	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:37:47.934194	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
659	Evidencia FIRMA (firma_119.png) visualizada	ACTA	119	2026-09-01 12:37:47.934194	0:0:0:0:0:0:0:1	/actas/119/firma	EVIDENCIA_VISTA	1	admin
661	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:37:52.67513	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
663	Evidencia FIRMA (firma_119.png) visualizada	ACTA	119	2026-09-01 12:37:52.678203	0:0:0:0:0:0:0:1	/actas/119/firma	EVIDENCIA_VISTA	1	admin
662	Evidencia FOTO (foto_119.jpg) visualizada	ACTA	119	2026-09-01 12:37:52.678203	0:0:0:0:0:0:0:1	/actas/119/foto	EVIDENCIA_VISTA	1	admin
664	Evidencia FIRMA (firma_119.png) visualizada	ACTA	119	2026-09-01 12:37:53.068852	0:0:0:0:0:0:0:1	/actas/119/firma	EVIDENCIA_VISTA	1	admin
665	Evidencia FOTO (foto_119.jpg) visualizada	ACTA	119	2026-09-01 12:37:53.068852	0:0:0:0:0:0:0:1	/actas/119/foto	EVIDENCIA_VISTA	1	admin
666	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:37:53.068852	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
667	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:37:54.239722	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
668	Evidencia FIRMA (firma_119.png) visualizada	ACTA	119	2026-09-01 12:37:54.239722	0:0:0:0:0:0:0:1	/actas/119/firma	EVIDENCIA_VISTA	1	admin
669	Evidencia FOTO (foto_119.jpg) visualizada	ACTA	119	2026-09-01 12:37:54.253241	0:0:0:0:0:0:0:1	/actas/119/foto	EVIDENCIA_VISTA	1	admin
671	PDF del acta visualizado/descargado	ACTA	119	2026-09-01 12:38:11.199686	0:0:0:0:0:0:0:1	/actas/119/pdf	DOCUMENTO_VISTO	1	admin
608	PDF del checklist visualizado/descargado	ACTA	119	2026-09-01 12:31:49.594913	0:0:0:0:0:0:0:1	/actas/119/checklist/pdf	DOCUMENTO_VISTO	1	admin
610	PDF del checklist visualizado/descargado	ACTA	119	2026-09-01 12:32:06.180687	0:0:0:0:0:0:0:1	/actas/119/checklist/pdf	DOCUMENTO_VISTO	1	admin
617	PDF del checklist visualizado/descargado	ACTA	119	2026-09-01 12:34:19.438706	0:0:0:0:0:0:0:1	/actas/119/checklist/pdf	DOCUMENTO_VISTO	1	admin
638	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:36:37.1295	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
641	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:36:42.226353	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
647	El firmante visualizo el PDF del checklist de entrega	ACTA	119	2026-09-01 12:36:52.193242	0:0:0:0:0:0:0:1	/firma/024c72cc-2477-4c84-b2a9-265751ff65dc/checklist/pdf	DOCUMENTO_VISTO	\N	PORTAL_FIRMA
652	PDF del checklist visualizado/descargado	ACTA	119	2026-09-01 12:37:38.602981	0:0:0:0:0:0:0:1	/actas/119/checklist/pdf	DOCUMENTO_VISTO	1	admin
660	PDF del checklist visualizado/descargado	ACTA	119	2026-09-01 12:37:51.425527	0:0:0:0:0:0:0:1	/actas/119/checklist/pdf	DOCUMENTO_VISTO	1	admin
670	PDF del checklist visualizado/descargado	ACTA	119	2026-09-01 12:37:58.499747	0:0:0:0:0:0:0:1	/actas/119/checklist/pdf	DOCUMENTO_VISTO	1	admin
672	PDF del checklist visualizado/descargado	ACTA	119	2026-09-01 12:38:19.383251	0:0:0:0:0:0:0:1	/actas/119/checklist/pdf	DOCUMENTO_VISTO	1	admin
673	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:24.387607	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
674	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:24.75741	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
675	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:25.004501	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
676	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:25.208082	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
677	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:25.424185	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
678	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:25.639395	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
679	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:25.872196	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
680	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:26.079859	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
681	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:26.286559	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
682	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:26.493416	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
683	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:26.707379	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
684	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:26.916525	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
685	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:27.121745	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
686	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:27.338104	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
687	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:27.553811	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
688	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:27.756304	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
689	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:27.995428	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
690	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:28.234183	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
691	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:28.475864	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
692	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:45:28.696321	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	noexiste_xyz
693	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:47:05.819898	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	usuario_que_no_existe_xyz
694	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:47:06.254838	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	usuario_que_no_existe_xyz
695	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 08:47:06.700332	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	usuario_que_no_existe_xyz
696	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 09:38:15.636491	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	admin
697	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 09:38:16.534722	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	admin
698	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 09:38:17.056349	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	admin
699	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 09:39:11.60405	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	juan
700	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 09:39:12.400282	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	tecnico
701	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:09:09.713829	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
702	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:09:10.015926	127.0.0.1	/auth/login	LOGIN_EXITOSO	42	sec011_ad
703	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:09:10.17513	127.0.0.1	/auth/login	LOGIN_EXITOSO	43	sec011_au
704	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:18:21.409931	127.0.0.1	/auth/login	LOGIN_EXITOSO	44	reauditsec13
705	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:21:52.816453	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
706	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:21:53.157163	127.0.0.1	/auth/login	LOGIN_EXITOSO	42	sec011_ad
707	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:21:53.313677	127.0.0.1	/auth/login	LOGIN_EXITOSO	43	sec011_au
708	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:25:54.223064	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
709	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:25:54.579732	127.0.0.1	/auth/login	LOGIN_EXITOSO	42	sec011_ad
710	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:25:54.739061	127.0.0.1	/auth/login	LOGIN_EXITOSO	43	sec011_au
711	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:26:22.22294	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
712	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:27:01.0536	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
713	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:27:01.472208	127.0.0.1	/auth/login	LOGIN_EXITOSO	42	sec011_ad
714	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:27:01.71503	127.0.0.1	/auth/login	LOGIN_EXITOSO	43	sec011_au
715	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:30:36.489171	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
716	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:30:36.798563	127.0.0.1	/auth/login	LOGIN_EXITOSO	42	sec011_ad
717	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:30:36.981788	127.0.0.1	/auth/login	LOGIN_EXITOSO	43	sec011_au
718	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:33:08.736862	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
719	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:34:38.305952	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
720	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:34:38.548816	127.0.0.1	/auth/login	LOGIN_EXITOSO	42	sec011_ad
721	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:34:38.712299	127.0.0.1	/auth/login	LOGIN_EXITOSO	43	sec011_au
722	Codigo OTP emitido para firma_token id=88	FIRMA_TOKEN	cb0e4b76-c719-4bce-8c95-f41eec3be0dc	2026-09-02 10:34:39.02605	127.0.0.1	/firma/cb0e4b76-c719-4bce-8c95-f41eec3be0dc/otp	OTP_GENERADO	\N	PORTAL_FIRMA
723	Correo OTP enviado a c***@test.local	FIRMA_TOKEN	cb0e4b76-c719-4bce-8c95-f41eec3be0dc	2026-09-02 10:34:41.567223	127.0.0.1	/firma/cb0e4b76-c719-4bce-8c95-f41eec3be0dc/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
724	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:35:06.424849	127.0.0.1	/auth/login	LOGIN_EXITOSO	43	sec011_au
725	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:35:41.808927	127.0.0.1	/auth/login	LOGIN_EXITOSO	41	sec011_tc
726	Cierre de sesion	AUTENTICACION	\N	2026-09-02 10:35:41.951339	127.0.0.1	/auth/logout	LOGOUT	41	sec011_tc
727	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-02 10:37:09.595301	0:0:0:0:0:0:0:1	/auth/login	LOGIN_FALLIDO	\N	admin
728	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 10:37:13.050671	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
729	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 11:32:30.657229	127.0.0.1	/auth/login	LOGIN_EXITOSO	45	perfmeas09
730	Access Denied	RECURSO	/generar-acta	2026-09-02 11:34:03.214026	127.0.0.1	/generar-acta	ACCESO_DENEGADO	\N	\N
731	Access Denied	RECURSO	/generar-acta	2026-09-02 11:34:03.419948	127.0.0.1	/generar-acta	ACCESO_DENEGADO	\N	\N
732	Access Denied	RECURSO	/generar-acta	2026-09-02 11:34:03.619805	127.0.0.1	/generar-acta	ACCESO_DENEGADO	\N	\N
733	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 16:59:14.097142	127.0.0.1	/auth/login	LOGIN_EXITOSO	47	fase1t_86353294
734	ZIP del acta descargado	ACTA	131	2026-09-02 17:00:15.14899	127.0.0.1	/actas/131/zip	DOCUMENTO_VISTO	47	fase1t_86353294
735	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-02 17:01:27.783468	127.0.0.1	/auth/login	LOGIN_EXITOSO	47	fase1t_86353294
736	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-03 07:55:14.591881	0:0:0:0:0:0:0:1	/auth/login	LOGIN_EXITOSO	1	admin
737	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 07:58:32.454204	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
738	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:00:39.951411	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
739	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:00:40.860911	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
740	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:01:06.796307	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
741	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:01:07.342477	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
742	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:01:39.621806	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
743	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:02:01.109344	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
744	PDF del acta visualizado/descargado	ACTA	132	2026-09-03 08:08:27.665739	0:0:0:0:0:0:0:1	/actas/132/pdf	DOCUMENTO_VISTO	1	admin
745	PDF del acta visualizado/descargado	ACTA	131	2026-09-03 08:08:37.390261	0:0:0:0:0:0:0:1	/actas/131/pdf	DOCUMENTO_VISTO	1	admin
746	Checklist de Entrega visualizado/descargado (documento asociado del expediente)	ACTA	131	2026-09-03 08:09:06.955728	0:0:0:0:0:0:0:1	/actas/131/checklist/pdf	DOCUMENTO_VISTO	1	admin
747	PDF del acta visualizado/descargado	ACTA	131	2026-09-03 08:09:07.79058	0:0:0:0:0:0:0:1	/actas/131/pdf	DOCUMENTO_VISTO	1	admin
748	Checklist de Entrega visualizado/descargado (documento asociado del expediente)	ACTA	131	2026-09-03 08:09:16.89825	0:0:0:0:0:0:0:1	/actas/131/checklist/pdf	DOCUMENTO_VISTO	1	admin
749	PDF del acta visualizado/descargado	ACTA	131	2026-09-03 08:09:18.108184	0:0:0:0:0:0:0:1	/actas/131/pdf	DOCUMENTO_VISTO	1	admin
750	Checklist de Entrega visualizado/descargado (documento asociado del expediente)	ACTA	131	2026-09-03 08:09:23.882578	0:0:0:0:0:0:0:1	/actas/131/checklist/pdf	DOCUMENTO_VISTO	1	admin
751	Checklist de Entrega visualizado/descargado (documento asociado del expediente)	ACTA	131	2026-09-03 08:09:33.449982	0:0:0:0:0:0:0:1	/actas/131/checklist/pdf	DOCUMENTO_VISTO	1	admin
752	PDF del acta visualizado/descargado	ACTA	131	2026-09-03 08:09:34.472737	0:0:0:0:0:0:0:1	/actas/131/pdf	DOCUMENTO_VISTO	1	admin
753	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:09:52.92239	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
754	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:10:47.294162	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
755	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:10:47.952461	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
756	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:15:41.275958	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
757	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:15:42.3472	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
758	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:15:50.185356	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
759	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:16:59.713595	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
760	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:17:05.561606	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
761	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:17:06.384255	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
762	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-03 08:21:42.592386	127.0.0.1	/auth/login	LOGIN_EXITOSO	49	form_41701453
763	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:21:43.450971	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
764	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:21:45.829468	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
765	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:21:46.647562	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
766	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:21:50.381267	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
767	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:21:53.449913	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
768	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:21:59.118403	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
769	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:00.515243	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
770	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:04.22623	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
771	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:04.538074	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
772	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:07.859114	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
773	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:08.918936	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
774	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:11.732198	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
775	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:14.063559	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
776	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:15.038955	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
777	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:15.889975	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
778	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:16.88094	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
781	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:23.259444	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
782	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:25.361877	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
785	Inicio de sesion exitoso	AUTENTICACION	\N	2026-09-03 08:24:27.140264	127.0.0.1	/auth/login	LOGIN_EXITOSO	49	form_41701453
786	Codigo OTP emitido para firma_token id=89	FIRMA_TOKEN	37da3177-3ed0-4c82-bbdc-c13976b63a04	2026-09-03 08:24:27.32932	127.0.0.1	/firma/37da3177-3ed0-4c82-bbdc-c13976b63a04/otp	OTP_GENERADO	\N	PORTAL_FIRMA
787	Correo OTP enviado a m***@test.local	FIRMA_TOKEN	37da3177-3ed0-4c82-bbdc-c13976b63a04	2026-09-03 08:24:29.191741	127.0.0.1	/firma/37da3177-3ed0-4c82-bbdc-c13976b63a04/otp	OTP_ENVIADO	\N	PORTAL_FIRMA
779	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:18.898256	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
780	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:22.458403	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
783	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 08:22:26.305854	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
784	Intento de autenticacion fallido	AUTENTICACION	\N	2026-09-03 08:23:39.182823	127.0.0.1	/auth/login	LOGIN_FALLIDO	\N	form_X
788	PDF del acta visualizado/descargado	ACTA	133	2026-09-03 09:18:08.587333	0:0:0:0:0:0:0:1	/actas/133/pdf	DOCUMENTO_VISTO	1	admin
\.


--
-- Data for Name: dispositivo; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dispositivo (id_dispositivo, numero_serie, numero_placa, descripcion, estado, id_marca) FROM stdin;
\.


--
-- Data for Name: evidencia; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.evidencia (id_evidencia, id_acta, tipo, ruta_archivo, fecha_creacion) FROM stdin;
1	1	FIRMA	uploads\\firmas\\firma_1.png	2026-07-27 15:23:17.443821
2	1	FOTO	uploads\\fotos\\foto_1.jpg	2026-07-27 15:23:17.448854
3	1	PDF_FINAL	C:\\Users\\juanhern\\OneDrive - COMPANIA DE FINANCIAMIENTO COMERCIAL COLTEFINANCIERA S.A\\Documentos\\actas-glpi-Suaco\\backend\\uploads\\pdf\\acta_1.pdf	2026-07-28 08:04:16.846122
7	9	FIRMA	C:\\Users\\juanhern\\AppData\\Local\\Temp\\actas_glpi_uploads\\firmas\\firma_9.png	2026-07-30 08:55:38.048558
8	9	FOTO	C:\\Users\\juanhern\\AppData\\Local\\Temp\\actas_glpi_uploads\\fotos\\foto_9.jpg	2026-07-30 08:55:38.081585
9	10	FIRMA	uploads/firmas/firma_10.png	2026-07-30 09:25:10.107538
10	10	FOTO	uploads/fotos/foto_10.jpg	2026-07-30 09:25:10.115704
11	10	PDF_FINAL	uploads/pdf/acta_10.pdf	2026-07-30 09:27:17.086318
12	14	FIRMA	uploads/firmas/firma_14.png	2026-07-30 12:15:14.217227
13	14	FOTO	uploads/fotos/foto_14.jpg	2026-07-30 12:15:14.23315
14	14	PDF_FINAL	uploads/pdf/acta_14.pdf	2026-07-30 12:15:24.600904
15	15	FIRMA	uploads/firmas/firma_15.png	2026-07-30 12:33:17.361178
16	15	FOTO	uploads/fotos/foto_15.jpg	2026-07-30 12:33:17.369493
17	15	PDF_FINAL	uploads/pdf/ActaEntrega_JK16ZW3_yyyy.pdf	2026-07-30 12:33:25.92194
18	17	FIRMA	uploads/firmas/firma_17.png	2026-07-30 15:01:05.623036
19	17	FOTO	uploads/fotos/foto_17.jpg	2026-07-30 15:01:05.623036
20	17	PDF_FINAL	uploads/pdf/ActaEntrega_JK16ZW3_nnnn.pdf	2026-07-30 15:01:11.156071
21	18	FIRMA	uploads/firmas/firma_18.png	2026-07-30 15:07:10.392772
22	18	FOTO	uploads/fotos/foto_18.jpg	2026-07-30 15:07:10.395731
23	18	PDF_FINAL	uploads/pdf/ActaEntrega_DYYCQM3_uuuu.pdf	2026-07-30 15:07:16.068548
24	19	FIRMA	uploads/firmas/firma_19.png	2026-07-30 15:11:56.720954
25	19	FOTO	uploads/fotos/foto_19.jpg	2026-07-30 15:11:56.728158
26	19	PDF_FINAL	uploads/pdf/ActaEntrega_123_gggg.pdf	2026-07-30 15:12:03.036416
27	20	FIRMA	uploads/firmas/firma_20.png	2026-07-31 07:40:54.035757
28	20	FOTO	uploads/fotos/foto_20.jpg	2026-07-31 07:40:54.035757
29	20	PDF_FINAL	uploads/pdf/ActaEntrega_DYYCQM3_actaentrega.pdf	2026-07-31 07:41:09.095398
30	21	FIRMA	uploads/firmas/firma_21.png	2026-07-31 07:41:49.015403
31	21	FOTO	uploads/fotos/foto_21.jpg	2026-07-31 07:41:49.015403
32	21	PDF_FINAL	uploads/pdf/Devolucion_DYYCQM3_actadevolucion.pdf	2026-07-31 07:41:59.960893
33	22	FIRMA	uploads/firmas/firma_22.png	2026-07-31 07:56:00.330689
34	22	FOTO	uploads/fotos/foto_22.jpg	2026-07-31 07:56:00.331773
35	22	PDF_FINAL	uploads/pdf/Devolucion_DYYCQM3_tttt.pdf	2026-07-31 07:56:12.606311
36	23	FIRMA	uploads/firmas/firma_23.png	2026-07-31 08:11:34.579194
37	23	FOTO	uploads/fotos/foto_23.jpg	2026-07-31 08:11:34.588273
38	23	PDF_FINAL	uploads/pdf/Devolucion_DYYCQM3_rrrrrrrrrr.pdf	2026-07-31 08:12:09.135283
39	25	FIRMA	uploads/firmas/firma_25.png	2026-07-31 09:10:11.651864
40	25	FOTO	uploads/fotos/foto_25.jpg	2026-07-31 09:10:11.654881
41	25	PDF_FINAL	uploads/pdf/Devolucion_30JZTN3_wwww.pdf	2026-07-31 09:10:35.646164
42	26	FIRMA	uploads/firmas/firma_26.png	2026-07-31 09:18:31.679836
43	26	FOTO	uploads/fotos/foto_26.jpg	2026-07-31 09:18:31.683655
44	26	PDF_FINAL	uploads/pdf/ActaEntrega_FM16ZW3_NuevoUsuario.pdf	2026-07-31 09:18:54.192754
45	27	FIRMA	uploads/firmas/firma_27.png	2026-07-31 09:22:50.039886
46	27	FOTO	uploads/fotos/foto_27.jpg	2026-07-31 09:22:50.039886
47	27	PDF_FINAL	uploads/pdf/Devolucion_CP16ZW3_Reparacion.pdf	2026-07-31 09:23:16.449969
52	32	FIRMA	uploads/firmas/firma_32.png	2026-07-31 10:44:20.13406
53	32	FOTO	uploads/fotos/foto_32.jpg	2026-07-31 10:44:20.151086
54	32	PDF_FINAL	uploads/pdf/ActaEntrega_CCV3F33_qqqq.pdf	2026-07-31 10:45:00.116123
60	36	FIRMA	uploads/firmas/firma_36.png	2026-07-31 11:19:53.74342
61	36	FOTO	uploads/fotos/foto_36.jpg	2026-07-31 11:19:53.750653
62	36	PDF_FINAL	uploads/pdf/Devolucion_30JZTN3_kkkk.pdf	2026-07-31 11:20:30.618449
72	37	FIRMA	uploads/firmas/firma_37.png	2026-07-31 15:16:27.045229
73	37	FOTO	uploads/fotos/foto_37.jpg	2026-07-31 15:16:27.070206
74	37	PDF_FINAL	uploads/pdf/ActaEntrega_123_ffff.pdf	2026-07-31 15:17:13.318323
82	38	FIRMA	uploads/firmas/firma_38.png	2026-07-31 16:19:03.249967
83	38	FOTO	uploads/fotos/foto_38.jpg	2026-07-31 16:19:03.253845
84	38	PDF_FINAL	uploads/pdf/ActaEntrega_123_ssss.pdf	2026-07-31 16:19:43.114653
85	41	FIRMA	uploads/firmas/firma_41.png	2026-08-04 14:17:55.972379
86	41	FOTO	uploads/fotos/foto_41.jpg	2026-08-04 14:17:55.994688
87	41	PDF_FINAL	uploads/pdf/ActaEntrega_123_NuevoUsuario.pdf	2026-08-04 14:18:13.209322
88	50	FIRMA	uploads/firmas/firma_50.png	2026-08-21 10:17:50.437869
89	50	FOTO	uploads/fotos/foto_50.jpg	2026-08-21 10:17:50.446108
90	50	PDF_FINAL	uploads/pdf/Devolucion_123_t.pdf	2026-08-21 10:18:32.941377
99	74	FIRMA	uploads/firmas/firma_74.png	2026-08-25 08:34:06.498491
100	74	FOTO	uploads/fotos/foto_74.jpg	2026-08-25 08:34:06.505189
101	74	PDF_FINAL	uploads/pdf/Devolucion_123_a.pdf	2026-08-25 08:34:33.431623
102	70	FIRMA	uploads/firmas/firma_70.png	2026-08-25 08:48:24.050701
103	70	FOTO	uploads/fotos/foto_70.jpg	2026-08-25 08:48:24.055845
104	70	PDF_FINAL	uploads/pdf/Devolucion_123_a.pdf	2026-08-25 08:48:38.588322
105	74	FIRMA	uploads/firmas/firma_74.png	2026-08-25 09:42:03.031692
106	74	FOTO	uploads/fotos/foto_74.jpg	2026-08-25 09:42:03.060205
107	74	PDF_FINAL	uploads/pdf/Devolucion_123_a_acta74.pdf	2026-08-25 09:42:23.221118
108	75	FIRMA	uploads/firmas/firma_75.png	2026-08-25 10:01:52.90221
109	75	FOTO	uploads/fotos/foto_75.jpg	2026-08-25 10:01:52.90704
110	75	PDF_FINAL	uploads/pdf/Devolucion_5CD2256W6H_aaaa_acta75.pdf	2026-08-25 10:02:06.427025
111	57	FIRMA	uploads/firmas/firma_57.png	2026-08-25 10:08:44.97596
112	57	FOTO	uploads/fotos/foto_57.jpg	2026-08-25 10:08:44.980456
113	57	PDF_FINAL	uploads/pdf/Devolucion_123_a_acta57.pdf	2026-08-25 10:09:02.686366
114	76	FIRMA	uploads/firmas/firma_76.png	2026-08-25 10:17:53.477075
115	76	FOTO	uploads/fotos/foto_76.jpg	2026-08-25 10:17:53.483251
116	76	PDF_FINAL	uploads/pdf/Devolucion_4K16ZW3_a_acta76.pdf	2026-08-25 10:18:10.259782
117	77	FIRMA	uploads/firmas/firma_77.png	2026-08-25 10:23:30.28721
118	77	FOTO	uploads/fotos/foto_77.jpg	2026-08-25 10:23:30.288447
119	77	PDF_FINAL	uploads/pdf/Devolucion_4K16ZW3_aa_acta77.pdf	2026-08-25 10:23:40.323237
120	68	FIRMA	uploads/firmas/firma_68.png	2026-08-25 11:09:02.735419
121	68	FOTO	uploads/fotos/foto_68.jpg	2026-08-25 11:09:02.748087
122	78	FIRMA	uploads/firmas/firma_78.png	2026-08-25 11:18:47.139029
123	78	FOTO	uploads/fotos/foto_78.jpg	2026-08-25 11:18:47.147765
124	78	PDF_FINAL	uploads/pdf/Devolucion_1PG15N3_aaaaaaaaaaaaaaaa_acta78.pdf	2026-08-25 11:19:06.939714
125	97	FIRMA	uploads/firmas/firma_97.png	2026-08-27 11:51:24.622425
126	97	FOTO	uploads/fotos/foto_97.jpg	2026-08-27 11:51:24.630984
127	98	FIRMA	uploads/firmas/firma_98.png	2026-08-27 12:10:03.455493
128	98	FOTO	uploads/fotos/foto_98.jpg	2026-08-27 12:10:03.463746
129	98	PDF_FINAL	uploads/pdf/FormateoSeguro_123_aaaaa_acta98.pdf	2026-08-27 12:10:34.737042
131	119	FIRMA	uploads/firmas/firma_119.png	2026-09-01 12:36:57.08745
132	119	FOTO	uploads/fotos/foto_119.jpg	2026-09-01 12:36:57.093441
133	119	PDF_FINAL	uploads/pdf/ActaEntrega_123_Entrega Nuevo Equipo_eb06eacf_acta119.pdf	2026-09-01 12:37:17.937544
134	119	CHECKLIST_FINAL	uploads/pdf/Checklist_123_Entrega Nuevo Equipo_49f719ed_checklist119.pdf	2026-09-01 12:37:53.880373
\.


--
-- Data for Name: firma_otp; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.firma_otp (id_otp, codigo_hash, correo_destino, fecha_creacion, fecha_expiracion, fecha_validacion, id_token_firma, intentos, sesion, usado) FROM stdin;
47	$2a$10$SwDRfOQDtoKf75mbsfSFQu7UXQMA73YOH8CTRk8T1yogcQ84MG3Qq	juanhernandez1122876@gmail.com	2026-08-31 08:48:15.067738	2026-08-31 08:58:15.063612	\N	83	0	\N	f
23	$2a$10$Sww4QoI2XRukJc2H.ABR/.i2WmH19lO7d/rBV/oAGVMnAW/WsPEya	juanhernandez1122876@gmail.com	2026-08-25 09:59:53.101412	2026-08-25 10:09:53.098937	2026-08-25 10:01:04.176002	63	1	105890cc-e41b-4f7e-91e4-11dec55147f2	t
24	x	razortxz@gmail.com	2026-08-25 10:07:45.859656	2026-08-25 10:17:45.859656	2026-08-25 10:07:45.859656	45	0	trazaAAA	t
8	$2a$10$n.WnxZlYhPDm7c6NeD4sYedxd74n9988pOKYOOcm71xxoID2AvAOu	razortxz@gmail.com	2026-08-24 11:51:16.214442	2026-08-24 12:01:16.207736	\N	42	0	\N	t
9	$2a$10$r17oIep6Y9RqYeTL290EfeqA9EtWgCPqTpx6lv.M7TlAQPojp2LwO	razortxz@gmail.com	2026-08-24 11:51:42.155411	2026-08-24 12:01:42.148041	\N	42	0	\N	f
25	$2a$10$m5Vuz7g6J61XVKkfsGqugeILWAV/0qU1FDiSo5p0nwaLy/IdXumIe	juanhernandez1122876@gmail.com	2026-08-25 10:17:08.050877	2026-08-25 10:27:08.050877	2026-08-25 10:17:31.933857	64	0	ea6e4335-a71c-41db-b43e-af24f5591152	t
26	$2a$10$5AyLYN38OSj5SQfVGeZVY.S0LU4x5kIXh1PfhIBwxXN1yKlwiiUj2	juanhernandez1122876@gmail.com	2026-08-25 10:22:44.33116	2026-08-25 10:32:44.33116	2026-08-25 10:23:09.521043	65	0	35963bf5-14e5-4a20-b867-3668d453f9bc	t
27	$2b$10$AnweHPtu5XWoGHE6/kkeUO9YpCqXd8lhkmteFP76uW41fL687sKYK	JuanHernandez@coltefinanciera.com.co	2026-08-25 10:42:35.781265	2026-08-25 10:54:35.781265	2026-08-25 10:52:02.967023	57	0	811f3a08-3be8-4533-816d-211ce4615bb2	t
28	$2b$10$AyoeeyC7sxcGrC5KI1Hvw.g4U39Cd2xOtY74trDlm9TT0MUxkoBoO	razortxz@gmail.com	2026-08-25 10:46:30.807945	2026-08-25 11:06:30.807945	2026-08-25 10:57:41.018626	56	0	09477448-7ebc-4e64-8e74-5c8cc195d28d	t
29	$2b$10$zKw56S/Qc47BCbruhkKW6.Q6oHaS00bqyd5VIMaTXacbdb5nm4g7m	razortxz@gmail.com	2026-08-25 10:56:49.26922	2026-08-25 11:16:49.26922	2026-08-25 11:09:00.771956	56	0	6e42373c-9cb7-493d-8f72-014ff97486c7	t
15	$2a$10$1/Pf0u58z71YVe8ea9J/ZuZ7gV1q/MgFujqVYrh0X3//2ISkLMCsW	razortxz@gmail.com	2026-08-24 16:00:09.66842	2026-08-24 16:10:09.66842	2026-08-24 16:04:38.272086	56	0	2de5364f-7407-48d3-9044-c69a9c61a00c	t
16	$2a$10$2p0KOavkTK/z4WNcc6GfI.limW5YGn0FE8KsvVpnHtxmxpH9682ze	JuanHernandez@coltefinanciera.com.co	2026-08-24 16:29:09.064386	2026-08-24 16:39:09.064386	\N	57	0	\N	f
17	$2a$10$ggAYbRr6RtnVfgfLffvLOOFBKwH7qvG0dL.YndYOZY4h0YbMh75X.	juanhernandez1122876@gmail.com	2026-08-24 16:32:04.565773	2026-08-24 16:42:04.564573	2026-08-25 08:46:20.377149	58	0	testsesionAAA	t
22	x	juanhernandez1122876@gmail.com	2026-08-25 09:30:51.582627	2026-08-25 09:40:51.582627	2026-08-25 09:30:51.582627	62	0	restore74	t
30	$2a$10$/o5GJrqM2GK06FbsogYcEOVN54CHo1dLjOlbVHV6s6b3moIy26kKe	juanhernandez1122876@gmail.com	2026-08-25 11:18:01.512374	2026-08-25 11:28:01.488785	2026-08-25 11:18:27.729764	66	0	67ef571d-a5f7-4c3a-beda-af513df9c33f	t
31	$2a$10$csyPpWLaU8NTyBQOmhVK/ecBMabyBZ5Y9beNJO2DM6b3T4wDeYmE.	juanhernandez1122876@gmail.com	2026-08-25 16:46:56.162987	2026-08-25 16:56:56.155313	2026-08-25 16:47:30.235504	67	0	001716ae-afb8-4cdb-af6b-94da82078796	t
32	$2a$10$zWwC.ZP7BYvJ6IvlR1B35.JWne4FuC/Li0COrnF3sJhpHKwKZdqmu	juanhernandez1122876@gmail.com	2026-08-27 11:50:12.156614	2026-08-27 12:00:12.152623	2026-08-27 11:50:52.874756	68	0	cf6c7cba-ad84-48e2-b8b6-197a98215cad	t
33	$2a$10$yGBQJ6cGQPPh5epO9CHgXO.e96O0Cl7ewOzBt2PEkjpM/TTfrC.iG	juanhernandez1122876@gmail.com	2026-08-27 12:09:08.978249	2026-08-27 12:19:08.968072	2026-08-27 12:09:28.399746	69	0	b97ca181-1bb0-4720-b283-827f053e8ce5	t
34	$2a$10$leZicTqNC7OW0dNPJQTXYO9WNCFfvPgUiANbtyhPIQhSFZHN0nWia	destino@test.com	2026-08-27 14:50:05.004613	2026-08-27 15:00:05.002665	\N	70	0	\N	f
35	$2a$10$QmneeOxNpsnwW2l0UgezKOGAoyrSxqVR39BGjFBi6uVKSDWT5ncJK	juanhernandez1122876@gmail.com	2026-08-27 16:53:19.566701	2026-08-27 17:03:19.56354	\N	71	0	\N	f
36	$2a$10$kK9IJeoNjPuM/UdLmxlo7.hOokq.82NU7yRSczXnDy8kY1Ejvk/Ue	juan.perez@coltefinanciera.com	2026-08-27 16:58:53.786139	2026-08-27 17:08:53.78262	\N	72	0	\N	f
37	$2a$10$uzSbXxwf6s97Y9zkwrx.oe4vhorR2hxOBp5O4j6V0dQ6dh9QvlbK6	juanhernandez1122876@gmail.com	2026-08-27 17:03:19.385425	2026-08-27 17:13:19.384421	\N	73	0	\N	f
38	$2a$10$12XwxM3A.P9UND8hmuZXYuLvIAWbSQxFw9US2yA0/miA1.ZZGBuIS	juanhernandez1122876@gmail.com	2026-08-31 08:03:20.891355	2026-08-31 08:13:20.882545	\N	74	0	\N	f
48	$2a$10$UD9DRTrqJ1ojr28Au5mu7O1vHkJAovWlz7CvB3S2RZqXUd4eVV74G	juanhernandez1122876@gmail.com	2026-08-31 09:11:46.660414	2026-08-31 09:21:46.652308	\N	84	0	\N	f
49	$2a$10$K4YhEiaU98rXViecmxsyDuIwcI0IUXNzB3GRzVnRLjO1h4jMqS/.K	maria.lopez@empresa.com	2026-08-31 12:29:09.509051	2026-08-31 12:39:09.506738	\N	86	0	\N	f
50	$2a$10$7Y4AYByg1bWlfSiOn/oZAuXTQlfAYbhHYLyHij6N35TpRvHfIReH6	juanhernandez1122876@gmail.com	2026-09-01 12:34:49.513169	2026-09-01 12:44:49.507561	2026-09-01 12:35:41.637808	87	0	c09c371a-c8c1-4493-86ab-ba1ab70774a7	t
51	$2a$10$8IWrgr.kQyExkn45rXOczOngpejCfdriQcIRra9OK8D7znniMzfFq	cperez@test.local	2026-09-02 10:34:39.017616	2026-09-02 10:44:39.013087	\N	88	0	\N	f
52	$2a$10$HwMcrTD7.9GVB.iNGgMVIO4gUmxR9KmLe5hh.RBtfVEZzrqsYFTle	maria.usuario@test.local	2026-09-03 08:24:27.32932	2026-09-03 08:34:27.305306	\N	89	0	\N	f
\.


--
-- Data for Name: firma_token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.firma_token (id_token, id_acta, token, utilizado, fecha_creacion, fecha_utilizacion, fecha_expiracion) FROM stdin;
1	1	7d61a3a3-c038-43e5-864e-b17012ca56a2	t	2026-07-27 15:20:30.593902	2026-07-27 15:23:17.455937	\N
69	98	77a95c3a-a46f-42f8-8826-48fb1b3121ee	t	2026-08-27 12:09:08.592416	2026-08-27 12:10:03.471936	2026-08-30 12:09:08.592416
4	9	ee9e174e-2c62-4cc4-aad2-b36af200dce5	t	2026-07-30 08:53:48.857107	2026-07-30 08:55:38.082814	\N
5	10	572bb120-a328-4958-8616-1d47efa33ed4	t	2026-07-30 09:24:24.672926	2026-07-30 09:25:10.115704	\N
6	14	dad2b895-6ff8-4497-b196-797b87ef38a3	t	2026-07-30 12:14:26.796024	2026-07-30 12:15:14.23315	\N
7	15	b0e622e5-260b-4e65-bf77-fb8164a926d5	t	2026-07-30 12:33:00.557091	2026-07-30 12:33:17.37151	\N
8	17	d2a86c8e-c6eb-4017-a794-4ae7cadf505b	t	2026-07-30 15:00:39.608784	2026-07-30 15:01:05.623036	\N
9	18	9686cfd7-bce1-4453-aac6-9945690d70fd	t	2026-07-30 15:06:42.994568	2026-07-30 15:07:10.395731	\N
10	19	c7518a9a-1b10-4590-a46f-4d201ef70656	t	2026-07-30 15:11:29.715114	2026-07-30 15:11:56.729678	\N
11	20	c63c1691-2ed7-4e19-bc0a-d97f66728705	t	2026-07-31 07:39:56.159135	2026-07-31 07:40:54.035757	\N
12	21	2e683435-8611-4acd-ad99-723e0afa9c25	t	2026-07-31 07:39:58.543225	2026-07-31 07:41:49.020383	\N
13	22	9ea8773a-c833-4508-a130-7757bace88e5	t	2026-07-31 07:55:35.08794	2026-07-31 07:56:00.331773	\N
14	23	bfbce981-5d9c-4baa-80cf-389c9a18206e	t	2026-07-31 08:10:48.732797	2026-07-31 08:11:34.595889	\N
15	24	cdf0ccc9-875f-455c-b89d-f012772cb957	t	2026-07-31 09:03:52.52568	2026-07-31 09:06:45.671764	\N
16	25	aca7f0ea-067e-4f73-b45c-b1e5a045e6c2	t	2026-07-31 09:08:17.962033	2026-07-31 09:10:11.658204	\N
17	26	5b887cf8-c008-45f1-b4b3-f0a2deb0b6a7	t	2026-07-31 09:16:17.150325	2026-07-31 09:18:31.68466	\N
18	27	e8563961-9ab6-466c-bbb1-46ca4912708c	t	2026-07-31 09:21:42.81395	2026-07-31 09:22:50.039886	\N
42	51	6160669f-edd3-4b41-9543-71bb9c927380	f	2026-08-21 10:55:20.890754	\N	\N
70	99	3131f844-13dc-4cb1-9f31-d018fa874c7a	f	2026-08-27 14:50:04.833159	\N	2026-08-30 14:50:04.833159
23	32	95bbf9cd-2bfe-43d8-9e68-f23d29a9e57a	t	2026-07-31 10:42:54.726275	2026-07-31 10:44:20.155974	\N
24	33	0eca24ef-8851-4217-851b-33ebeb7f13f8	t	2026-07-31 10:42:56.688934	2026-07-31 10:47:28.328376	\N
71	100	2957f6e7-92c6-459f-b523-a01f3283410b	f	2026-08-27 16:53:19.253626	\N	2026-08-30 16:53:19.253626
27	36	c55fe5b0-3f8d-439d-805d-f3c9636e7724	t	2026-07-31 11:19:29.859983	2026-07-31 11:19:53.764055	\N
72	101	ca7b3ae3-87cc-4551-9b76-6771940ac8c2	f	2026-08-27 16:58:53.58095	\N	2026-08-30 16:58:53.58095
31	37	c5f51a25-7f50-43f1-b090-27e65c99119c	t	2026-07-31 15:15:34.705347	2026-07-31 15:16:27.087081	\N
35	38	900b71be-4a10-45bf-980b-d7b6af8c9a2b	t	2026-07-31 16:18:39.555019	2026-07-31 16:19:03.263276	\N
36	39	76fa6810-ed67-4faa-9cba-eef184c56abd	f	2026-08-03 14:57:55.654527	\N	\N
37	40	45ff3e5c-a14e-4b5f-844b-816644687f0f	f	2026-08-04 08:36:46.802552	\N	\N
38	41	d818753c-5ca4-4514-a00b-91c30f66ae88	t	2026-08-04 14:16:47.428114	2026-08-04 14:17:56.003242	\N
39	48	0c8ee409-7d44-45ed-a111-deb6ac424564	f	2026-08-21 09:54:54.694002	\N	\N
40	50	b3e4851b-383f-46b6-8215-500ee495890a	t	2026-08-21 10:17:00.389585	2026-08-21 10:17:50.452882	\N
73	102	40087e9b-22eb-405f-838d-f71083cc2d9c	f	2026-08-27 17:03:19.29816	\N	2026-08-30 17:03:19.29816
57	69	68987659-3693-4136-a098-8a93bd1133fe	f	2026-08-24 16:29:08.907008	\N	2026-08-27 16:29:08.907008
74	103	39096d26-0cfb-4b26-b4b3-8fb108b3b40e	f	2026-08-31 08:03:20.640929	\N	2026-09-03 08:03:20.640929
58	70	73105897-930d-431c-ad83-915b89ee435b	t	2026-08-24 16:32:04.492771	2026-08-25 08:48:24.060506	2026-08-27 16:32:04.492771
62	74	795cd5c5-f4a7-4d84-b371-b39800684e3d	t	2026-08-25 08:30:23.932035	2026-08-25 09:42:03.065471	2026-08-28 09:30:51.562954
63	75	d81882db-c831-4813-98f9-592f94df38f5	t	2026-08-25 09:59:52.801605	2026-08-25 10:01:52.909073	2026-08-28 09:59:52.801605
45	57	32fddcea-0f19-4240-b1a6-3b24f1824b56	t	2026-08-21 12:07:45.296817	2026-08-25 10:08:44.980456	\N
64	76	f258381a-a584-4361-aa26-a2df6d38528b	t	2026-08-25 10:17:07.905296	2026-08-25 10:17:53.486682	2026-08-28 10:17:07.905296
65	77	0474363b-7d54-47db-9061-f031622ff796	t	2026-08-25 10:22:44.22452	2026-08-25 10:23:30.288447	2026-08-28 10:22:44.22452
56	68	df3b79b5-b7e1-4ceb-9946-05820d7fe899	t	2026-08-24 16:00:09.511048	2026-08-25 11:09:02.755839	2026-08-27 16:00:09.511048
66	78	46e03189-1a76-4dfa-a39b-bef2af9f58c3	t	2026-08-25 11:18:01.365438	2026-08-25 11:18:47.156206	2026-08-28 11:18:01.365438
67	88	ec085287-8e60-4ea3-af14-9dddb00d2903	f	2026-08-25 16:46:55.908233	\N	2026-08-28 16:46:55.908233
68	97	a6db463e-c9fe-4dcb-a456-cbfd76e3bc2b	t	2026-08-27 11:50:11.744156	2026-08-27 11:51:24.638975	2026-08-30 11:50:11.744156
83	105	11618a88-98ae-41a0-924a-400bf02fc1ab	f	2026-08-31 08:48:14.795135	\N	2026-09-03 08:48:14.795135
84	106	55bce696-b806-45f4-b9d0-b780bbbc8327	f	2026-08-31 09:11:46.320914	\N	2026-09-03 09:11:46.320914
87	119	024c72cc-2477-4c84-b2a9-265751ff65dc	t	2026-09-01 12:34:49.3002	2026-09-01 12:36:57.097253	2026-09-04 12:34:49.3002
88	123	cb0e4b76-c719-4bce-8c95-f41eec3be0dc	f	2026-09-02 10:34:38.813399	\N	2026-09-05 10:34:38.813399
89	134	37da3177-3ed0-4c82-bbdc-c13976b63a04	f	2026-09-03 08:24:27.187569	\N	2026-09-06 08:24:27.187569
\.


--
-- Data for Name: jwt_revocado; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.jwt_revocado (jti, fecha_expiracion_token, fecha_revocacion, usuario) FROM stdin;
ef124fdd-d588-4695-ade3-212ee9729448	2026-09-02 18:27:01	2026-09-02 10:27:03.043119	sec011_tc
0b8523cb-55d4-4a23-8032-c8e09bcf4321	2026-09-02 18:35:06	2026-09-02 10:35:06.723412	sec011_au
\.


--
-- Data for Name: marca; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.marca (id_marca, nombre, id_tipo) FROM stdin;
\.


--
-- Data for Name: rol; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.rol (id_rol, nombre) FROM stdin;
1	ADMINISTRADOR
2	TECNICO
3	AUDITOR
\.


--
-- Data for Name: tipo; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tipo (id_tipo, nombre) FROM stdin;
\.


--
-- Data for Name: usuario; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuario (id_usuario, cedula, nombres, apellidos, nombre_usuario, correo, password_hash, cargo, empresa, lugar_trabajo, bloqueado, id_rol) FROM stdin;
29	111	Auditor	Test	auditor	auditor@test.com	$2a$10$rfouPRGnw87suXaXDQ81AOO6vCR4ptTBD/udIfP5WKhmOfiUa7u.6	Auditor	Coltefinanciera	Torre Coltefinanciera	f	3
30	9001	Auditor	Evidencia	auditorem	auditor.ev@coltefinanciera.com	$2a$10$D/.j2JO2/5gDIOkBrHUrhO4jlEO04hblhb694N2t2YYq1gvmqbYSK	Auditor Interno	Coltefinanciera	Principal	f	3
3	1111111	testttttt	test	test	test@test.com	$2a$10$9zfNETmM3zS0S2z6rhMcN.X6GkXu43F/ufx//TD/YgI7Ubkwy4lvm	test	test	test	f	2
31	9002	Tecnico	Evidencia	tecnicoev	tecnico.ev@coltefinanciera.com	$2a$10$YMehcdNOUp8ihruAUhVEe.Sk5V3xb2vRXzziGZsrdjy.pbGs30CGm	Tecnico	Coltefinanciera	Principal	f	2
32	9003	Admin	Evidencia	adminev	admin.ev@coltefinanciera.com	$2a$10$o5zQ4dedjjDhrwJKlohAqesC3BWjzBjb1rVlWZX7LzGE2j0r8oNXu	Admin	Coltefinanciera	Principal	f	1
2	123456789	Juan Jose	Hernandez	tecnico1	tecnico1@test.com	$2a$10$18se4uTpugz50s10yzCHGuiXWDEjrV2Z1JrAgUIgzqKUhozY103T2	Tecnico	Actas GLPI	Medellin	f	2
7	99999	Juan	Hernandez	juan	tecnicoprueba@test.com	$2a$10$qUavAg8Ik8LiupdYaISjfufzx0Mh2k9gs5EhNNd8CBv5LxuQru7iG	Tecnico	Prueba	test	f	2
20	999999999	OTP Test	Verificacion	otptest	otp.test.verify@example.com	$2a$10$o6b5868dBzOcv2mPGFApv.8CdXPMqtlXrFxLczsXU9TJSHj7pgJB6	QA	Coltefinanciera	Oficina	f	1
22	8888888888	Preview	Test	previewtest	previewtest@tests.local	$2a$10$9KK4ciQ2kmBRVne/ex3jxOlUW13mrVgo6QWPSFC.9P3B0aNe/MCde	\N	\N	\N	f	2
8	9999999	Juan	Hernandez	juantec	test1@test.com	$2a$10$VhNyrAwlTYzzRv/bUHd/AOTk5cnp3HOV/H4WmkOlrB6wC4PxfKaZ2	Tecnico	test	test	f	2
1	12345678	Juan	Hernandez	admin	admin@test.com	$2a$10$FWwzCIjhHoIY2uwp55YBzuGDV13La3wDLPJc/2FrCOAP/evsQx3oq	Administrador	Prueba	Medellin	f	1
38	88888888	Aud	Prueba	audprueba	aud@prueba.com	$2a$10$RgEutZjiyC/zx0gFYX7xhu6x57AQL3ofd58U2nGNyE06W0vHpLNSq	Auditor	CFC	Bogota	f	3
39	99999999	Tec	Prueba	tecprueba	tec@prueba.com	$2a$10$6h4vK5eUCVmGU65x/Ybs/OK.QTnAgraCXOG62P2OcMH9Xgtccut3S	Tecnico	CFC	Bogota	f	2
40	999	Audit	Sec006	audit_seco06_zz	audit_seco06_zz@nonexistent.invalid	$2a$10$kUoCpS8we7Rh9Tg59JGgMub4jdSY2GcaqMx1.lo8Sy8n/iIt.ZvUa	x	x	x	f	2
41	48321037	Prueba	Seguridad	sec011_tc	sec011_tc@seg.test	$2a$10$7KbeZcWiR6CKAD91jCcvaOrT2b4CWvnP..7UOouOmQIVXKvnG7FjW	Test	Coltefinanciera	Oficina	f	2
42	43697651	Prueba	Seguridad	sec011_ad	sec011_ad@seg.test	$2a$10$.0ADb.FlpTlTA5lTPo./seFM5p9WccfEYyP2gBf98wEPJ8Jh4AuEG	Test	Coltefinanciera	Oficina	f	1
43	60788458	Prueba	Seguridad	sec011_au	sec011_au@seg.test	$2a$10$ot1juav5XvnW.HVNAirMiONHPhEKw98BunHHH6uJNKvndeUD4qC7C	Test	Coltefinanciera	Oficina	f	3
45	1234567890	Perf Test Tecnico	Instrumentacion	perfmeas09	perfmeas09@example.co	$2a$10$4ng6VWoXQH.9d5NcTVJSz.wFxeDm4Ey6F5xEE8ZazlxIO1VtxTFHu	Tecnico	Coltefinanciera	Bogota	f	2
47	48386353294	Fase Uno	Prueba Async	fase1t_86353294	fase1t_86353294@test.local	$2a$10$ouvknhPYQAXZnaT378Bmt.UqtecqfFW3R0CQu0z4WIyHwsgh6laY6	Tecnico	Coltefinanciera	Oficina	f	2
48	48486487802	Devol Fase	Dos	devf_86487802	devf_86487802@test.local	$2a$10$atIO1UUglLyzSQpFnAvFou1Ovvjk3v2grKPhJiK.PlzEce98HjT/6	Analista	Coltefinanciera	Oficina	f	2
49	741701453	Form Hadler	Id Test	form_41701453	form_41701453@test.local	$2a$10$gvnKRUS82AHkpMZyFbF9VulqCrptkLQMWpQ4vbnqN/Y/3tONkZkvS	Tecnico	Coltefinanciera	Oficina	f	2
\.


--
-- Data for Name: usuario_firma; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuario_firma (id_firma, fecha_actualizacion, fecha_creacion, ruta_firma, usuario_id) FROM stdin;
8	2026-08-25 15:36:37.452072	2026-08-25 15:36:37.452072	uploads/firmas_tecnico/firma_tecnico_22.png	22
5	2026-08-25 15:38:30.007995	2026-08-25 15:20:44.142427	uploads/firmas_tecnico/firma_tecnico_1.png	1
9	2026-08-26 08:28:20.314109	2026-08-26 08:28:20.314109	uploads/firmas_tecnico/firma_tecnico_8.png	8
\.


--
-- Name: acta_historial_id_historial_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.acta_historial_id_historial_seq', 446, true);


--
-- Name: acta_id_acta_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.acta_id_acta_seq', 135, true);


--
-- Name: asignacion_id_asignacion_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.asignacion_id_asignacion_seq', 1, false);


--
-- Name: auditoria_sistema_id_auditoria_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.auditoria_sistema_id_auditoria_seq', 788, true);


--
-- Name: dispositivo_id_dispositivo_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.dispositivo_id_dispositivo_seq', 1, false);


--
-- Name: evidencia_id_evidencia_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.evidencia_id_evidencia_seq', 134, true);


--
-- Name: firma_otp_id_otp_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.firma_otp_id_otp_seq', 52, true);


--
-- Name: firma_token_id_token_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.firma_token_id_token_seq', 89, true);


--
-- Name: marca_id_marca_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.marca_id_marca_seq', 1, false);


--
-- Name: rol_id_rol_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.rol_id_rol_seq', 3, true);


--
-- Name: tipo_id_tipo_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tipo_id_tipo_seq', 1, false);


--
-- Name: usuario_firma_id_firma_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuario_firma_id_firma_seq', 9, true);


--
-- Name: usuario_id_usuario_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuario_id_usuario_seq', 49, true);


--
-- Name: acta_historial acta_historial_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acta_historial
    ADD CONSTRAINT acta_historial_pkey PRIMARY KEY (id_historial);


--
-- Name: acta acta_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acta
    ADD CONSTRAINT acta_pkey PRIMARY KEY (id_acta);


--
-- Name: asignacion asignacion_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asignacion
    ADD CONSTRAINT asignacion_pkey PRIMARY KEY (id_asignacion);


--
-- Name: auditoria_sistema auditoria_sistema_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auditoria_sistema
    ADD CONSTRAINT auditoria_sistema_pkey PRIMARY KEY (id_auditoria);


--
-- Name: dispositivo dispositivo_numero_placa_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dispositivo
    ADD CONSTRAINT dispositivo_numero_placa_key UNIQUE (numero_placa);


--
-- Name: dispositivo dispositivo_numero_serie_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dispositivo
    ADD CONSTRAINT dispositivo_numero_serie_key UNIQUE (numero_serie);


--
-- Name: dispositivo dispositivo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dispositivo
    ADD CONSTRAINT dispositivo_pkey PRIMARY KEY (id_dispositivo);


--
-- Name: evidencia evidencia_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.evidencia
    ADD CONSTRAINT evidencia_pkey PRIMARY KEY (id_evidencia);


--
-- Name: firma_otp firma_otp_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.firma_otp
    ADD CONSTRAINT firma_otp_pkey PRIMARY KEY (id_otp);


--
-- Name: firma_token firma_token_id_acta_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.firma_token
    ADD CONSTRAINT firma_token_id_acta_key UNIQUE (id_acta);


--
-- Name: firma_token firma_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.firma_token
    ADD CONSTRAINT firma_token_pkey PRIMARY KEY (id_token);


--
-- Name: firma_token firma_token_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.firma_token
    ADD CONSTRAINT firma_token_token_key UNIQUE (token);


--
-- Name: jwt_revocado jwt_revocado_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.jwt_revocado
    ADD CONSTRAINT jwt_revocado_pkey PRIMARY KEY (jti);


--
-- Name: marca marca_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.marca
    ADD CONSTRAINT marca_pkey PRIMARY KEY (id_marca);


--
-- Name: rol rol_nombre_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rol
    ADD CONSTRAINT rol_nombre_key UNIQUE (nombre);


--
-- Name: rol rol_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rol
    ADD CONSTRAINT rol_pkey PRIMARY KEY (id_rol);


--
-- Name: tipo tipo_nombre_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tipo
    ADD CONSTRAINT tipo_nombre_key UNIQUE (nombre);


--
-- Name: tipo tipo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tipo
    ADD CONSTRAINT tipo_pkey PRIMARY KEY (id_tipo);


--
-- Name: usuario_firma ukc5sqrpyaagkp3741jhtuulibi; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario_firma
    ADD CONSTRAINT ukc5sqrpyaagkp3741jhtuulibi UNIQUE (usuario_id);


--
-- Name: firma_otp ukewi7emeer4le2qor3ils7gxjx; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.firma_otp
    ADD CONSTRAINT ukewi7emeer4le2qor3ils7gxjx UNIQUE (sesion);


--
-- Name: usuario usuario_cedula_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_cedula_key UNIQUE (cedula);


--
-- Name: usuario usuario_correo_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_correo_key UNIQUE (correo);


--
-- Name: usuario_firma usuario_firma_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario_firma
    ADD CONSTRAINT usuario_firma_pkey PRIMARY KEY (id_firma);


--
-- Name: usuario usuario_nombre_usuario_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_nombre_usuario_key UNIQUE (nombre_usuario);


--
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id_usuario);


--
-- Name: idx_firma_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_firma_token ON public.firma_token USING btree (token);


--
-- Name: asignacion fk_asignacion_dispositivo; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asignacion
    ADD CONSTRAINT fk_asignacion_dispositivo FOREIGN KEY (id_dispositivo) REFERENCES public.dispositivo(id_dispositivo);


--
-- Name: dispositivo fk_dispositivo_marca; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.dispositivo
    ADD CONSTRAINT fk_dispositivo_marca FOREIGN KEY (id_marca) REFERENCES public.marca(id_marca);


--
-- Name: marca fk_marca_tipo; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.marca
    ADD CONSTRAINT fk_marca_tipo FOREIGN KEY (id_tipo) REFERENCES public.tipo(id_tipo);


--
-- Name: usuario fk_usuario_rol; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES public.rol(id_rol);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--

\unrestrict RM5JaGLcmRFgcVCgX4gtoAEr7ON3E1E9QeBaDz0QIqTTUBtX0EgMm5LlQPZ6VnE

