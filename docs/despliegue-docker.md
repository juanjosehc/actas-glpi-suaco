# PREPARACIÓN DE SAUCO PARA DESPLIEGUE EN DOCKER Y PRODUCCIÓN

> **Tipo:** análisis de arquitectura real (basado en el código del repo al commit `5632839`).
> **Alcance:** qué se necesita exactamente para desplegar SAUCO desde cero en un servidor limpio.
> **Fecha:** 2026-09-04.
> **Veredicto:** ❌ **REQUIERE AJUSTES PREVIOS** — ver sección 13.

---

## 1. Estado real de la infraestructura

| Componente | Estado hoy | Evidencia |
|---|---|---|
| `Dockerfile` / `docker-compose.yml` / `.dockerignore` | **No existen** en el repo | glob raíz: 0 resultados |
| Backend JAR | Spring Boot 3.4.1, repackage de Maven **sin nombre fijo** (`actas-glpi-<version>.jar`) | `backend/pom.xml` (artifactId `actas-glpi`) |
| Base de datos | JPA `ddl-auto: update` **crea el esquema solo** en BD vacía | `application.yml` |
| Migraciones SQL | 11 archivos, **todos legacy** (NO se corren en instalación fresca) | sección 8 |
| Seed de roles | `DataInitializer` siembra `ADMINISTRADOR`, `TECNICO`, `AUDITOR` **solo si faltan** | `config/DataInitializer.java` |
| **Seed de usuarios** | **NO existe.** Ningún `admin` inicial se crea solo | `DataInitializer` solo toca `rol` |
| Almacenamiento de archivos | `storage.root` (default `${user.dir}/storage`), dirs creados al arranque, **falla si no son escribibles** | `config/AppConfig.java` |
| Perfil de LibreOffice | Compartido bajo `storage.root/lo-profile` (persistible) | `service/LibreOfficePdfService.java:126` |
| Conversión DOCX→PDF | LibreOffice headless (`soffice`), semáforo global `Semaphore(1)`, 2 intentos, timeout 120 s | `LibreOfficePdfService` |
| Conversión fallback | OpenPDF (Java puro, menos fiel) | `service/DocxToPdfService.java` |
| Plantillas DOCX | Dentro del JAR (`classpath:plantillas`) → **sin volumen** | `application.yml` (`app.templates-dir`) |
| Frontend | Estático, **NO lo sirve el backend** (sin `static-locations`); se sirve con Live Server/nginx | `frontend/` |
| URL de API en JS | `window.SAUCO_API || "http://localhost:8001"` → la base se puede inyectar | `frontend/js/api.js:14` |
| CORS | Hardcodeado a `localhost`/`127.0.0.1` en puertos 80/5500/8080/8001 → **no apto para origin de prod** | `config/SecurityConfig.java:131-138` |
| Swagger | Público por defecto (`app.documentacion.publica:true`); en prod debe ir `false` | `SecurityConfig.java:40-67` |
| Health endpoint | **No existe controller `/health` ni Actuator** (solo está en permitAll) → da 404 | pom sin actuator; grep `health` sin controller |
| Generación asíncrona | `ExecutorService` de **un solo hilo daemon en memoria**; cola no persistente | `service/GeneracionDocumentalAsyncService.java` |
| Recuperación de actas congeladas | Al arrancar re-encola `GENERANDO_DOCUMENTOS` desde `datosOriginales` | `service/ReintentoGeneracionService.java` |
| Reintento manual | `POST /actas/{id}/reintentar-generacion` tras `GENERACION_FALLIDA` | `ActaController` |
| Rate limiting | En memoria del proceso (por IP): login 20/60 s, registro 5/h | `application.yml` + `RateLimitFilter` |
| Correo | Opcional (`MAIL_*` vacío → arranca igual y omite envío); `ssl.trust` default Brevo | `application.yml` |
| Tests | 6 clases en `backend/src/test` (seguridad, JWT, firma, GLPI, controller) | `src/test/java/...` |

**Conclusión parcial:** el backend es **dockerizable en caliente** (patrón clásico jar + volumen de storage + PostgreSQL externo), pero **no hay 1 línea de infraestructura Docker escrita** y existen **6 brechas concretas** (sección 12) que cerrar antes de producción.

