# Auditoría y normalización del modelo de datos de SaucoDB

- **Fecha:** 2026-09-03
- **Bases de datos:** `SaucoDB` — PostgreSQL 18 (localhost:5432)
- **Contexto:** sistema de gestión de actas GLPI (Coltefinanciera), backend Spring Boot 3.4.1 + JPA `ddl-auto: update`
- **Estado:** **SOLO AUDITORÍA Y PLAN — ninguna migración ejecutada.** Los scripts SQL se revisan antes de aplicar.

---

## 1. Objetivo

Inventariar el modelo físico completo, correlacionar cada columna con su uso real a través de
las 9 áreas (BD, entities JPA, DTOs, controllers, services, frontend, generación DOCX,
generación PDF, auditoría), clasificar todo como **CONSERVAR / OBSOLETO / SOSPECHOSO**,
detectar columnas heredadas o reemplazadas por `datos_originales`, y proponer un modelo final
con plan de limpieza por fases.

## 2. Método

Evidencia recopilada, no suposiciones:

1. Esquema físico completo vía `information_schema` (14 tablas, 120 columnas).
2. Constraints reales vía `pg_constraint` (101: PK, UNIQUE, NOT NULL, FK, CHECK).
3. Conteos de filas por tabla y histograma de NULL sobre `acta` (n=90).
4. Tamaños físicos reales vía `pg_total_relation_size` + sequences (`information_schema.sequences`).
5. Barrido de referencias por capa:
   - Entities JPA (13 archivos bajo `com.empresa.actas.*.entity` + `security/JwtRevocado`).
   - DTOs (`dto/response`, `firma/dto`).
   - Controllers/services que leen o escriben cada columna (getters/setters traceados).
   - Frontend (`frontend/js/{app,actas,acta-view,firmas,firma,formateo,devolucion,perfil,usuarios}.js` y HTML).
   - Plantillas DOCX: placeholders inyectados por `DocumentoWordService` (`eq_N_*`, `hw_N_*`, `ot_N_*`,
     `chk_N_si/no`, `win10/win11/macos`, `{{dia/mes/anio}}`, `{{entregado_por}}`, `{{entrega_por}}`, `{{responsable_verificacion}}`, …).
   - Generación PDF: `PdfService` (fallback OpenPDF), `LibreOfficePdfService` (ruta principal DOCX→PDF).
   - Auditoría: `auditoria_sistema`, `acta_historial`, `AuditoriaConsultaService`.

## 3. Inventario de tablas

| # | Tabla | Filas | Entity JPA | Integración real | Clasificación |
|---|-------|-------|------------|------------------|---------------|
| 1 | `acta` | 90 | `Acta` | CRUD, ciclo de vida, regeneración, portal | **CONSERVAR** |
| 2 | `acta_historial` | 310 | `ActaHistorial` | trazabilidad documental | **CONSERVAR** |
| 3 | `auditoria_sistema` | 754 | `AuditoriaSistema` | auditoría operacional/seguridad (18 enums) | **CONSERVAR** |
| 4 | `evidencia` | 97 | `Evidencia` | firmas, fotos, PDF final | **CONSERVAR** |
| 5 | `usuario` | 21 | `Usuario` | auth JWT, roles | **CONSERVAR** |
| 6 | `usuario_firma` | 3 | `UsuarioFirma` | firma permanente del técnico | **CONSERVAR** |
| 7 | `firma_token` | 49 | `FirmaToken` | enlace de firma (UUID, expiración) | **CONSERVAR** |
| 8 | `firma_otp` | 28 | `FirmaOtp` | OTP BCrypt, intentos, sesión | **CONSERVAR** |
| 9 | `jwt_revocado` | 2 | `JwtRevocado` | denylist JWT (SEC-011) | **CONSERVAR** |
| 10 | `rol` | 3 | `Rol` | ADMINISTRADOR/TECNICO/AUDITOR | **CONSERVAR** |
| 11 | `asignacion` | **0** | — | — | **OBSOLETA** |
| 12 | `dispositivo` | **0** | — | — | **OBSOLETA** |
| 13 | `marca` | **0** | — | — | **OBSOLETA** |
| 14 | `tipo` | **0** | — | — | **OBSOLETA** |

