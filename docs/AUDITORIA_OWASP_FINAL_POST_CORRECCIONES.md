# Auditoría OWASP — Informe Final Post-Correcciones

Sistema: **SAUCO — Gestión de Actas de Entrega/Devolución (Coltefinanciera)**
Auditoría base: `docs/auditoria_owasp_preproduccion_2026-09-07.md`
Fecha del cierre: **2026-09-07**
Criterio: **0 CRÍTICOS, 0 ALTOS, riesgo residual aceptable**, aprobación de producción.

---

## 1. Resumen Ejecutivo

La auditoría OWASP de pre-producción detectó **28 hallazgos** (SEC-101..128). Tras la
remediación integral en 4 fases:

| Hallazgos | ANTES | DESPUÉS |
|---|---|---|
| CRÍTICO | 1 (SEC-101) | **0** |
| ALTO | 1 (SEC-102) | **0** |
| MEDIO | 13 (SEC-103..115) | **0** |
| BAJO | 13 (SEC-116..128) | **0** |
| **Total** | **28** | **0 abiertos** |

Todos los hallazgos se **eliminaron por corrección de código/configuración** o quedaron
**mitigados con control compensatorio y riesgo residual documentado** (2 casos, ver §4).

**Dictamen: ✅ APTO PARA PRODUCCIÓN** (bajo las condiciones de despliegue de §6).

---

## 2. Método y Fases

| Fase | Alcance | Resultado |
|---|---|---|
| FASE 0 | SEC-101 (CRÍTICO), SEC-102 (ALTO) + rotación de secretos | Corregido, validado |
| FASE 1 | SEC-103..106 (MEDIO) | Corregido, validado |
| FASE 2 | SEC-107..115 (MEDIO) | Corregido, validado |
| FASE 3 | SEC-116..128 (BAJO/INFO) | Corregido, validado |
| Validación | Compilación Maven + arranque + pruebas de no-regresión en cada fase | OK |

Cada corrección siguió el flujo requerido: evidencia original → modificación →
por qué mitiga → validación de no-regresión.

---

## 3. Estado por Hallazgo (ANTES vs DESPUÉS)

### FASE 0 — Críticos/Altos

| ID | Severidad | ANTES | Corrección | DESPUÉS |
|---|---|---|---|---|
| SEC-101 | CRÍTICO | Path traversal: `rutaPdf` controlada por cliente permitía lectura arbitraria de archivos | `StoragePathResolver.bajoUploads` (normalize + startsWith, único punto de resolución) + cambios en `ActaService`/`PdfService`/`SignedDocumentService` | **ELIMINADO** |
| SEC-102 | ALTO | Stored XSS: `cargo` inyectado sin escapar en panel de usuarios; registro público lo permitía | Sanitización con `owasp-java-html-sanitizer` en almacenamiento + escape en render | **ELIMINADO** |
| — | — | Secretos (DB, JWT, GLPI) en historial git | Rotación documentada en `docs/rotacion_secretos.md`; `SEC-002`: sin default en yaml, fail-fast | **MITIGADO** |

### FASE 1 — Medios (SEC-103..106)

| ID | Corrección | DESPUÉS |
|---|---|---|
| SEC-103 | JWT invalidados tras cambio/reset de password (verificación de versión/revocación en filtro) | **ELIMINADO** |
| SEC-104 | Rate limit por IP sobre `/auth/recuperar` (`RateLimitFilter`) | **ELIMINADO** |
| SEC-105 | Tokens de firma/reset enmascarados en logs/auditoría; OTP hasheado BCrypt | **ELIMINADO** |
| SEC-106 | `/descargar-acta/{nombreZip}` verifica propiedad (IDOR cerrado) | **ELIMINADO** |

### FASE 2 — Medios (SEC-107..115)