---

## 2. Arquitectura de despliegue recomendada

Tres contenedores en una sola red Docker + volúmenes persistentes:

```
                        Internet (HTTPS, adelante: proxy / Firewall / WAF)
                                      |
                              +-------------------+
                              | frontend (nginx)   |   sirve frontend/ estático
                              | :80/:443          |   + proxy reverso de la API
                              +-------------------+
                                 |            |
                  /actas /firma /auth /usuario /equipo
                  /uploads /generar-* /descargar-acta /health ...
                                 |            |
                                 v            v
                       +----------------+   +-------------------+
                       | backend:8001   |   | db (postgres:16)  |
                       | JDK 21 + LO    |   | SaucoDB:5432      |
                       +----------------+   +-------------------+
                            |                        |
                     ┌──────┴───────┐         ┌──────┴───────┐
                     │ sauco-storage│         │ sauco-pgdata │
                     │ (volumen)    │         │ (volumen)    │
                     └──────────────┘         └──────────────┘

Integración externa: GLPI (REST, red corporativa) · SMTP (correo)
```

**Por qué nginx tiene que hacer proxy de la API (y no solo servir estático):**

- El backend **solo** sirve `uploads/**` (por controladores con validación de acceso) y Swagger. Las páginas HTML del frontend no las sirve.
- El JS del frontend llama rutas en **raíz** (`/actas`, `/firma/…`, `/auth/…`, `/equipo/…`, `/generar-acta`, `/descargar-acta/**`). Con nginx proxy de esas rutas a `backend:8001`, las llamadas quedan **same-origin** → **no se dispara CORS** → el hardcode de `SecurityConfig` deja de ser un problema.
- Se inyecta `window.SAUCO_API = "/"` en las páginas (nginx `sub_filter`) para que `api.js` use el mismo origen en lugar del fallback `http://localhost:8001`.

> **Alternativa A (si se prefiere exponer API y frontend en dominios separados):** hay que hacer configurable el origen en `SecurityConfig.corsConfigurationSource()` (propiedad `app.cors.allowed-origins`, hoy lista hardcodeada) y apuntar `SAUCO_API` al dominio de la API. Más movimiento; no recomendado.

**Escalabilidad:** el aplicativo es de **réplica única forzosa** (cola asíncrona y rate-limit en memoria del proceso; conversión de PDF serializada por JVM). Escalar es **vertical** (más CPU/RAM al contenedor de backend), nunca horizontal sin re-diseñar la cola (por ejemplo, con RabbitMQ/Redis y conversión externalizada).

---

## 3. `Dockerfile` backend (propuesta)

```dockerfile
# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline -q
COPY src ./src
RUN mvn -B package -DskipTests -q

# ---- run ----
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends \
        libreoffice-writer \
        fonts-dejavu-core fonts-liberation \
        curl \
        ca-certificates tzdata \
    && rm -rf /var/lib/apt/lists/*

# libreoffice.headless en Linux usa una instalación portable
ENV LIBREOFFICE_PATH=/usr/lib/libreoffice/program/soffice \
    TZ=America/Bogota

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# `.env` opcional se monta en /app/.env (spring.config.import la lee si existe)
EXPOSE 8001
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8001/v3/api-docs | grep -qE '^(200|401|404)$' || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
```

Notas:
- `libreoffice-writer` trae el binario con el que se convierte; sin él, preflight mata la generación (`LibreOffice no encontrado en: ...`).
- `fonts-*` para que el PDF conserve el layout del template (una fuente faltante desplaza medidas y puede romper celdas).
- `curl` solo para `HEALTHCHECK`.
- El healthcheck acepta `200` (API pública viva con swagger abierto), `401` (swagger cerrado en prod), o `404` (app viva sin endpoint), porque **no existe `/health`**.
- `spring-boot-devtools` está como `runtime`, `optional=true`; se recomienda excluirlo del jar en prod (`excludeGroupIds` en `spring-boot-maven-plugin`).

---

## 4. `Dockerfile` frontend + nginx (propuesta)

```dockerfile
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend /usr/share/nginx/html
# Sauco: sin SPA, el nginx inyecta API_BASE al servir (ver nginx.conf)
```

