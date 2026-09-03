# Propuesta técnica — Generación asíncrona de actas

**Fecha:** 2026-09-02 · **Estado:** a validar por el equipo (sin implementar).
**Objetivo:** que el usuario vea su acta de inmediato (< 1 s) y que los documentos (DOCX, ZIP, PDF, checklist) se generen en segundo plano.

---

## 1. Resumen ejecutivo

| Hoy (POST bloqueante) | Propuesto (POST asíncrono) |
|---|---|
| ENTREGA ~30.7 s · DEVOLUCIÓN ~9.0 s · FORMATEO ~7.2 s | Respuesta **< 1 s** en los 3 casos |
| El acta se persiste **después** del PDF (10 ms al final) | El acta se persiste **al inicio** (estado `GENERANDO_DOCUMENTOS`) |
| El 200 llega cuando ya existe PDF + ZIP | El 200 llega cuando existe la fila en DB; PDF/ZIP llegan después |
| Usuario mira una página congelada sin feedback | Listado muestra la acta al instante con badge "Generando…" |

La generación real no se toca: **DOCX → firma → ZIP → PDF** queda igual, solo se mueve a un hilo en segundo plano. El 90–96 % del tiempo es conversión LibreOffice, y eso no bloquea la respuesta.

---

## 2. Qué no cambia, qué sí

### No cambia (sin riesgo de regresión)
- Plantillas DOCX, motor `DocxTemplateEngine`, `DocumentoWordService`, `ZipService`.
- Conversión LibreOffice (`LibreOfficePdfService`, preflight, 2 intentos, timeout 120 s, semáforo global).
- Contrato de **entrada** de los 3 endpoints (`@Valid` igual).
- Persistencia de `datosOriginales`, auditoría `ActaHistorial`, portal de firma.
- Roles y permisos.

### Sí cambia (el diseño tiene la forma abajo)
- 3 servicios orquestadores: se parten en **"persistir y responder"** + **"generar en segundo plano"**.
- Nuevo estado intermedio + un estado terminal de error.
- Frontend: ya no auto-descarga el ZIP a ciegas; consulta el listado y muestra estado.

---

## 3. Nuevos estados

Se agregan dos valores al enum `EstadoActa`:

| Estado | Semántica | Badge |
|---|---|---|
| **`GENERANDO_DOCUMENTOS`** | La acta está en DB; DOCX/ZIP/PDF aún no existen o están en proceso | "Generando…" (ámbar, spinner) |
| **`GENERACION_FALLIDA`** | El proceso en segundo plano terminó en error (p.ej. falta LibreOffice) | "Error de generación" (rojo) |

**¿Por qué `GENERANDO_DOCUMENTOS` y no `PROCESANDO`/`GENERANDO_PDF`?**
- Los estados actuales son participios pasados que describen un **hito completado** (`GENERADA`, `ENVIADA`, `FIRMADA`…). El par natural para "en curso" en español es el gerundio. `GENERANDO_DOCUMENTOS` es auto-documentado en el historial: un lector entiende qué se estaba haciendo exactamente.
- `PROCESANDO` es más corto pero ambiguo (¿procesando qué?). `GENERANDO_PDF` está de más — el flujo genera DOCX, ZIP y PDF; además el checklist se convierte a PDF y el nombre quedaría incompleto.
- Recomendación: `GENERANDO_DOCUMENTOS` en DB, etiqueta de UI "Generando…" con spinner.

### Migración SQL (obligatoria — el CHECK `acta_estado_check` SÍ existe en SaucoDB)

```sql
ALTER TABLE acta DROP CONSTRAINT acta_estado_check;
ALTER TABLE acta ADD CONSTRAINT acta_estado_check CHECK (
    estado IN ('GENERADA','ENVIADA','FIRMADA','APROBADA','RECHAZADA',
               'GENERANDO_DOCUMENTOS','GENERACION_FALLIDA')
);
```

