# Visión General del Proyecto — SAUCO

**Sistema de Gestión de Actas de Entrega y Devolución de Activos Tecnológicos**

> **Audiencia:** transferencia de conocimiento, equipo de desarrollo, infraestructura, soporte, auditores, liderazgo técnico y nuevos integrantes del proyecto.
> **Última actualización:** 2026-09-04.

| Campo | Valor |
|---|---|
| Nombre comercial | **SAUCO** |
| Propósito | Gestión digital de actas de entrega / devolución de activos tecnológicos |
| Backend | Java 21 · Spring Boot 3.4.1 · Maven |
| Frontend | HTML / CSS / JavaScript (Tailwind CSS 4 + FlyonUI, flatpickr) |
| Base de datos | PostgreSQL (`SaucoDB`) |
| Autenticación | JWT stateless · BCrypt · OTP para firma |
| Documentos | Apache POI 5.2.5 (DOCX) · LibreOffice / OpenPDF (PDF) |
| Integraciones | GLPI (inventario y usuarios) · SMTP (correo) |

---

## 1. Resumen Ejecutivo

SAUCO es una plataforma corporativa para la **gestión digital completa del ciclo de vida de las actas de entrega y devolución de activos tecnológicos**. El sistema reemplaza el proceso manual basado en formatos físicos y documentos diligenciados a mano por un flujo digital que genera los documentos (Word/PDF), gestiona su estado, integra el inventario desde GLPI, envía el acta al firmante mediante un enlace público con validación por OTP, y conserva trazabilidad y auditoría de cada operación.

La plataforma cubre tres tipos de acta —**entrega, devolución y formateo seguro**— y expone un ciclo de estado controlado (`GENERADA → ENVIADA → FIRMADA → APROBADA` o `RECHAZADA`) con registro de eventos por acta y de auditoría a nivel de seguridad. Está construida sobre Java 21 / Spring Boot 3, persiste en PostgreSQL y se sirve a través de un frontend estático de una sola página por módulo.

## 2. Propósito del Proyecto

Digitalizar el proceso de soporte que valida la entrega y devolución de equipos tecnológicos, sustituyendo la dependencia de **formatos físicos y del diligenciamiento manual** por un circuito digital auditable:

- Generación automática del documento formal (acta y lista de chequeo) a partir de datos capturados de forma estructurada.
- Consulta del equipo contra el inventario de GLPI por serial, evitando datos digitados a mano e inconsistencias.
- Firma del destinatario a través de un portal público, con **token de un solo uso y validación OTP**.
- Almacenamiento permanente de evidencias (firma, foto, PDF final) y trazabilidad de auditoría por estado.

## 3. Problema de Negocio que Resuelve

1. **Formatos físicos y manuales:** registros en papel, susceptibles a pérdida, deterioro, errores de transcripción y falta de trazabilidad.
2. **Inventario no confiable:** datos de equipos digitados manualmente sin verificación contra la fuente de inventario.
3. **Firma presencial:** depende de la presencia física del responsable, retrasando la formalización del proceso.
4. **Falta de auditoría:** sin registro inmutable de quién generó, envió, firmó y aprobó cada acta ni de cuándo ocurrió cada transición.
5. **Validación débil de identidad:** sin un control adicional que vincule la firma al destinatario (hoy resuelto con OTP).

## 4. Objetivos del Sistema

- Automatizar la **generación de actas** de entrega, devolución y formateo seguro a partir de formularios estructurados.
- Integrar **GLPI como fuente de verdad del inventario** (serial → marca, tipo, modelo, procesador).
- Estandarizar un **flujo controlado de firma** con enlace temporal, token de un solo uso y validación OTP.
- Generar y conservar la **documentación formal** (DOCX + PDF de acta y lista de chequeo).
- Mantener **trazabilidad completa** de estados y de **auditoría de eventos de seguridad y operación**.
- Controlar el **acceso por roles** con privilegios mínimos para administradores, técnicos y auditores.