```nginx
# nginx.conf  — sirve estático + proxy reverso same-origin de la API
server {
    listen 80;
    server_name _;

    # Inyecta API_BASE same-origin en todas las páginas HTML servidas.
    sub_filter_once on;
    sub_filter '</head>' '<script>window.SAUCO_API="/";</script></head>';

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /login.html;
    }

    # Rutas públicas del backend que el frontend llama en raíz.
    location ~ ^/(auth|actas|firma|usuario|equipo|usuarios-glpi|uploads|generar-acta|generar-devolucion|generar-formateo-seguro|descargar-acta|swagger|v3|health|auditoria|sesion)/ {
        proxy_pass http://backend:8001;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> Ajuste fino de rutas: revisar contra `SecurityConfig.PUBLIC_PATHS` + `@RequestMapping` reales de cada controller (lista en sección 11 del checklist si se quiere cerrar el proxy a lo mínimo).

---

## 5. `docker-compose.yml` (propuesta)

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: SaucoDB
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD requerida}
    volumes:
      - sauco-pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d SaucoDB"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  backend:
    build:
      context: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/SaucoDB
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET:?JWT_SECRET requerida}
      STORAGE_ROOT: /data/actas
      LIBREOFFICE_PATH: /usr/lib/libreoffice/program/soffice
      APP_DOCUMENTACION_PUBLICA: "false"
      FIRMA_URL_BASE: ${FIRMA_URL_BASE:?FIRMA_URL_BASE requerida}
      GLPI_URL: ${GLPI_URL}
      GLPI_APP_TOKEN: ${GLPI_APP_TOKEN:-}
      GLPI_USER_TOKEN: ${GLPI_USER_TOKEN:-}
      MAIL_HOST: ${MAIL_HOST:-}
      MAIL_PORT: ${MAIL_PORT:-587}
      MAIL_USERNAME: ${MAIL_USERNAME:-}
      MAIL_PASSWORD: ${MAIL_PASSWORD:-}
      MAIL_FROM: ${MAIL_FROM:-}
      MAIL_SMTP_AUTH: ${MAIL_SMTP_AUTH:-true}
      MAIL_SMTP_STARTTLS: ${MAIL_SMTP_STARTTLS:-true}
      MAIL_SMTP_TRUST: ${MAIL_SMTP_TRUST:-smtp-relay.brevo.com}
      RATE_LIMIT_TRUST_XFF: "true"   # detras de nginx de confianza
    volumes:
      - sauco-storage:/data/actas
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  frontend:
    build:
      context: .
      dockerfile: Dockerfile.frontend
    depends_on: [backend]
    ports:
      - "80:80"
    restart: unless-stopped

volumes:
  sauco-pgdata:
  sauco-storage:
```

---

## 6. Estructura de volúmenes (persiste / NO persiste)

| Ruta | ¿Persistir? | Contenido | Por qué |
|---|---|---|---|
| `DB` (`sauco-pgdata`) | ✅ **SÍ** | Todas las tablas de `SaucoDB` | Datos del negocio, auditorías, tokens |
| `STORAGE_ROOT/generated/` | ✅ **SÍ** | DOCX y ZIP generados | Re-generables, pero conservar evidencia |
| `STORAGE_ROOT/uploads/pdf/` | ✅ **SÍ** | PDF finales | Evidencia legal de las actas |
| `STORAGE_ROOT/uploads/firmas/` | ✅ **SÍ** | `firma_<id>.png` | Firma manuscrita del firmante |
| `STORAGE_ROOT/uploads/fotos/` | ✅ **SÍ** | `foto_<id>.jpg` | Fotografía del firmante |
| `STORAGE_ROOT/lo-profile/` | ✅ **SÍ** | Perfil compartido de LibreOffice | Evita re-crearlo en cada arranque; se limpia solo si queda `.lock` huérfano |
| `/app/app.jar` + dependencias | ❌ NO | Código y libs | Viven en la imagen |
| `classpath:plantillas/*` | ❌ NO | Templates DOCX | Dentro del JAR |
| `backend/target/` | ❌ NO | Artefactos de build | Destruibles, se regeneran |
| Dirs temporales del SO (`/tmp`) | ❌ NO | Trabajo de LO | Efímeros |
| Cola async en memoria | ❌ NO | Tareas `generacion-documental` | Se pierde al reiniciar; la recupera `ReintentoGeneracionService` desde `datosOriginales` |