Las tablas 11-14 son el modelo original de inventario manual (GLPI local). No tienen entity,
repository, query nativa ni uso en frontend. Su cadena FK es interna
(`asignacion.id_dispositivo → dispositivo.id_marca → marca.id_tipo → tipo`), sin ninguna
referencia entrante desde tablas vivas.

## 4. Inventario de columnas

### 4.1 `acta` (35 columnas) — el foco del problema

Escritura verificada por flujo:

- **V1 async:** `DocxActaService.persistirActaEnProceso` (ENTREGA, L176), `DevolucionService`
  (DEVOLUCION, L171), `FormateoSeguroService` (FORMATEO, L170).
- **V2 manual:** `ActaService.crearActa` (L75).
- **Ciclo de vida:** `FirmaService` (enviar/firmar/rechazar), `ActaService` (aprobar/rechazar).

| Columna | Poblado (n=90) | Escritura | Lectura funcional | Clasificación |
|---|---|---|---|---|
| `id_acta` | PK | JPA | repository, mapper | **CONSERVAR** |
| `id_tecnico` | 90 | 4 flujos | `findByIdTecnico`, filtro ROL TECNICO | **CONSERVAR** |
| `ticket_glpi` | 35 | ENTREGA (`numero_sac` L179), V2 | repo.buscar, `PdfService:164`, `FirmaService:302`, actas/firmas/firma.js | **CONSERVAR** |
| `tipo_acta` | 90 | 4 flujos | CHECK + repos + mappers | **CONSERVAR** |
| `estado` | 90 | ciclo de vida | `findByEstado`, re-encolado async | **CONSERVAR** |
| `cedula_usuario` | DEVOLUCIÓN/V2 | esos flujos | actas.js:438, firmas.js:353, firma.js:400, buscar | **CONSERVAR** |
| `nombre_usuario` | 90 | 4 flujos (regla por tipo) | repo.buscar, listados, detalle, portal, historial | **CONSERVAR** |
| `correo_usuario` | ~45 | 4 flujos + `FirmaService:151` | **destino enlace firma** (FirmaService/OtpService), firmas.js | **CONSERVAR** |
| `serial_equipo` | 90 | `primerSerial` | buscar, frontend | **CONSERVAR** |
| `placa_equipo` | 90 | `primerInventario` | buscar, frontend | **CONSERVAR** |
| `descripcion_equipo` | 90 | `descripcionEquipo` | buscar, frontend | **CONSERVAR** |
| `contenido_html` | 7 | V2 (sanitizado) | `ActaService:175`, `FirmaService:306` | **CONSERVAR** |
| `ruta_pdf` | 83 | async, firma, aprobación | descarga, evidencia PDF_FINAL | **CONSERVAR** |
| `ruta_pdf_checklist` | 6 | async ENTREGA, `SignedDocumentService` | portal `rutaPdfChecklist` | **CONSERVAR** |
| `ruta_zip` | 5 | V1 (nombreZip) | `/descargar-acta` | **CONSERVAR** |
| `datos_originales` | 77 | 4 flujos + `ActaService:93` | **fuente regeneración documento firmado** | **CONSERVAR — crítico** |
| `fecha_creacion` | 90 | JPA | auditoría, listados | **CONSERVAR** |
| `fecha_envio` | 49 | `FirmaService:168` | mapper, display | **CONSERVAR** |
| `fecha_firma` | 33 | `FirmaService:414` | mapper, display | **CONSERVAR** |
| `fecha_aprobacion` | 21 | `ActaService:347` | mapper, display | **CONSERVAR** |
| `fecha_rechazo` | 4 | `ActaService:378`/`FirmaService:331` | FirmaPublicaResponse, PdfService | **CONSERVAR** |
| `observacion_rechazo` | 5 | idem | idem + `PdfService:170` | **CONSERVAR** |
| `id_asignacion` | **0** | nunca | solo whitelist sort `ActaController:58` | **OBSOLETA** |
| `marca_modelo` | **0** | nunca | solo `PdfService:197` (nunca true) | **OBSOLETA** |
| `procesador` | **0** | nunca | solo `PdfService:198` | **OBSOLETA** |
| `memoria_ram` | **0** | nunca | solo `PdfService:199` | **OBSOLETA** |
| `disco_duro` | **0** | nunca | solo `PdfService:200` | **OBSOLETA** |
| `sistema_operativo` | **0** | nunca | solo `PdfService:201` | **OBSOLETA** |
| `monitor` | **0** | nunca | solo `PdfService:202` | **OBSOLETA** |
| `accesorios` | **0** | nunca | solo `PdfService:203` | **OBSOLETA** |
| `estado_equipo` | **0** | nunca | solo `PdfService:204` | **OBSOLETA** |
| `cargo` | **0** | nunca | solo `PdfService:184` | **OBSOLETA** |
| `lugar_trabajo` | **0** | nunca | solo `PdfService:185` | **OBSOLETA** |
| `empresa` | **0** | nunca | solo `PdfService:186` | **OBSOLETA** |
| `observaciones` | **0** | nunca | solo `PdfService:205` | **OBSOLETA** |

