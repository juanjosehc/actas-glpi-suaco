# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Sistema de gestión de actas de entrega/devolución de activos tecnológicos (Coltefinanciera). Genera documentos Word (DOCX) desde templates con placeholders `{{ var }}`, los convierte a PDF, permite firma digital pública por token y mantiene auditoría por estado. Consulta equipos contra una instancia GLPI por serial.

La documentación en `README.md`, `ARQUITECTURA.md` y `FLUJO_FUNCIONAL.md` describe la arquitectura original (solo generación de documentos). El repo ha evolucionado mucho más allá: ahora incluye autenticación JWT, roles, CRUD de actas con historial, portal público de firma y persistencia en PostgreSQL. Trátalas como referencia histórica, no como el estado actual.

## Stack

- **Backend**: Java 21, Spring Boot 3.4.1, Maven (compila a Java 21). Lombok + MapStruct (mappers generados por anotación).
- **Base de datos**: PostgreSQL `SaucoDB` (localhost:5432, usuario `postgres`). JPA con `ddl-auto: update`.
- **Seguridad**: JWT (jjwt 0.12.6) + Spring Security, stateless. Passwords con BCrypt.
- **Documentos**: Apache POI 5.2.5. Conversión a PDF vía LibreOffice portable externo (`libreoffice.path`, un `soffice.exe`) o OpenPDF (fallback propio, menos fiel).
- **Frontend**: HTML/CSS/JS vanilla, Tailwind CSS 4 (CLI) + FlyonUI, Flatpickr. Sin build de bundle; se sirve estático.
- **OpenAPI/Swagger** en `/swagger-ui` (springdoc).

## Comandos comunes

```bash
# Backend — compilar
cd backend && mvn clean package -DskipTests

# Backend — ejecutar (puerto 8001)
cd backend && mvn spring-boot:run

# Backend — compilar con tests (no hay tests todavía en src/test)
cd backend && mvn test

# Frontend — dependencias y regenerar CSS (Tailwind 4 + FlyonUI se compilan desde app.css)
cd frontend && npm install
cd frontend && npx @tailwindcss/cli -i ./css/app.css -o ./css/output.css

# Frontend — servir en http://127.0.0.1:5500 (o abrir frontend/pages/*.html directo)
# (VS Code Live Server u otro estático; el backend CORS acepta localhost:5500)

# Migración de almacenamiento antiguo → storage persistente
python tools/migrate_storage.py --dry-run   # ver qué se movería
```

El backend **debe** correr en el puerto 8001: el frontend lo tiene hardcodeado (`http://127.0.0.1:8001`).

## Arquitectura backend (`com.empresa.actas`)

Conviven dos generaciones de código bajo el mismo paquete base:

### 1. Módulo gestionado (nuevo, preferido para trabajo nuevo)

Paquetes autocontenidos siguiendo patrón controller/dto/entity/repository/service/mapper:

- `acta/` — CRUD de actas, ciclo de vida y auditoría. Entidades `Acta`, `ActaHistorial`; estados `GENERADA → ENVIADA → FIRMADA → APROBADA` (o `RECHAZADA`). `Acta` guarda `datosOriginales` (JSON del request original, reutilizado para regenerar el documento firmado) y `contenidoHtml`.
- `firma/` — Portal público de firma por token (`/firma/{token}`), evidencia (firma PNG, foto JPG, PDF final). `FirmaToken` es un UUID de un solo uso con `utilizado` + `fechaUtilizacion`. Las rutas públicas no requieren JWT.
- `usuario/`, `rol/`, `auth/` — Usuarios, roles (`ADMINISTRADOR`, `TECNICO`, `AUDITOR` — sembrados por `config/DataInitializer`), login/registro JWT.
- `mail/` — Envío de correos SMTP (Spring Mail). Opcional: si `MAIL_*` no está configurado arranca igual y omite con un log.