**Regla crítica:** `STORAGE_ROOT` **debe** apuntar a un volumen montado de lectura-escritura y ser de **propiedad del usuario que corre la JVM**. `AppConfig` lanza `IllegalStateException` al arranque si `generated/` o `uploads/{pdf,firmas,fotos}` no se pueden crear; si `STORAGE_ROOT` no se configura, el default `${user.dir}/storage` cae dentro del contenedor efímero y **todo storage se pierde al recrear el contenedor**.

**Contenido del volumen `sauco-storage` después de operar:**

```
/data/actas/
├── generated/            # DOCX y ZIP
├── lo-profile/           # perfil compartido de LibreOffice
└── uploads/
    ├── pdf/
    ├── firmas/
    └── fotos/
```

**Las rutas en BD (`acta.ruta_pdf`, `evidencia.ruta_archivo`) son virtuales** (`uploads/...`) y el backend las resuelve contra `STORAGE_ROOT`; por eso mover la raíz de storage en un entorno ya usado `sauco-pgdata` NO rompe las rutas (solo hay que mover el árbol y conservar el esquema virtual).

---

## 7. Variables de entorno completas

### 7.1 Obligatorias (sin default: sin ellas la app **no arranca**)

| Variable | Propiedad | Qué rompe si falta |
|---|---|---|
| `DB_PASSWORD` | `spring.datasource.password` | Fail-fast: la app no arranca |
| `JWT_SECRET` | `security.jwt.secret` | Fail-fast: no arranca. Debe ser **Base64 de ≥ 256 bits** (≥ 44 caracteres) |
| `FIRMA_URL_BASE` | `app.firma-url-base` | Arranca OK, pero **el enlace del correo de firma queda roto** (email con URL vacía) |

### 7.2 Con default que hay que sobreescribir en Docker

| Variable | Default | En Docker |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/SaucoDB` (fijo en yaml) | Obligatoria: `jdbc:postgresql://db:5432/SaucoDB` |
| `DB_USERNAME` | `postgres` | Mantener si coincide |
| `STORAGE_ROOT` | `${user.dir}/storage` | **Obligatoria**: `/data/actas` (volumen) |
| `LIBREOFFICE_PATH` | `${user.home}/LibreOfficePortable/.../soffice.exe` (ruta Windows) | **Obligatoria**: `/usr/lib/libreoffice/program/soffice` |
| `APP_DOCUMENTACION_PUBLICA` | `true` | `false` en prod (cierra Swagger tras JWT) |
| `GLPI_URL` | `http://10.86.1.33/glpi/apirest.php` (IP corporativa) | Si cambiar, setear red que alcance GLPI |

### 7.3 Opcionales (default sensato en yaml)