> **Total obsoletas en `acta`: 13 columnas** (todas con 0/90 valores). Las 12 de hardware/ubicación
> heredaron del esquema V1 (una columna por atributo de equipo). El flujo moderno persiste el
> request completo en `datos_originales` y regenera el documento desde ahí; la generación DOCX
> se alimenta del JSON del request (`eq_N_*`, `hw_N_*`, `chk_N_*`, `{{entregado_por}}`, …),
> **jamás de estas columnas**. Frontend: 0 referencias. Único lector: `PdfService.generarPdfFinal`,
> fallback para 13 actas legacy sin `datos_originales`, donde la condicional de nulidad nunca imprime nada.

### 4.2 Resto de tablas (origen de las 120 - 35 columnas de acta)

Todas sus columnas están en uso; sin obsoletas.

- **`usuario` (13):** todas vivas. `cargo/empresa/lugar_trabajo` se escriben en registro/perfil
  (`UsuarioService:140-142`) y se muestran en `usuarios.html`/`perfil.js`. `cedula`,
  `nombre_usuario`, `correo` UNIQUE usados por auth/JWT.
- **`acta_historial` (11):** todas vivas. ⚠️ **Redundancia conceptual:** `usuario_accion`
  (NOT NULL, username) convive con `actor_id` + `actor_nombre`. La lectura usa `actorNombre`
  con fallback a `usuario_accion` (`AuditoriaConsultaService:105-110`); la escritura puebla
  ambos (`ActaService:162-164`). Normalizable en fase opcional.
- **`auditoria_sistema` (10):** vivas; 18 tipos en CHECK (754 eventos).
- **`evidencia` (5):** vivas; 4 tipos con datos (FOTO 33, FIRMA 33, PDF_FINAL 30, CHECKLIST_FINAL 1).
- **`firma_token` (7):** vivas; `fecha_expiracion` = vencimiento del enlace (default 72 h).
- **`firma_otp` (11):** vivas; `sesion` (UNIQUE) gatea `X-OTP-Sesion`, `intentos`, `codigo_hash` BCrypt.
- **`usuario_firma` (5), `jwt_revocado` (4), `rol` (2):** vivas.

## 5. Mapa de dependencias

**`datos_originales` es el núcleo** del flujo documental:

```
datos_originales ──▶ DOCX ──▶ ZIP (ruta_zip) ──▶ PDF (ruta_pdf) ──▶ evidencia (PDF_FINAL)
                         └──▶ PDF checklist (ruta_pdf_checklist) ──▶ evidencia (CHECKLIST_FINAL)
                         └──▶ firma embebida (SignedDocumentService) ──▶ regenera PDF firmado
```