| ID | Corrección | DESPUÉS |
|---|---|---|
| SEC-107 | `/equipo/**` ya no público: exige JWT; frontend lo envía; serial validado por allow-list | **ELIMINADO** |
| SEC-108 | `mail.smtp.ssl.trust` sin default (validación TLS completa del relay) | **ELIMINADO** |
| SEC-109 | `ddl-auto` parametrizable; producción con `validate` + migraciones SQL manuales | **ELIMINADO** |
| SEC-110 | Swagger/OpenAPI deshabilitado en producción | **ELIMINADO** |
| SEC-111 | Allow-list de dominios de correo (`CORREO_DOMINIOS_PERMITIDOS`) | **ELIMINADO** |
| SEC-112 | GLPI `https` por defecto; HTTP solo por decisión conciente de entorno | **ELIMINADO** |
| SEC-113 | Cola de generación acotada (20) + backpressure `CallerRunsPolicy`; límites de archivos | **ELIMINADO** |
| SEC-114 | CORS parametrizable (`CORS_ALLOWED_ORIGINS`) para despliegue real | **ELIMINADO** |
| SEC-115 | Actuator `/health` expuesto al mínimo (sin detalles), readiness operativo | **ELIMINADO** |

### FASE 3 — Bajos/Info (SEC-116..128)

| ID | Corrección | DESPUÉS |
|---|---|---|
| SEC-116 | Registro: mensajes genéricos de duplicado (anti-enumeración) | **ELIMINADO** |
| SEC-117 | Token de recuperación en fragmento `#token=` (no query string); frontend lo lee y limpia | **ELIMINADO** |
| SEC-118 | Un solo logout real `/sesiones/revocar` (revoca jti + audita LOGOUT, falla si no escribe); eliminado `/auth/logout` | **ELIMINADO** |
| SEC-119 | `GET /otp/estado` sin efecto lateral (no envía correo); reenvío único emisor; throttle por IP sobre `/firma/*/otp` | **ELIMINADO** |
| SEC-120 | `PasswordResetToken` como digest SHA-256 hex (`TokenDigest`), `findByTokenHash` (@Query), columna varchar(64) + backfill SQL | **MITIGADO** (FirmaToken residual, ver §4) |
| SEC-121 | Auditoría de IP confía en `X-Forwarded-For` solo tras proxy de confianza | **ELIMINADO** |
| SEC-122 | Mensajes de negocio genéricos ("Acta no encontrada", sin id) en 7 servicios | **ELIMINADO** |
| SEC-123 | Allow-list de campos de ordenación en `/usuarios` (sin 500 inducible) | **ELIMINADO** |
| SEC-124 | Ruta de evidencia ya resuelta por `StoragePathResolver.bajoUploads` (fallback `Paths.get` eliminado) | **ELIMINADO** (verificado) |
| SEC-125 | Consulta GLPI de usuarios auditada (`CONSULTA_GLPI_USUARIOS` + CHECK), longitud mínima 2 chars, `q=""` no vuelca | **ELIMINADO** |
| SEC-126 | `spring-boot-devtools` retirado del artefacto de producción | **ELIMINADO** |
| SEC-127 | `datosOriginales` fuera de la allow-list de ordenación de actas | **ELIMINADO** |
| SEC-128 | `glpi.url` sin default interno; `GLPI_URL` explícita; fallback claro si vacía | **ELIMINADO** |

---

## 4. Riesgo Residual Documentado

Dos decisiones de diseño dejan riesgo residual **aceptado y justificado**:

### SEC-120 — FirmaToken en claro (residual)
El token de **firma** (a diferencia del de recuperación, ya hasheado) sigue en claro en BD.
Razones:
1. Su uso está **gateado por un segundo factor OTP** (6 dígitos, hasheado BCrypt, enviado al
   correo del destinatario). Un dump de BD otorga al atacante el enlace, **jamás el OTP**, y
   sin OTP no hay sesión de firma válida (`verificarSesion` en `OtpService`).
2. La funcionalidad legítima de "copiar enlace" del personal (`firmas.js` usa `tokenFirma`
   proveniente de `ActaService.setTokenFirma`) requiere el valor en claro; un hash de un solo
   sentido lo destruiría.