`JWT_EXPIRATION` (ms), `GLPI_APP_TOKEN`, `GLPI_USER_TOKEN`, `GLPI_CONNECT_TIMEOUT_MS`, `GLPI_REQUEST_TIMEOUT_MS`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_SMTP_TIMEOUT_*`, `MAIL_SMTP_TRUST` (default Brevo → para otro relay sobreescribir), `FIRMA_TOKEN_EXPIRA_HORAS` (72), `FIRMA_OTP_EXPIRA_MINUTOS` (10), `FIRMA_OTP_SESION_MINUTOS` (30), `FIRMA_OTP_MAX_INTENTOS` (5), `FIRMA_OTP_MAX_REENVIOS` (3), `FIRMA_OTP_COOLDOWN_SEGUNDOS` (60), `RATE_LIMIT_LOGIN_MAX/SEGUNDOS`, `RATE_LIMIT_REGISTRO_MAX/SEGUNDOS`, `RATE_LIMIT_TRUST_XFF` (false; **true solo detrás del nginx propio**), `ADMIN_PROTEGIDO_USERNAME` (default `admin`), `TZ`.

### 7.4 Mapa de propiedad → variable (resumen del `application.yml`)

```
DB_USERNAME → spring.datasource.username        DB_PASSWORD → spring.datasource.password
JWT_SECRET → security.jwt.secret                JWT_EXPIRATION → security.jwt.expiration
RATE_LIMIT_* → security.rate-limit.*            GLPI_URL → glpi.url
GLPI_*_TOKEN → glpi.app-token / glpi.user-token STORAGE_ROOT → storage.root
FIRMA_URL_BASE → app.firma-url-base             FIRMA_*  → app.firma-*
ADMIN_PROTEGIDO_USERNAME → app.admin-protegido-username
MAIL_* → spring.mail.* y mail.smtp.*            LIBREOFFICE_PATH → libreoffice.path (relaxed binding)
APP_DOCUMENTACION_PUBLICA → app.documentacion.publica
SPRING_DATASOURCE_URL → spring.datasource.url
```

---

## 8. Base de datos desde cero

**No se corre ningún script de `backend/src/main/resources/sql/` en instalación fresca.**

Los 11 scripts existentes son **migraciones legacy** para bases ya pobladas:

| Script | Para qué | ¿Necesario en BD nueva? |
|---|---|---|
| `migracion_auditoria_acta_historial.sql` | Añade `tipo_evento` NOT NULL + backfill sobre tabla con filas | ❌ (`ddl-auto` la crea completa) |
| `migracion_auditoria_checklist_acta.sql` | CHECK constraint sobre tabla con datos | ❌ |
| `migracion_firma_tecnico.sql` | CHECK de `auditoria_sistema.tipo_evento` | ❌ |
| `migracion_tipo_acta_formateo.sql` | Nuevo <tipo> de acta | ❌ |
| `migracion_estados_generacion_asincrona.sql` | Estados `GENERANDO_DOCUMENTOS`/`GENERACION_FALLIDA` | ❌ |
| `correccion_usuario_actas_devolucion.sql` | Corrección de datos históricos | ❌ |
| `migracion_paso{1..5}_*.sql` | Normalización del modelo (drop/DROP legacy, FKs, `contenido_html`) | ❌ |

**Procedimiento desde cero:**

1. **Crear la BD:** `CREATE DATABASE SaucoDB;` (en el contenedor `db` la crea `POSTGRES_DB`).
2. **Arrancar el backend una vez** → `ddl-auto: update` crea las ~13 tablas del modelo actual (las 8 entidades de `acta`, `firma`, `usuario`, `rol`, `auditoria` y auxiliares).
3. **Seed de roles:** `DataInitializer` inserta `ADMINISTRADOR`, `TECNICO`, `AUDITOR` automáticamente si la tabla de roles está vacía. **No crea usuarios.**
4. **Primer administrador: manual** (ver sección 9).

---

## 9. Primer administrador después del reset (bootstrap)

**No existe bootstrap automático de usuario admin** en `DataInitializer`. El registro público (`POST /auth/register`) crea **siempre** `TECNICO` (el rol se decide en servidor, ver `AuthService.java:85-104`), y crear un `ADMINISTRADOR` por `POST /usuarios` exige autenticarse como `ADMINISTRADOR`. → La primera cuenta admin es un **huevo y la gallina** que se resuelve por SQL directo.

**Pasos:**

```bash
# 1. Generar hash BCrypt de la contraseña elegida (fires 2a/2b/2y; Spring checkpw acepta ambos)
python -c "import bcrypt; print(bcrypt.hashpw(b'ContrasenaInicial!2026', bcrypt.gensalt(rounds=10)).decode())"
```

```sql
-- 2. Insertar el admin (los nombres de columna son los reales de la entidad Usuario)
--    username 'admin' coincide con ADMIN_PROTEGIDO_USERNAME default → queda protegido.
INSERT INTO usuario
    (cedula, nombres, apellidos, nombre_usuario, correo, password_hash,
     cargo, empresa, lugar_trabajo, bloqueado, id_rol)
VALUES
    ('0000000001', 'Administrador', 'SAUCO', 'admin',
     'admin@coltefinanciera.com', '<bcrypt_hash>',
     'Administrador del sistema', 'Coltefinanciera', NULL, false,
     (SELECT id_rol FROM rol WHERE nombre = 'ADMINISTRADOR'));