Se versiona como `backend/src/main/resources/sql/migracion_async_generacion.sql`, ejecutada a mano (mismo patrón que `migracion_auditoria_acta_historial.sql`). `ddl-auto: update` **no** puede agregar valores al CHECK sobre tabla con datos.

> Nota además en `TipoEventoActa`: si se quiere registrar también el fallo en el historial con un tipo propio (`GENERACION_FALLIDA`), hace falta el mismo patrón de migración sobre el CHECK de `auditoria_sistema.tipo_evento`. Durante la implementación debo verificar los valores actuales de `TipoEventoActa` y el CHECK de `auditoria_sistema` antes de tocar nada.

---

## 4. Diseño del flujo asíncrono

### Patrón a replicar: ya existe en el código

`FirmaController` hace exactamente esto desde antes:

```java
private final ExecutorService firmaPdfExecutor =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "firma-pdf");
            t.setDaemon(true);   // no impide shutdown de la JVM
            return t;
        });

// tras registrar la firma, el PDF firmado se regenera en segundo plano:
firmaPdfExecutor.execute(() -> signedDocumentService.generarDocumentoFirmado(token));
```

La generación de actas copia este patrón con un executor **propio de generación** (un solo hilo es suficiente: el cuello es LibreOffice, que ya está serializado por su semáforo global).

### Secuencia propuesta (los 3 orquestadores siguen el mismo esquema)

```
POST /generar-acta (o /generar-devolucion, /generar-formateo)
│
│  [hilo HTTP]  validar @Valid (rápido, igual que hoy)
│              capturar idTecnico + firma del técnico (SecurityContext ANTES del async)
│              persistirActa(estado GENERANDO_DOCUMENTOS, datosOriginales, nombreZip calculado)
│              → respuesta 200 {success, idActa, estado}         ← < 1 s
│
└► [hilo "gen-actas"]  generar DOCX → firma técnico (si hay) → ZIP
                       → convertir PDF (acta y, si ENTREGA, checklist)
                       → update acta: estado=GENERADA, rutaPdf, rutaPdfChecklist, rutaZip
                       → historial ACTA_GENERADA
                       → si error: estado=GENERACION_FALLIDA + historial error
```

### Contrato de respuesta (cambio de comportamiento)

```json
{
  "success": true,
  "idActa": 120,
  "estado": "GENERANDO_DOCUMENTOS",
  "nombre_zip": null,
  "ruta_pdf": null,
  "mensaje": "Acta creada. Documentos en generación."
}
```

- `nombre_zip` y `ruta_pdf` vienen `null` en la respuesta inmediata porque aún no existen; pasan a tener valor cuando el estado llega a `GENERADA`.
- El frontend actual asume que tras el POST puede descargar `result.nombre_zip` — eso se elimina (ajuste del paso 6).
- El ZIP conserva su nombre canónico (`ActaLista_<serial>_<asunto>_<sufijo>.zip`), que se calcula y persiste de antemano; cuando termina la generación se puede pedir por `GET /actas/{id}/zip`.

### Nuevo endpoint para descargar el ZIP por id (en vez de por nombre)

```java
@GetMapping("/actas/{id}/zip")                    // JWT
public ResponseEntity<?> descargarZip(@PathVariable Long id)
```

- Lee la fila, usa `acta.getRutaZip()` (nombre del archivo), lo resuelve contra `generatedDir` con la misma contención de ruta de `descargar-acta/{nombreZip}` (getFileName + startsWith), y lo sirve.
- Devuelve 404 si aún no existe (`estado != GENERADA` o archivo ausente).
- `descargar-acta/{nombreZip}` se puede conservar para compatibilidad y evitar romper enlaces previos.

### Modelo: una columna nueva

```java
@Column(name = "ruta_zip", length = 255)
private String rutaZip;   // nombre del ZIP, null mientras GENERANDO_DOCUMENTOS
```