- Consumidores de `datos_originales`: `SignedDocumentService` (L96/151/162, regeneración post-firma),
  reintentos async de `DocxActaService`/`DevolucionService`/`FormateoSeguroService` (L149/147/146),
  `ActaService.datosOriginalesO` (serialización de respaldo QA-04).
- **Sin esta columna no hay documento firmado reproducible.**

- Columnas usadas en búsquedas server-side (`ActaRepository.buscar`): `estado`, `tipoActa`,
  `nombreUsuario`, `descripcionEquipo`, `serialEquipo`, `placaEquipo`, `cedulaUsuario`,
  `idActa`, `ticketGlpi`.
- Portal público (`FirmaPublicaResponse`): `tipoActa`, `estado`, `rutaPdf`, `rutaPdfChecklist`,
  `nombreUsuario`, `cedulaUsuario`, `correoUsuario`, `descripcionEquipo`, `serialEquipo`,
  `placaEquipo`, `ticketGlpi`, `contenidoHtml`, `fechaRechazo`, `observacionRechazo`.
- Frontend listados/detalle (`actas.js`, `acta-view.js`, `firmas.js`, `firma.js`): conjunto igual
  al del portal, más `tipoActa`, `fechaEnvio`, `fechaFirma`, `fechaAprobacion`.
- Plantillas DOCX: **0 dependencias** de columnas de `acta`; todas las variables vienen del request
  (ya persistido en `datos_originales`).
- Auditoría: `acta_historial` (ciclo de vida) y `auditoria_sistema` (eventos op/sec/security).

**Dependencias de las 13 columnas obsoletas de `acta`:** en exclusiva `PdfService.generarPdfFinal`
(condiciones `!= null` jamás verdaderas en datos actuales) y `ActaController:58` (whitelist de
`sort` para `id_asignacion`).

## 6. Candidatos a eliminación y riesgo

### Columnas de `acta` (13)

| # | Columnas | Evidencia | Riesgo datos | Riesgo código | Mitigación |
|---|---|---|---|---|---|
| C1 | `id_asignacion` | 0/90 NULL, sin FK | **Nulo** | **Bajo** | quitar de sort whitelist + entity |
| C2-C13 | `marca_modelo, procesador, memoria_ram, disco_duro, sistema_operativo, monitor, accesorios, estado_equipo, cargo, lugar_trabajo, empresa, observaciones` | 0/90 NULL, 0 frontend, 0 DOCX, 0 búsqueda | **Nulo** | **Medio** | refactor previo de `PdfService` (leer `datos_originales`) |

### Tablas legacy (4)

| # | Tablas | Evidencia | Riesgo |
|---|---|---|---|
| T1-T4 | `asignacion`, `dispositivo`, `marca`, `tipo` | 0 filas, sin entity/query | **Nulo** (rollback reconstruible de la estructura) |

Junto a ellas caen sus índices (7: `asignacion_pkey`, `dispositivo_pkey`,
`dispositivo_numero_serie_key`, `dispositivo_numero_placa_key`, `marca_pkey`, `tipo_pkey`,
`tipo_nombre_key`) y 4 sequences (`asignacion_id_asignacion_seq`, `dispositivo_id_dispositivo_seq`,
`marca_id_marca_seq`, `tipo_id_tipo_seq`).

### Fase opcional

| # | Elemento | Evidencia | Riesgo |
|---|---|---|---|
| C14 | `acta_historial.usuario_accion` | redundante con `actor_id`/`actor_nombre` (fallback en lectura) | **Bajo** — backfill previo de `actor_id`/`actor_nombre` |

**No se eliminan por más de lo que parecen:** `contenido_html` (V2 lo escribe, portal lo lee),
`ruta_zip` (5/90, V1 persiste y descarga), `ticket_glpi` (55 NULL es diseño de DEVOLUCIÓN/FORMATEO).

