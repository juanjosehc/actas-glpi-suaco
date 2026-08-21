# Actas GLPI

Sistema para la generación automatizada de documentos Word (DOCX) de actas de entrega y devolución de activos tecnológicos, integrado con GLPI para la consulta de equipos.

## Descripción

El sistema permite a personal de TI generar actas oficiales y listas de chequeo a partir de datos capturados en un formulario web. Los documentos se generan en formato DOCX, se empaquetan en ZIP y se descargan automáticamente al navegador.

### Tipos de documento generados

| Tipo | Archivos ZIP | Descripción |
|------|-------------|-------------|
| **Acta de Entrega** | `ActaLista_{serial}_{asunto}.zip` | Acta de entrega + Lista de chequeo (2 DOCX) |
| **Acta de Devolución** | `Devolucion_{serial}_{motivo}.zip` | Acta de devolución (1 DOCX) |

## Requisitos previos

- Java 21
- Maven 3.8+
- Node.js (opcional, para Tailwind CSS)
- Cuenta activa en instancia GLPI con permisos de API REST

## Estructura del proyecto

```
actas-glpi/
├── backend/                    # API REST (Spring Boot 3.4.1)
│   ├── pom.xml                 # Dependencias Maven
│   └── src/main/
│       ├── java/com/empresa/actas/
│       │   ├── ActasApplication.java
│       │   ├── config/         # CORS, variables de entorno
│       │   ├── controller/     # Endpoints REST
│       │   ├── dto/            # Request/Response DTOs
│       │   ├── exception/      # Manejo global de errores
│       │   └── service/        # Lógica de negocio
│       └── resources/
│           ├── application.yml
│           └── plantillas/     # Templates DOCX
├── frontend/                   # Interfaz web (HTML/CSS/JS)
│   ├── css/                    # Estilos (Tailwind + CSS custom)
│   ├── js/                     # Lógica JavaScript
│   └── pages/                  # Páginas HTML
├── plantillas/                 # Templates DOCX (copia de referencia)
├── generados/                  # Directorio de ZIPs generados
└── .env                        # Variables de entorno (GLPI tokens)
```

## Instalación

### 1. Backend

```bash
cd backend
mvn clean package -DskipTests
```

### 2. Variables de entorno

Crear archivo `.env` en la raíz del proyecto:

```
GLPI_URL=http://tu-servidor-glpi/glpi/apirest.php
GLPI_APP_TOKEN=tu-app-token
GLPI_USER_TOKEN=tu-user-token
```

### 3. Ejecutar

```bash
cd backend
mvn spring-boot:run
```

El servidor arranca en `http://127.0.0.1:8001`.

### 4. Frontend

Abrir directamente en el navegador:

```
frontend/pages/acta-entrega.html
frontend/pages/acta-devolucion.html
```

O usar Live Server de VS Code en el puerto 5500.

## Almacenamiento de archivos (producción)

Los datos de las actas se guardan en PostgreSQL. Los archivos físicos (PDF, DOCX/ZIP, firmas PNG, fotos JPG) se guardan en un almacenamiento permanente configurable — **no** en directorios temporales del SO.

### Configuración

La raíz del almacenamiento se define con una sola clave: `storage.root` (o la variable de entorno `STORAGE_ROOT`).

| Entorno | Configuración |
|---------|---------------|
| Desarrollo | `STORAGE_ROOT` sin definir → `{directorio de trabajo}/storage` |
| Windows producción | `STORAGE_ROOT=D:/ActasStorage` (o editar `storage.root` en `application.yml`) |
| Linux | `STORAGE_ROOT=/opt/actas-storage` o `/data/actas` |
| Docker | `-e STORAGE_ROOT=/data/actas` + volumen montado en `/data/actas` |

Estructura resultante:

```
storage.root/
├── generated/        # DOCX y ZIP generados
└── uploads/
    ├── pdf/          # PDF finales
    ├── firmas/       # Firmas PNG
    └── fotos/        # Fotografías JPG
```

Las rutas guardadas en la base de datos (`uploads/...`) son virtuales y no cambian al mover la raíz. El backend sirve los archivos vía `/uploads/**` contra el directorio configurado.

> En producción asegúrese de que el directorio tenga permisos de escritura para el usuario del proceso y respaldo (backup) periódico, ya que es el archivo documental permanente.

### Migración desde directorios temporales antiguos

Si existían archivos en el almacenamiento anterior (`%TEMP%/actas_glpi_*` o `backend/uploads`), migrelos antes de poner en marcha la nueva configuración:

```bash
python tools/migrate_storage.py --dry-run          # ver qué se movería
python tools/migrate_storage.py                     # migrar (temp del SO)
# destinos antiguos explícitos (ej. desarrollo con carpeta local en el repo):
python tools/migrate_storage.py --old-generated backend/generados --old-uploads backend/uploads --root /data/actas
```

El script copia los archivos preservando la estructura `generated/` y `uploads/{pdf,firmas,fotos}/`, no sobrescribe archivos existentes y es re-ejecutable. Las rutas en PostgreSQL siguen siendo válidas; no requieren actualización.

### Docker (referencia)

```yaml
services:
  backend:
    build: ./backend
    environment:
      - STORAGE_ROOT=/data/actas
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/SaucoDB
    volumes:
      - actas-storage:/data/actas   # persistencia documental permanente

volumes:
  actas-storage:
```

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/generar-acta` | Genera acta de entrega + checklist |
| `POST` | `/generar-devolucion` | Genera acta de devolución |
| `GET` | `/equipo/{serial}` | Consulta equipo en GLPI por serial |
| `GET` | `/descargar-acta/{nombreZip}` | Descarga el ZIP generado |

## Tecnologías

**Backend:**
- Java 21
- Spring Boot 3.4.1
- Apache POI 5.2.5 (manipulación DOCX)
- Jackson (serialización JSON)
- Lombok
- Jakarta Validation
- dotenv-java 3.2.0

**Frontend:**
- HTML5, CSS3, JavaScript vanilla
- Tailwind CSS 4.3.3
- FlyonUI 2.4.1 (componentes UI)
- Flatpickr (selector de fechas)

## Notas importantes

- El frontend está hardcodeado a `http://127.0.0.1:8001`. El backend **debe** ejecutarse en el puerto 8001.
- Los tokens de GLPI en `.env` están excluidos del repositorio (`.gitignore`).
- Los archivos generados se guardan en el almacenamiento permanente configurado por `storage.root` (ver sección anterior) — ya no en el directorio temporal del sistema.
- Los PDF finales se generan en `uploads/pdf/`; los DOCX y ZIP en `generated/`.