Las rutas protegidas exigen JWT salvo las listas en `SecurityConfig` y `JwtAuthenticationFilter` (`PUBLIC_PATHS`): `/auth/**`, `/equipo/**`, `/usuario`, `/generar-acta`, `/generar-devolucion`, `/descargar-acta/**`, `/firma/**`, `/uploads/**`, swagger. Nota: el filtro JWT solo valida si llega header `Authorization: Bearer`, así que los endpoints `permitAll` realmente aceptan ambos.

### 2. Servicios de generación (original, en `service/`)

- `DocxTemplateEngine` — Motor de templates DOCX a nivel de "run" (una `{{ var }}` puede estar partida en varios runs de Word por cambios de formato; reconstruye el texto del párrafo, localiza el placeholder y escribe el valor en el run preservando formato). Es `static`, sin estado.
- `DocumentoWordService` — Prepara datos para templates: descompone fecha en `dia/mes/anio`, indexa listas (`eq_N_*`, `hw_N_*`, `ot_N_*`, `chk_N_si/no`, checkboxes `■`/`□`, `win10/win11/macos`). Límites por template: 10 equipos, 11 hardware, 10 otros, 36 checkboxes; items extra se ignoran silenciosamente.
- `DocxActaService` — Orquesta: genera acta + checklist (entrega) o devolución, empaqueta ZIP, y convierte el DOCX a PDF vía `LibreOfficePdfService`.
- `LibreOfficePdfService` — Convierte DOCX→PDF ejecutando soffice headless con perfil aislado (semáforo global `Semaphore(1)` — nunca dos conversiones simultáneas), 2 intentos, timeout de 120s.
- `DocxToPdfService` — Conversión alternativa con OpenPDF (fuente Java pura, menos fiel al layout; no la usan los flujos principales hoy).
- `DocxImageReplacer` (static) — Inserta firma/foto embebidas en el DOCX reemplazando `{{firma_usuario}}` / `{{foto_usuario}}`, con cálculo de dimensiones para que quepan en sus celdas.
- `SignedDocumentService` — Regenera el documento firmado tras la firma: re-serializa `datosOriginales`, agrega `fecha_firma`, reinserta firma+foto e invierte a PDF, registrando evidencias y eventos de auditoría.
- `EquipoService` — Integración GLPI REST: `POST /initSession` con App-Token + User-Token, `GET /search/Computer` filtrando por campo 5 (serial), extrae campos 23 (marca), 4 (tipo), 40 (modelo), 17 (CPU) y abrevia el CPU (ej. "Core i5").
- `ZipService` — Empaqueta DOCX en ZIP.

### Flujo típico de un acta

1. `POST /actas` (JWT) crea la acta con `estado=GENERADA`, guarda `datosOriginales` (payload completo) y registra evento `ACTA_GENERADA`.
2. `POST /actas/{id}/enviar` → `FirmaService.enviarActa`: crea `FirmaToken` (UUID), estado `ENVIADA`, correo con URL `{firma-url-base}/firma.html?token=...`, evento `ACTA_ENVIADA`.
3. El firmante abre `/firma.html?token=...` (público): `GET /firma/{token}` devuelve los datos del acta.
4. `POST /firma/{token}` guarda firma PNG + foto JPG en storage, marca token como usado, estado `FIRMADA`, luego `SignedDocumentService` regenera el documento con firma embebida y lo pasa a PDF.
5. Técnico/Admin aprueba o rechaza: `POST /actas/{id}/aprobar` (genera PDF final si falta, guarda evidencia `PDF_FINAL`) o `/rechazar`.
   Cada transición registra un `ActaHistorial` con `tipo_evento`, actor y token de firma.

## Almacenamiento de archivos

Todo se escribe bajo `storage.root` (env `STORAGE_ROOT`, default `${user.dir}/storage`):

```
storage.root/
├── generated/        # DOCX y ZIP
└── uploads/
    ├── pdf/          # PDF finales
    ├── firmas/       # firma_<id>.png
    └── fotos/        # foto_<id>.jpg
```