## 5. Alcance del Proyecto

**Dentro del alcance**

- CRUD y ciclo de vida de actas con historial de eventos por acta.
- Generación de documentos Word (plantillas con placeholders) y conversión a PDF.
- Portal público de firma con token de un solo uso y OTP.
- Autenticación JWT, registro y gestión de usuarios, roles y firma permanente del técnico.
- Integración de inventario y usuarios con GLPI.
- Envío de correos con enlace de firma (SMTP configurable).
- Almacenamiento persistente de archivos con rutas virtuales.
- Auditoría de operación (`acta_historial`) y de seguridad/eventos (`auditoria_sistema`).
- API documentada (OpenAPI/Swagger) y pruebas automatizadas de los componentes críticos (seguridad, autenticación, firma y consulta de equipos).

**Fuera del alcance / por validar**

- **Entorno de producción:** URLs públicas del portal de firma, `firma-url-base` y parámetros de despliegue dependen de variables de entorno. *Pendiente de validación del entorno productivo.*

## 6. Beneficios Esperados

| Beneficio | Impacto |
|---|---|
| Eliminación del papel | Reducción de formatos físicos y del tiempo de diligenciamiento manual |
| Trazabilidad total | Cada transición de estado queda registrada con actor y fecha |
| Inventario confiable | Datos de equipos validados contra GLPI por serial |
| Firma en línea | El destinatario firma desde cualquier lugar, con validación OTP |
| Evidencias centralizadas | Firma, foto y PDF final almacenados de forma persistente |
| Acceso controlado | Roles con privilegios mínimos y secretos fuera del repositorio |
| Auditoría lista | Eventos de seguridad y operación consultables |

## 7. Actores del Sistema

| Actor | Rol en el sistema | Privilegios |
|---|---|---|
| **ADMINISTRADOR** | Gestiona usuarios y actas | Ve todas las actas; crea usuarios con rol elegido; aprueba/rechaza; operación completa |
| **TÉCNICO** | Operación del día a día | Crea y gestiona **solo las actas que él generó** (`idTecnico` = propio); firma y aprueba |
| **AUDITOR** | Control y verificación | Consulta y evidencia (solo lectura); el backend bloquea las acciones operativas (403) |
| **Destinatario / Firmante** | Usuario externo sin cuenta | Accede al portal público con el token del correo; valida por OTP; firma y registra evidencia |

> Nota: el registro público de cuentas asigna siempre rol **TÉCNICO**; la creación con rol elegido es exclusiva del administrador. Se protege una cuenta administrativa principal que no puede ser bloqueada ni cambiada de rol.

## 8. Funcionalidades Principales

**Gestión de actas**

- Creación de actas de **entrega, devolución y formateo seguro**.
- Límites de plantilla por documento (hasta 10 equipos, 11 hardware, 10 otros, 36 checkboxes).
- Regeneración de documentos ante fallos (`GENERANDO_DOCUMENTOS` / `GENERACION_FALLIDA`).
- Vista previa, detalle, historial y evidencias por acta.

**Ciclo de vida y firma**

- Envío del acta por correo con enlace temporal (`firma.html?token=…`).
- Portal público con **token de un solo uso** y **verificación OTP** (vigencia 10 min por intento, máx. 5 intentos, reenvíos limitados).
- Firma manuscrita (PNG) + fotografía del responsable (JPG).
- Regeneración del documento firmado con la firma embebida y reconversión a PDF.
- Aprobación o rechazo con motivo por parte de administradores/técnicos.

**Usuarios y seguridad**

- Login/registro JWT, cierre de sesión con revocación de token, cambio de credenciales.
- Firma permanente del técnico (`usuario_firma`).
- Auditoría de eventos de seguridad (`auditoria_sistema`): logins, logouts, intentos fallidos.