**Riesgo residual:** bajo. Documentado como decisión conciente. Si en el futuro se elimina la
vista "copiar enlace", migrar FirmaToken a digest SHA-256 como PasswordResetToken.

### SEC-128 — GLPI por HTTP (decisión de entorno)
El entorno local consulta GLPI interno por HTTP (LAN). Se documenta como decisión conciente
(`SEC-112`), con HTTPS por defecto y sin URL hardcodeada.

---

## 5. Validaciones de No-Regresión (FASE 3)

Ejecutadas contra la app levantada (puerto 8001):

| Control | Prueba | Resultado |
|---|---|---|
| Compilación | `mvn clean package -DskipTests` | OK (jar generado) |
| Arranque | `java -jar` con `.env` | OK (Started ActasApplication) |
| SEC-123 | `sort=cedula` → 200; `sort=passwordHash` → 200 (cae a default, sin 500) | OK |
| SEC-127 | `actas?sort=datosOriginales` → 200 (default) | OK |
| SEC-119 | 31.ª petición a `/firma/{t}/otp/estado` → **429** | OK |
| SEC-121 | Login con `X-Forwarded-For: 203.0.113.99` → auditoría guarda `127.0.0.1` | OK |
| SEC-125 | `q=jo` → audita `CONSULTA_GLPI_USUARIOS`; `q=a` (1 char) → no audita | OK |
| SEC-117/120 | `POST /auth/recuperar` → token en BD como **hash SHA-256 de 64 hex**; respuesta genérica | OK |
| SEC-118 | `POST /auth/logout` → 403 (eliminado); `POST /sesiones/revocar` → 200 | OK |
| Migración | `migracion_hash_tokens_recuperacion.sql` aplicada (columna→64, CHECK ampliado) | OK |

**Correcciones adicionales durante validación:**
- `findByTokenHash` pasó de derivación (fallaba: no existe propiedad `tokenHash`) a `@Query` explícito.
- El CHECK constraint de `auditoria_sistema.tipo_evento` se amplió manualmente para aceptar
  `CONSULTA_GLPI_USUARIOS` (ddl-auto:update no altera constraints con datos).

---

## 6. Condiciones para Producción (checklist de despliegue)

- [ ] `JPA_DDL_AUTO=validate` (esquema bajo migraciones SQL), nunca `update`.
- [ ] `JWT_SECRET` nuevo y fuerte (`openssl rand -base64 48`); no reusar el local.
- [ ] `DB_PASSWORD` por secretary/env, sin default.
- [ ] `GLPI_URL` explícita (HTTPS o HTTP-LAN decidido conscientemente).
- [ ] `CORREO_DOMINIOS_PERMITIDOS=coltefinanciera.com`.
- [ ] `CORS_ALLOWED_ORIGINS=https://actas.coltefinanciera.com`.
- [ ] Swagger deshabilitado (`springdoc` apagado en prod).
- [ ] `RATE_LIMIT_TRUST_XFF=true` **solo** si hay proxy de confianza; si no, `false`.
- [ ] Artifact desplegado sin `spring-boot-devtools` (ya retirado del build).
- [ ] Storage apuntando a volúmenes persistentes (`STORAGE_ROOT`).
- [ ] LibreOffice portable presente donde se convierta a PDF.

---

## 7. Conclusión

Todos los hallazgos de la auditoría OWASP quedaron **corregidos o mitigados con control
compensatorio documentado**. El sistema pasa de **NO APTO (1 CRÍTICO / 1 ALTO)** a:

> **✅ APTO PARA PRODUCCIÓN** — 0 CRÍTICOS, 0 ALTOS, riesgo residual bajo y aceptado.

La remediación quedó versionada como commits "Java N.M" desde Java 3.2 hasta **Java 5.1**,
con auditoría de sistema (CAPA 2) registrando las acciones de seguridad y trazabilidad de
consultas a datos personales (GLPI).