Las rutas guardadas en `acta.ruta_pdf` / `evidencia.ruta_archivo` son **virtuales** (`uploads/...`, apuntando al storage root configurado), servidas por el backend bajo `/uploads/**`. No dependen del SO ni de la raíz de despliegue.

`storage/` y `backend/storage/` están en `.gitignore`. Si se mueve la raíz de storage en un entorno ya usado, correr `python tools/migrate_storage.py` (idempotente, no sobrescribe, conserva el esquema virtual).

## Base de datos

- JPA `ddl-auto: update` crea/actualiza tablas. Las migraciones manuales van en `backend/src/main/resources/sql/` y se ejecutan a mano (ej. `migracion_auditoria_acta_historial.sql`: agrega `tipo_evento` NOT NULL con backfill, lo que `ddl-auto` no puede hacer sobre tabla con datos).
- Sin tests en el repo (`src/test` vacío); solo starter-test declarado.
- Config de conexión y credenciales en `backend/src/main/resources/application.yml`. Tras **SEC-002 ya no hay secretos commiteados**: password DB, JWT secret y tokens GLPI van por variables de entorno o por `backend/.env` (gitignoreado). Para desarrollo local: `copy backend\.env.example backend\.env` y completar `DB_PASSWORD`, `JWT_SECRET`, `GLPI_APP_TOKEN`, `GLPI_USER_TOKEN`. `DB_PASSWORD` y `JWT_SECRET` no tienen default en el yaml: si faltan la app no arranca (fail-fast, no hay secret de respaldo). Los valores viejos que quedaron en el historial de git deben rotarse.

## Frontend

- Páginas estáticas en `frontend/pages/`: `login.html` (auth), `home.html` (menú), `actas.html` + `acta-view.html` (paneles de gestión), `acta-entrega.html` / `acta-devolucion.html` / `acta-formateo.html` (formularios V1 vigentes) y `firma.html` / `firmas.html` (portal público y listado), `usuarios.html`, `perfil.html`, `auditoria.html`. Sprint 4 eliminó `generar-acta.html` (V2, huérfana y rota: dependía de `frontend/templates/` que no existe) y `checklist-entrega.html` (sin JS). La migración V1→V2 quedó descartada: los formularios V1 son la navegación canónica.
- Los botones "Vista Previa" de Entrega/Devolución dependen de `frontend/templates/*.html` (NO existen) y de `acta-view.html?preview=true`; están rotos (QA-42). Formateo no tiene Vista Previa por consistencia. Arreglar preview = crear el dir de templates, fuera de Sprint 4.
- Cada página tiene su JS homónimo en `frontend/js/`; `ui.js` trae utilidades compartidas. Los fetch al backend usan `http://127.0.0.1:8001` y envían el JWT de `login` en `Authorization: Bearer` para las rutas protegidas.
- CSS: `output.css` es el build de Tailwind 4 + FlyonUI compilado desde `css/app.css` (fuente con `@import "tailwindcss"` y `@plugin "flyonui"`). No editar `output.css` a mano; regenerarlo con el comando de arriba. El resto de `css/*.css` son estilos custom.
- CORS del backend acepta `localhost`/`127.0.0.1` en puertos 80, 5500, 8080 y 8001.

## Notas de entorno y cosas no obvias

- `word/` está vacío y `graphify-out/` es caché de una herramienta de análisis (no tocar). `opencode.json` en la raíz solo configura un MCP de PostgreSQL para otra herramienta; el password del MCP va por variable de entorno `PG_PASSWORD` (el archivo NO contiene secretos, ver SEC-002).
- `node_modules` y `package-lock.json` en la raíz son del MCP de opencode, no del frontend (el frontend tiene el suyo en `frontend/`).
- El proyecto se versiona con commits "Java N.M" (Java 1.0 → 3.2), referenciando etapas de evolución.
- LibreOffice portable se espera en `%USERPROFILE%\LibreOfficePortable\...\soffice.exe` (config `libreoffice.path`); si no existe, la conversión a PDF falla en runtime.