`ddl-auto: update` la agrega sola.

---

## 5. Consideraciones de robustez (no se omiten)

1. **`SecurityContext` en el hilo async.** `tecnicoAutenticado()` y `obtenerFirmaBytesDe(...)` leen del `SecurityContextHolder`, que es `ThreadLocal` y **no** se propaga al hilo del executor. El `idTecnico` y los bytes de la firma del técnico se capturan **antes** de encolar el trabajo y se pasan como argumento al método async. Sin esto, el documento del técnico saldría sin firma (regresión).
2. **Transaccionalidad.** El guardado inicial (`GENERANDO_DOCUMENTOS`) es una transacción corta que hace commit. El update a `GENERADA` + registro de historial se hace en otra transacción al terminar. Hoy el `@Transactional` del orquestador abarca también los 30 s de conversión — en el diseño async el hilo de DB queda libre durante ese tiempo (mejora, no regresión).
3. **Huérfanas por reinicio.** Si la JVM cae con actas en `GENERANDO_DOCUMENTOS`, al arrancar un `ApplicationRunner` las detecta y las re-encola con sus `datosOriginales` (o las marca `GENERACION_FALLIDA` si el fallo persiste). Esto evita actas "congeladas" en listado para siempre.
4. **Reintentos.** `LibreOfficePdfService` ya tiene 2 intentos por conversión; se conserva. Un reintento de *toda* la generación (tras `GENERACION_FALLIDA`) puede ser un botón "Reintentar generación" que re-encola con los `datosOriginales`. Propuesto como adición opcional; no bloquea la v1.
5. **Semáforo LibreOffice.** Se mantiene: nunca dos conversiones simultáneas, sin importar cuántas actas se estén generando en segundo plano. El executor de un solo hilo hace que las actas queden en cola FIFO — comportamiento predecible.

---

## 6. Gestión de actas — cómo se ve mientras se genera

### Listado (`actas.html` / `actas.js`)

- Nueva entrada en `getBadgeClass`:
  - `GENERANDO_DOCUMENTOS` → `badge--PROCESANDO` (ámbar) con texto "Generando…" + spinner (CSS de FlyonUI ya tiene spinners).
  - `GENERACION_FALLIDA` → badge rojo "Error de generación".
- Botón "Descargar ZIP/PDF": deshabilitado mientras `GENERANDO_DOCUMENTOS`, habilitado cuando `GENERADA`.
- Botón "Enviar": **deshabilitado** mientras `GENERANDO_DOCUMENTOS` (no tiene sentido enviar una acta que aún no tiene PDF). `FirmaService` además debe rechazar `enviar` si `estado != GENERADA` (guard a nivel de servicio, no solo de UI).
- **Refresco tipo "pull-lite"**: tras `loadActas()`, si hay alguna acta `GENERANDO_DOCUMENTOS`, programar `setTimeout(loadActas, 3000)` — el listado se refresca solo hasta que todo deja de generar. Coste trivial (GET /actas son 25–35 ms). Cuando desaparece el último `GENERANDO_DOCUMENTOS`, se detiene el polling.

### Detalle (`acta-view.html`)

- Si `estado == GENERANDO_DOCUMENTOS`: sección "Documentos" con skeleton + "Generando documentos…"; botones de descarga ocultos; mismo poll de 3 s.
- Si `GENERACION_FALLIDA`: banner rojo con el error y (si v1 lo incluye) botón "Reintentar generación".

### Estados posteriores intactos

- `ENVIADA`, `FIRMADA`, `APROBADA`, `RECHAZADA` siguen igual. La firma del portal público ocurre sobre un acta ya `GENERADA` con su PDF listo.

---

## 7. Pantalla de carga — UX del POST

Propuesta al pie de la letra: **"Generando Acta"** con ícono ✓.