**Documentación y almacenamiento**

- Conversión DOCX → PDF (LibreOffice portable; OpenPDF como respaldo).
- Almacenamiento persistente bajo `storage.root` con rutas virtuales (`uploads/…`).

**API**

- API REST documentada (OpenAPI/Swagger, `/swagger-ui`), con validación de parámetros y respuestas tipadas.

## 9. Integraciones Principales

| Integración | Uso | Detalle técnico |
|---|---|---|
| **GLPI** | Inventario y usuarios | API REST (`initSession` con App-Token + User-Token). Consulta el equipo por serial y extrae marca, tipo, modelo y procesador. Autocompletado de usuarios del área |
| **SMTP (correo)** | Envío del enlace de firma | Spring Mail; compatible con Microsoft 365 / Exchange / SMTP estándar. Configurable y opcional (sin `MAIL_*` la app arranca e ignora el envío) |
| **LibreOffice / OpenPDF** | Generación de PDF | Conversión headless DOCX→PDF; OpenPDF como fallback propio |

## 10. Arquitectura de Alto Nivel

```
                      +--------------------------------------------------+
      Navegador       |                 Frontend estático                 |
   (login, actas,     |   HTML/CSS/JS · Tailwind 4 · FlyonUI · flatpickr  |
    firma pública)    +--------------------------------------------------+
                                 |   HTTP/JSON (JWT Bearer)
                                 v
                      +--------------------------------------------------+
                      |               Backend Spring Boot 3               |
                      |   Controller → Service → Repository (JPA)         |
                      |   · acta / firma / usuario / rol / auth / mail    |
                      |   · auditoría  · motor DOCX + PDF  · seguridad    |
                      +--------------------------------------------------+
                          |            |               |              |
                    +-----------+ +---------+     +---------+   +-------------+
                    | PostgreSQL| |   GLPI  |     |  SMTP   |   | Storage     |
                    |  SaucoDB  | |  REST   |     | correo  |   | /uploads/** |
                    +-----------+ +---------+     +---------+   +-------------+
```

**Componentes backend** (paquete `com.empresa.actas`)

- **Módulos gestionados:** `acta` (CRUD, ciclo de vida, historial), `firma` (portal público, token, OTP, evidencias), `usuario`/`rol`/`auth` (identidad y JWT), `mail`, `auditoria`.
- **Servicios de generación documental:** motor de plantillas DOCX (`DocxTemplateEngine`), preparación de datos (`DocumentoWordService`), orquestación (`DocxActaService`), conversión a PDF (`LibreOfficePdfService` / `DocxToPdfService`) y embebido de firma/foto (`DocxImageReplacer`).
- **Seguridad:** JWT stateless, BCrypt, filtro de rate limiting, revocación de tokens, rutas públicas controladas y sanitización de HTML (OWASP).

**Frontend** — páginas estáticas por módulo: login, home, gestión de actas, actas de entrega/devolución/formateo, portal y listado de firmas, usuarios, perfil y auditoría.

## 11. Flujo General del Negocio

1. **Generación.** Administrador o técnico crea el acta (entrega / devolución / formateo). El sistema persiste el payload original y genera los documentos (DOCX + ZIP) y su versión PDF. → `GENERADA`.
2. **Envío.** El técnico envía el acta; se genera un **token UUID de un solo uso** y se envía el correo con el enlace del portal de firma. → `ENVIADA`.
3. **Firma.** El destinatario abre el enlace (ruta pública), valida su identidad por **OTP** y registra firma + fotografía. El token queda marcado como utilizado y el documento se regenera con la firma embebida. → `FIRMADA`.
4. **Cierre.** Administrador o técnico aprueba (generando el PDF final si falta) o rechaza con motivo. → `APROBADA` / `RECHAZADA`.

Cada transición registra un evento en `acta_historial` (tipo de evento, actor y token) y los eventos de seguridad en `auditoria_sistema`.