## 7. Scripts de migración (revisar antes de aplicar)

Los tres scripts están en `backend/src/main/resources/sql/`. **Ninguno ha sido ejecutado.**

### PASO 1 — `migracion_paso1_eliminar_legacy.sql`

| Atributo | Valor |
|---|---|
| **Riesgo** | MUY BAJO — 0 filas en las 4 tablas; `id_asignacion` 0/90 NULL; sin dependencias de código |
| **Dependencias** | misma release que la limpieza de `ActasActa.idAsignacion` en `Acta.java` y de la whitelist de `ActaController`. No toca datos |
| **Rollback** | bloque comentado al final del script (recrea tabla/columna/sequences) |
| **Tablas afectadas** | `acta` (1 columna), `asignacion`, `dispositivo`, `marca`, `tipo` (DROP) |
| **Validaciones** | `SELECT` post-check dentro del script (10 tablas, `acta` 34 columnas, sin `id_asignacion`) |

### PASO 2 — `migracion_paso2_drop_columnas_acta_obsoletas.sql`

| Atributo | Valor |
|---|---|
| **Riesgo** | BAJO con prerequisito cumplido; MEDIO si se omite el refactor |
| **Dependencias** | **requiere ANTES** el refactor Java de `PdfService.generarPdfFinal` (fuente `datos_originales` en lugar de getters de entidad) + rebuild `mvn clean` + smoke test de aprobación. El script lo aborta si detecta datos en las columnas |
| **Rollback** | `ADD COLUMN` con lengths originales (bloque comentado) |
| **Tablas afectadas** | `acta` (12 columnas: marca_modelo, procesador, memoria_ram, disco_duro, sistema_operativo, monitor, accesorios, estado_equipo, cargo, lugar_trabajo, empresa, observaciones) |
| **Validaciones** | `SELECT count(*)` post-check: 22 columnas en `acta`; generación de acta ENTREGA/DEVOLUCION/FORMATEO; portal de firma; búsqueda |

### PASO 3 (fase opcional) — `migracion_paso3_normalizar_acta_historial.sql`

| Atributo | Valor |
|---|---|
| **Riesgo** | BAJO — backfill idempotente con pre-check de cobertura |
| **Dependencias** | requiere quitar `usuarioAccion` de `ActaHistorial.java` y el fallback `getUsuarioAccion()` en `AuditoriaConsultaService:110`, en la misma release |
| **Rollback** | `ADD COLUMN usuario_accion varchar NOT NULL` con backfill desde `actor_nombre` (bloque comentado) |
| **Tablas afectadas** | `acta_historial` (drop `usuario_accion`) |
| **Validaciones** | `SELECT` post-check: 10 columnas; query de historial en la UI de actas (panel Historia) y de auditoría |

## 8. Requisito previo de código (PASO 2)

`PdfService.generarPdfFinal` es el único lector de las 12 columnas de hardware/ubicación. Hoy
genera el PDF "final" del acta desde la entidad. Cambio propuesto:

- Leer del JSON de `datos_originales` (claves del request: `entregado_por/entregado_a`,
  `cargo_*`, `asunto`, `equipos[].{marca,modelo,serial,inventario}`, etc.), misma fuente que el DOCX.
- Para las 13 actas legacy **sin** `datos_originales`, el PDF queda básico (cabecera + campos
  disponibles), comportamiento idéntico al actual (esas columnas ya están NULL).
- Con el refactor mergeado y validado, se eliminan los 12 getters y se aplica el SQL del PASO 2.

## 9. Estimación de reducción del modelo

### 9.1 Volumetría estructural (medible)

| Métrica | Hoy | Post PASO 1+2 | Delta |
|---|---|---|---|
| Tablas | 14 | 10 | **−28,6 %** |
| Columnas | 120 | 88 | **−26,7 %** |
| Constraints | 101 | 76 | **−24,8 %** |
| Índices legacy | 7 | 0 | −7 |
| Sequences | 8 | 4 | −4 |