```

3. **Entrar:** `login.html` → usuario `admin` + contraseña elegida → JWT para las rutas protegidas.
4. **Crear el resto del equipo** desde `usuarios.html` (o `POST /usuarios` con JWT de `admin`), asignando `TECNICO` / `AUDITOR`.

**Alternativa sin tocar SQL a mano:** como `auth/register` crea `TECNICO`, se podría subir el rol del primer técnico con un `UPDATE` puntual. El insert directo de arriba es el camino mínimo y determinista. (Idealmente esta brecha se cierra con un flag `app.bootstrap-admin.enabled` y credenciales por env — ver "ajustes previos".)

---

## 10. Dependencias dentro del contenedor backend

| Dependencia | Qué aporta | Cómo se instala |
|---|---|---|
| JDK 21 (JRE) | Runtime de Spring Boot | Imagen base `eclipse-temurin:21-jre` |
| LibreOffice Writer (`soffice`) | Conversión DOCX→PDF (calidad real del template) | `apt-get install libreoffice-writer` |
| Fuentes (DejaVu + Liberation) | Fidelidad de layout del PDF | `fonts-dejavu-core fonts-liberation` |
| `ca-certificates` / `tzdata` | TLS (GLPI/MAIL) y zona horaria consistente | `apt-get install ca-certificates tzdata` |
| `curl` | Healthcheck | apt |
| Driver PostgreSQL | JDBC → incluido en el fat-jar | `org.postgresql:postgresql` (dependencia de Maven) |
| Bash + utilidades base | Scripts de arranque | Incluidas en las imágenes oficiales |

**No hace falta** el resto del escritorio de LO ni `libreoffice --no-install-recommends` excede el paquete `writer`.

---

## 11. Checklist de validación en producción

> Ejecutar en una terminal, contra el entorno desplegado (`https://actas.coltefinanciera.com` como ejemplo).

| # | Ítem | Validación | Comando / ruta |
|---|---|---|---|
| 1 | Login admin | 200 + JWT | `POST /auth/login` |
| 2 | Usuarios | listar usuarios paginado | `GET /usuarios?page=0&size=10` (JWT admin) |
| 3 | Creación de acta ENTREGA | `GENERADA` + PDF + insert en `acta_historial` | `POST /generar-acta` |
| 4 | Devolución | `GENERADA` + PDF | `POST /generar-devolucion` |
| 5 | Formateo seguro | `GENERADA` | `POST /generar-formateo-seguro` |
| 6 | Envío + OTP | email con `firma.html?token=…` y OTP 6 dígitos | `POST /actas/{id}/enviar` |
| 7 | Firma por portal | firma PNG + foto JPG + `FIRMADA` + PDF regenerado | abrir el enlace público, validar OTP, firmar |
| 8 | Aprobación / rechazo | `APROBADA`/`RECHAZADA` + PDF final + evidencia | `POST /actas/{id}/aprobar` o `/rechazar` |
| 9 | Evidencias y descarga | PDF, firma, foto descargables | `GET /actas/{id}/... ` y rutas de `uploads/**` |
| 10 | Auditoría | eventos por acta y de seguridad | `GET /auditoria/...` (admin/auditor) |
| 11 | Generación asíncrona | `GENERANDO_DOCUMENTOS` → `GENERADA` en segundos | ver log del contenedor |
| 12 | Reintento + reencolado | matar el contenedor con una acta en `GENERANDO_DOCUMENTOS`, reiniciarlo, ver `Re-encolando N acta(s)` | `docker compose up -d backend` + `docker logs backend` |
| 13 | GLPI | consulta por serial devuelve marca/tipo/modelo/CPU | `GET /equipo/{serial}` |
| 14 | Fallbacks | `GENERACION_FALLIDA` deja acta terminal y reintento manual `POST /actas/{id}/reintentar-generacion` no la cuelga | manipular `datosOriginales` inválido o quitar LO temporalmente |

---

## 12. Riesgos

**Despliegue / configuración**