## 12. Consideraciones de Seguridad

- **Autenticación:** JWT stateless con expiración configurable; contraseñas con **BCrypt**; cierre de sesión revoca tokens (`JwtRevocado`).
- **Portales públicos controlados:** solo las rutas necesarias (`/auth/**`, `/equipo/**`, `/firma/**`, descargas y documentación y uploads) quedan fuera de JWT.
- **Rate limiting:** límite de intentos de login por IP (20/60 s) y de registros (5/hora) para frenar fuerza bruta y spam de cuentas.
- **Validaciones de entrada:** cédula numérica, juegos de caracteres permitidos, política de contraseña (mayúscula, minúscula, dígito y especial), whitelist de campos de ordenamiento y sanitización de contenido HTML.
- **Principio de menor privilegio:** el técnico solo opera sus actas; el auditor es solo lectura; el registro público asigna TÉCNICO (nunca ADMIN), y se protege una cuenta administrativa principal.
- **Secretos fuera del repositorio (SEC-002):** contraseña de BD, secreto JWT y tokens GLPI se inyectan por variables de entorno o `backend/.env` (gitignoreado); el código no contiene secretos embebidos.
- **OTP de firma:** segunda capa de identidad sobre el token; límites de intentos, reenvíos y caducidad.

## 13. Beneficios Operativos

- **Tiempos reducidos:** generación automática de documentos y firma en línea sin gestión de papel.
- **Calidad de datos:** equipos validados contra GLPI por serial; menos campos digitados a mano.
- **Recuperación y consistencia:** sobre fallos de generación, el sistema permite **reintentar** la generación de documentos conservando el payload original.
- **Centralización:** evidencias y documentos finales en almacenamiento persistente, independiente del SO o de la raíz de despliegue.

## 14. Beneficios para Auditoría y Control

- **Trazabilidad por acta:** `acta_historial` registra cada evento del ciclo (generación, envío, apertura, firma, rechazo, aprobación, evidencias, regeneración y fallos) con actor y token.
- **Auditoría de seguridad:** `auditoria_sistema` captura eventos de autenticación e intentos fallidos para revisión.
- **Evidencias inmutables:** firma, fotografía y PDF final quedan vinculados al acta en almacenamiento persistente.
- **Modelo de datos normalizado:** el esquema pasó por un proceso de normalización con migraciones SQL versionadas y respaldo previo (ver `docs/auditoria-modelo-datos-sauco.md`).

## 15. Glosario Básico

| Término | Definición |
|---|---|
| **Acta** | Documento formal que registra la entrega, devolución o formateo de un activo tecnológico |
| **GLPI** | Sistema de gestión de inventario de activos; fuente de datos de equipos y usuarios |
| **OTP** | Código de un solo uso enviado al destinatario para validar su identidad al firmar |
| **JWT** | Token JSON de autenticación stateless usado por API y frontend |
| **DOCX / PDF** | Formatos del documento generado: plantilla editable (Word) y versión final (PDF) |
| **Token de firma** | UUID de un solo uso que habilita el portal público de firma |
| **Stock de inventario** | Registro de activos (marca, tipo, modelo, procesador) proveniente de GLPI |
| **`datosOriginales`** | JSON del request original conservado para regenerar el documento firmado |

## 16. Conclusión

SAUCO consolida en una sola plataforma el circuito documental de entrega y devolución de activos, con generación automatizada de documentos, firma en línea validada por OTP, integración con GLPI y auditoría completa del ciclo de vida. El diseño por roles, la separación de módulos, la trazabilidad por evento y la gestión de secretos fuera del repositorio lo hacen apto para operación corporativa y revisión por auditoría.

Para completar el panorama de despliegue, queda pendiente validar con el área responsable el **entorno productivo**: URL pública del portal de firma (`firma-url-base`) y parámetros de despliegue, hoy definidos por variables de entorno.