1. El usuario pulsa "Generar" → overlay fijo (posición absoluta, fondo translúcido, spinner de FlyonUI) con texto **"Generando actas…"**.
2. `fetch` POST → vuelve con `{ success, idActa, estado GENERANDO_DOCUMENTOS }` en < 1 s.
3. Inmediatamente: el overlay cambia a **"Acta creada ✓"** y se navega a `actas.html` (la acta ya figura en el listado con badge "Generando…").
4. El poll del listado la ve pasar a `GENERADA` en segundos (30 s el peor caso ENTREGA); cuando eso ocurre, un toast "Documentos listos ✓" acompaña el cambio de badge.

Diseño del componente (en `ui.js` o como patrón reutilizable con FlyonUI):
- `mostrarGenerando()` → overlay con spinner + "Generando actas…".
- `mostrarActaCreada()` → mismo overlay, ícono `✓` verde, "Acta creada".
- Auto-descarte al navegar (el polling del listado toma el relevo).

No se necesita un spinner que dure 30 s: el objetivo es que el POST vuelva antes de que el usuario perciba espera. El "Generando…" del listado es la pantalla de carga *real* del procesamiento en segundo plano (y por eso necesita status + polling: es la fuente de verdad).

---

## 8. Checklist 2.4× — resultado de la investigación

**Pregunta:** ¿por qué el PDF del checklist (20.9 s) es 2.4× más lento que el del acta (8.5 s) en el mismo ENTREGA?

### Estructura de la plantilla (análisis XML del DOCX)

| Métrica | `"Acta de Entrega 2 2 - copia.docx"` | `ListaChequeo.docx` |
|---|---|---|
| document.xml | 90 KB | **192 KB** (~2×) |
| Runs de texto | 197 | **569** (~2.9×) |
| Celdas de tabla | 85 | **196** (~2.3×) |
| `<tcBorders>` | 12 | **87** (~7.3×) |
| Secciones (`sectPr`) | 1 | **2** |
| Referencias header/footer | 1 | **6** (3 × 2 secciones) |
| Páginas del PDF | 1 (96 K) | **2 (248 K)** |

La plantilla es objetivamente más pesada: más tablas de checkboxes, 36 casillas pintadas con bordes por celda y dos secciones con header/footer duplicados.

### Pero la medición aislada desmiente a la plantilla

Corridas aisladas de `soffice` con el **mismo** perfil reutilizado:

```
CHECKLIST  perfil frío (nuevo)               13.5 s
CHECKLIST  mismo perfil, 2ª conversión        7.6 s
CHECKLIST  perfil ya cálido                   2.9 s   ← MÁS rápido que el acta
ACTA       perfil cálido (misma corrida)      6.2 s
```

Con perfil cálido, **el checklist convierte más rápido que el acta**. El costo dominante no es la plantilla: es que `LibreOfficePdfService` crea un `lo-profile-*` temporal **nuevo por conversión** (línea `Files.createTempDirectory("lo-profile-")`) y lo borra al terminar. Cada conversión paga el arranque en frío del perfil (cache de fuentes, extension manager, escritorio), que en producción es del orden de ~6–13 s y es el grueso del tiempo.

El 20.9 s del checklist en producción = **perfil frío** (arranque) **+ plantilla pesada** ensamblada sobre un motor todavía frío + varianza. El 2.4× no es un defecto de `ListaChequeo.docx`; es la suma de un arranque en frío por conversión (×2 en ENTREGA) con una plantilla más compleja que paga más caro ese arranque.

### Conclusión y recomendación concreta

1. **No rehacer la plantilla del checklist** — no es el cuello de botella real.
2. La palanca fuerte es **reutilizar el perfil de LibreOffice** (sección 9): con un solo perfil cálido, ENTREGA pasa de ~29.5 s de conversión a ~9–10 s (acta 6.2 + checklist 2.9 aislados), y la varianza frío cae.
3. Optimizaciones de plantilla **secundarias** (solo si después del perfil reutilizado aún hiciera falta, y con medición): reducir los `tcBorders` por celda del checklist usando bordes de tabla (de ~87 a ~13 bloques), reducir a una sola sección si el diseño lo tolera. Son optimizaciones de layout, no de conversión.