Con PASO 3 (opcional): **87** columnas, **75** constraints.

### 9.2 Espacio físico (pg_total_relation_size, medición real)

| Tabla | Bytes hoy |
|---|---|
| `auditoria_sistema` | 352 256 |
| `acta` | 139 264 |
| `acta_historial` | 122 880 |
| `usuario` | 81 920 |
| `firma_token` | 73 728 |
| `evidencia` | 65 536 |
| `firma_otp` | 49 152 |
| `usuario_firma` | 40 960 |
| `rol` + `jwt_revocado` | 65 536 |
| **subtotal vivas** | **≈ 991 232 B (~968 KB)** |
| `dispositivo` | 32 768 |
| `marca` | 8 192 |
| `tipo` | 16 384 |
| `asignacion` | 8 192 |
| Índices legacy (7) | 57 344 |
| Sequences legacy (4) | 32 768 |
| **subtotal legacy (PASO 1)** | **≈ 155 648 B (~152 KB)** |

**Interpretación honesta:** el ahorro físico es **modesto (~152 KB de un total real de ~1,12 MB)**
porque en PostgreSQL una columna NULL no ocupa almacenamiento de datos (solo hasta ~2 bytes de
bitmap por tupla con ese perfil de nulidad). El beneficio de la limpieza NO es bytes:

1. **Modelo más simple:** 14 columnas y 4 tablas menos; cada `INSERT` de acta deja de escribir
   13 columnas NULL; menos superficie de migración y de confusión (¿qué columna es canónica?).
2. **Cero ambigüedad de fuente de verdad:** `datos_originales` pasa a ser la única fuente
   documental, eliminando el doble almacenamiento conceptual que representaban.
3. **Menor superficie de auditoría:** 25 constraints legacy menos.
4. **Alto de tuplas:** bitmap menor por tupla (margininal); esperado en el futuro con volúmenes grandes.

## 10. Deuda relacionada detectada (fuera del alcance de esta limpieza)

| Ítem | Detalle | Acción sugerida |
|---|---|---|
| `DocxToPdfService.java` | fallback OpenPDF definido pero **nunca invocado** | eliminar clase + test de referencia |
| `frontend/templates/` inexistente | "Vista Previa" de Entrega/Devolución rota (QA-42); el HTML viejo usa vars (`marcaModelo`, `procesador`, …) que no existen en ningún template real | recuperar template o quitar el botón |
| FKs sueltas | `evidencia.id_acta`, `firma_token.id_acta`, `firma_otp.id_token_firma`, `acta_historial.id_acta`, `acta.id_tecnico` no tienen FK (gestor de integridad): con `acta.id_tecnico` apuntando a usuarios ya borrados | fase independiente: backfill + `ADD CONSTRAINT` |
| `word/` vacío, `graphify-out/` | caché/herramientas, fuera del modelo | — |

## 11. Registro de decisiones (ADR)

| Decisión | Justificación |
|---|---|
| Drop de `asignacion/dispositivo/marca/tipo` | 0 filas, sin integración, sin dependencias entrantes |
| Drop de las 13 columnas de `acta` | 0 pobladas; reemplazadas por `datos_originales`; solo lector es fallback legado |
| Conservar `contenido_html` | lo escribe V2 (sanitizado) y lo consume el portal de firma |
| Conservar `ruta_zip`/`ruta_pdf_checklist` | usados por descarga y evidencia |
| Conservar `ticket_glpi` | 35 valores vivos; NULL en DEVOLUCIÓN/FORMATEO es diseño |
| `usuario_accion` diferido a fase opcional | requiere refactor de lectura y backfill de 310 filas; no bloquea |
| Sin `varchar` → `text` ni cambios de tipo | no aportan a la limpieza; YAGNI |

---

*Fin del documento. Ningún script de migración ha sido ejecutado; todos están listos para
revisión en `backend/src/main/resources/sql/`.*