| Riesgo | Detalle | Mitigación |
|---|---|---|
| `STORAGE_ROOT` sin volumen | Default efímero → pérdida total de documentos en recreación de contenedor | Siempre montar volumen en `/data/actas` |
| `FIRMA_URL_BASE` vacía | Enlace del correo roto | Fijarla en el entorno |
| CORS hardcodeado | Origin de prod rechazado si API y frontend en distintos hosts | Mismo-origin vía nginx (recomendado), o parametrizar `allowed-origins` |
| `window.SAUCO_API` sin inyectar | El frontend llama `localhost:8001` del navegador del cliente | `sub_filter` de nginx inyectando `window.SAUCO_API="/"` |
| Swagger público | Exponen OpenAPI en prod | `APP_DOCUMENTACION_PUBLICA=false` |
| `RATE_LIMIT_TRUST_XFF=false` detrás de proxy | XFF no confiable → el límite cuenta al proxy, no al cliente; con proxy propio, activarla | si se activa, que solo exista el nginx como saltador |

**Docker**

| Riesgo | Detalle | Mitigación |
|---|---|---|
| LibreOffice sobrecarga | Conversión: JVM + LO en el mismo contenedor, semáforo serializa; timeout 120 s | RAM/CPU holgados; única réplica |
| Imagen grande | `maven + LO + JDK` | Multi-stage; solo `jre-jammy` en runtime |
| Fuentes faltantes | PDF con layout corrido | `fonts-dejavu fonts-liberation` |
| Perfil `lo-profile` corrupto | LO no arranca | El servicio tiene limpieza de `.lock` huérfano (`LibreOfficePdfService`) |

**DR / continuidad**

| Riesgo | Detalle | Mitigación |
|---|---|---|
| Sin respaldo en la app | No hay backup automático de storage ni de PG | Respaldo a nivel infra: dump PostgreSQL + copia del volumen `sauco-storage` |
| Pérdida de la cola async | Tareas encoladas se pierden si el contenedor muere | El reencolado al arranque (desde `datosOriginales`) reconstruye `GENERANDO_DOCUMENTOS` |
| `ADMIN_PROTEGIDO_USERNAME` distinto | Se protege al admin más antiguo por `id` si no se setea (default `admin`) | Dejar el default; documentar |
| Contraseñas en historial git | `Junio2026+` quedó en el historial (SEC-002): **debe rotarse** | Cambiar password PG + JWT + tokens GLPI; rotar los que se hayan expuesto |

---

## 13. Veredicto

### ❌ REQUIERE AJUSTES PREVIOS

**No hay un "Dockerfile listo" ni un "docker-compose listo" en el repo; la app es infra-desplegable, no switch-flippable.** El backend arranca con IAAS limpio y el modelo `ddl-auto` regenera el esquema solo, la recuperación async está resuelta, y las plantillas viven en el jar. Pero **6 ajustes concretos** bloquean producción:

1. **Primer admin sin bootstrap** — `DataInitializer` solo siembra roles; la primera cuenta `ADMINISTRADOR` se crea por SQL (sección 9). Cerrar con bootstrap opcional si se quiere automatizar.
2. **CORS hardcodeado a localhost** — para frontend y API en dominios/ports distintos no sirve; usar **same-origin vía nginx** (recomendado) o parametrizar `app.cors.allowed-origins`.
3. **`SAUCO_API` no se inyecta** — el JS hace fetch a `http://localhost:8001` por defecto; en prod hay que inyectar `window.SAUCO_API` (nginx `sub_filter`) o el navegador del cliente nunca hablará con el backend.
4. **No existe endpoint `/health`** — Docker `HEALTHCHECK` no tiene qué sondear; healthcheck por código HTTP (`200/401/404`) o añadir `spring-boot-starter-actuator` (recomendado, además da `health` y `metrics` para el WAF/CMS).
5. **Swagger público por defecto** — cerrar con `APP_DOCUMENTACION_PUBLICA=false`.
6. **`LIBREOFFICE_PATH` apunta a una ruta Windows** — en la imagen hay que setarla al `soffice` de Linux; sin eso, la generación de PDF falla en runtime.

**Plus:** exportar el nombre del jar a `app.jar` (o usar multi-stage `COPY --from=build target/*.jar`), excluir `spring-boot-devtools` del jar final, y rotar los secretos históricos (`Junio2026+`) antes de promocionar.

Con esos 6 ajustes + la infraestructura de las secciones 3–5, SAUCO queda **LISTO PARA DOCKER** en una sola réplica (backend + nginx frontend + postgres), con storage y esquema reconstruibles desde cero en minutos.