---

## 9. LibreOffice: reutilizar el perfil (con mediciones)

### Hallazgo confirmado

`LibreOfficePdfService.convertirDocxAPdf`:
- lanza un `ProcessBuilder` **nuevo por conversión** (proceso osoffice nuevo siempre);
- usa `Files.createTempDirectory("lo-profile-")` + `-env:UserInstallation` — **perfil nuevo y aislado por conversión**;
- lo borra en `finally { borrarDirectorio(profileDir) }`.

Nunca se reutilizan proceso ni perfil. El arranque en frío del perfil se paga en cada conversión.

### Medición del beneficio de reutilizar el perfil

```
CHECKLIST perfil frío → cálido:   13.5 s → 7.6 s → 2.9 s   (≈ 4.7× en el peor frío)
ACTA      perfil cálido:           6.2 s
Saving típico por convertir en perfil ya cálido: ~1.8× (13.5 → 7.6) a ~4× (13.5 → 2.9)
```

### Propuesta — Fase 1 (bajo costo, bajo riesgo): perfil fijo reutilizado

Cambiar `LibreOfficePdfService` para usar **un solo perfil persistente y reutilizado** bajo el storage root, p.ej. `storage/lo-profile`:

- El perfil se crea una vez (primera conversión) y se reutiliza en todas las siguientes.
- `-env:UserInstallation` apunta al directorio fijo (formateado como `file:///` URL, como hoy).
- Se mantiene el **semáforo global**: nunca dos procesos con el mismo perfil a la vez (evita corrupción de locks). Con un solo hilo de generación + regeneraciones de firma, la exclusión mutua no cambia.
- Manejo de locks huérfanos: si un proceso muere y deja el lock del perfil, en el siguiente arranque se limpia el `.lock` antes de usar (patrón conocido de LO headless: borrar `registrymodifications.xcu` lock o el directorio de usuario si queda bloqueado). En el peor caso se borra el perfil y se deja recrear (pierde caché, no datos del usuario).
- Fallo a perfil temporal aislado como *fallback*: si la conversión con perfil compartido falla 2 veces, reintentar una vez con perfil nuevo (igual preserva el comportamiento actual ante un perfil corrupto).

Estimación de impacto en tiempos de conversión:

| Flujo | Hoy (producción) | Con perfil reutilizado (est.) |
|---|---|---|
| ENTREGA | 29.5 s | ~9–12 s |
| DEVOLUCIÓN | 8.1 s | ~3–6 s |
| FORMATEO | 6.8 s | ~3–6 s |

Combinado con la sección 4, el usuario deja de *esperar* el POST por completo (responde < 1 s); convertir la respuesta "no se siente" es entonces opcional y secundario.

### Propuesta — Fase 2 (opcional, más inversión): mantener un proceso LibreOffice vivo

Alternativa/evolución: un único `soffice` en modo escucha (p.ej. `--accept=socket,host=localhost,port=2002;urp;` — protocolo UNO) o usar la suite de conversión persistente de LO, para no pagar ni el spawn del proceso ni el arranque del perfil. Mucho más trabajo (cliente UNO, gestión de sesiones, reconexión) para un margen adicional que tras la Fase 1 ya es pequeño (6.2 → 2.9 s). **No recomendada** para la v1.

---

## 10. Archivos a tocar (mapa de implementación)

| Archivo | Cambio |
|---|---|
| `backend/src/main/java/com/empresa/actas/acta/entity/EstadoActa.java` | + `GENERANDO_DOCUMENTOS`, `GENERACION_FALLIDA` |
| `backend/src/main/resources/sql/migracion_async_generacion.sql` (NUEVO) | CHECK `acta_estado_check` + opcional CHECK `auditoria_sistema.tipo_evento` |
| `backend/.../acta/entity/Acta.java` | + columna `ruta_zip` |
| `backend/.../acta/repository/ActaRepository.java` | + query para actas `GENERANDO_DOCUMENTOS` (re-encolado) |
| `backend/.../service/DocxActaService.java` | partir `generarActa`: persistir-inicio + tarea async + finalizar |
| `backend/.../service/DevolucionService.java` | idem |
| `backend/.../service/FormateoSeguroService.java` | idem |
| `backend/.../config/` (NUEVO, o campo static en cada servicio) | `ExecutorService` de generación ("gen-actas", daemon, 1 hilo) |
| `backend/.../acta/controller/ActaController.java` | + `GET /actas/{id}/zip`; (los POST de generar no cambian de ruta) |
| `backend/.../firma/service/FirmaService.java` | guard: `enviar` solo si `estado == GENERADA` |
| `backend/.../service/LibreOfficePdfService.java` | **Fase 1**: perfil fijo reutilizado + fallback perfil temporal |
| `backend/.../` ApplicationRunner (NUEVO o en `config/DataInitializer`) | re-encolar `GENERANDO_DOCUMENTOS` huérfanas al arranque |
| `frontend/js/actas.js` | badge + botones por estado nuevo + poll 3 s |
| `frontend/js/acta-view.js` | skeleton "Generando…" + poll + pie de error |
| `frontend/js/app.js` | POST ya no descarga ZIP; navega a listado; overlay "Generando acta… ✓" |
| `frontend/js/ui.js` | utilidades de overlay + toast "Documentos listos" |

---

## 11. Prioridad y fases de entrega

1. **Fase 0 — Migración DB**: CHECK de estados + columna `ruta_zip` (sin código). Permite validar SQL sobre SaucoDB sin tocar la app.
2. **Fase 1 — Async**: estados, executor, partición de los 3 servicios, `GET /actas/{id}/zip`, guards, re-encolado, frontend (badge/poll/overlay). La conversión sigue siendo perfil-temp (resultado correcto, solo se mueve de lugar).
3. **Fase 2 — Perfil LO reutilizado**: `LibreOfficePdfService` perfil fijo + fallback. Baja la conversión ~2–4×, mejora el tiempo real "a lista" para el peor caso ENTREGA.
4. **Fase 3 (opcional)** — sofisticación: reintentar desde `GENERACION_FALLIDA`, botón "Reintentar generación", daemon LO.

Cada fase es entregable e independiente; la Fase 1 ya cumple el objetivo central del usuario (< 1 s de respuesta y acta visible al instante).

---

## 12. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Actas `GENERANDO_DOCUMENTOS` huérfanas tras reinicio de la JVM | ApplicationRunner las re-encola o marca `GENERACION_FALLIDA` al arrancar |
| `SecurityContext` perdido en el hilo async → firma del técnico ausente | Capturar `idTecnico` + bytes de la firma antes de encolar; pasarlos como argumento |
| El frontend actual auto-descarga el ZIP y quedaría roto | Ajuste explícito del flujo POST + nuevo endpoint `GET /actas/{id}/zip`; conservar `descargar-acta/{nombreZip}` |
| Dos conversiones simultáneas con perfil compartido corrompen el perfil | Semáforo global ya lo impide; fallback a perfil temporal aislado + limpieza de locks |
| `RUTA_ZIP` se pide antes de existir | 404 claro; UI deshabilita el botón hasta `GENERADA` |
| `FirmaService.enviar` sobre `GENERANDO_DOCUMENTOS` | Guard a nivel de servicio (`estado == GENERADA`), no solo UI |

---

*Documento de propuesta — no ha habido cambios de código. La base es `INFORME_RENDIMIENTO_GENERACION_ACTAS.md` (mediciones de producción) y pruebas aisladas de `soffice` con perfil frío vs. cálido.*