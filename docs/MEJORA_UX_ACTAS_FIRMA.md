# Mejora Integral de UX en Generación de Actas y Firma Electrónica

Sistema: **SAUCO — Gestión de Actas de Entrega/Devolución (Coltefinanciera)**
Fecha: **2026-09-07** — Java 5.6

## Entregable (10 puntos)

1. Causa exacta de recargas repetidas
2. Lugares donde ocurren
3. Correcciones realizadas
4. Evidencia de reducción de reloads
5. Nuevo flujo visual de firma
6. Pantalla de procesamiento implementada
7. Evidencia de experiencia antes/después
8. Impacto en rendimiento percibido
9. Validación auditoría OWASP intacta
10. Confirmación sin regresiones de seguridad

---

## 1. Causa exacta de las recargas/navegaciones repetidas

El parpadeo "recarga sin parar" al **generar** un acta y al **firmar** tuvo
**dos causas acumuladas**, ambas confirmadas por reproducción headless real
(puppeteer-core + Edge, contador de cargas persistente en `sessionStorage`,
trazas CDP `Page.frameScheduledNavigation`/`Page.frameNavigated`).

### 1a. Causa original parcial — JS antiguo en caché (corregida en Java 5.5)

Los `<script src>` de las 14 páginas no llevaban `?v=`. Un navegador con sesión
abierta seguía ejecutando los **JS viejos cacheados** (con reloads y flujos V2
navegando de más) pese a que el repo ya estaba limpio de `location.reload`.

| Página | Antes | Después |
|---|---|---|
| `app.js`, `devolucion.js`, `formateo.js` y módulos de gestión | Sin `?v` → versión cacheada vieja | `?v=20260907` |
| `firma.js` | `?v=20260825` | `?v=20260907` (bump) |

`grep` global de scripts sin `?v` → 0 resultados. Esto eliminó la ejecución de
JS obsoletos, pero el usuario **reportó que el parpadeo continuaba** — quedaba
un segundo origen:

### 1b. Causa raíz confirmada — Live Reload de "Live Server" recargando por escrituras del backend

Reproducción A/B del mismo flujo (login JWT + llenado del formulario + clic en
**Generar Acta**) contra dos servidores estáticos:

| Servidor | Resultado observado |
|---|---|
| **Live Server de VS Code** en `:5500` (puerto documentado en CLAUDE.md) | `SCHEDULED reason=reload` sobre `acta-entrega.html` repetido; contador de cargas crece sin límite (9 → 14 en 30 s); la navegación única a `actas.html` queda **pisada** |
| **`python -m http.server 8080`** (estático, sin livereload) | **Una sola navegación**: `SCHEDULED reason=scriptInitiated → actas.html`; contador estable en 3; cero reloads |

Secuencia causal exacta:

1. El usuario usa **Live Server Go Live** (`:5500`) para abrir el frontend. La
   extensión inyecta en cada página un cliente con **Live Reload** (confirmado
   por `console.log("Live reload enabled.")` en la consola del documento).
2. Al clic en **Generar Acta**, el POST `/generar-acta` persiste el acta y el
   backend **escribe archivos dentro del workspace** (`backend/storage/generated/`,
   `backend/storage/uploads/…`, logs). `backend/storage` está bajo el directorio
   que **Live Server vigila**.
3. Live Server detecta los cambios de archivo y ordena al navegador
   `location.reload()` (`SCHEDULED reason=reload`). Esto recarga `acta-entrega.html`.
4. El código del repo navega al listado **una vez** (`setTimeout(600) →
   `actas.html`, `reason=scriptInitiated`), pero la recarga del livereload la
   pisa: la página vuelve a cargar, el backend sigue generando (Varios DOCX/ZIP
   asíncronos), Live Server vuelve a recargar → **"recarga sin parar"**.
5. Al **firmar**: `POST /firma/{token}` guarda `firma_<id>.png`, `foto_<id>.jpg`
   y el PDF final en `storage/uploads/`. El mismo mecanismo recarga `firma.html`
   a mitad del procesamiento → el firmante cree que "la firma falló" y recarga
   a mano (los "Navigated to" que el usuario ve).

**Conclusión:** el código del repo hace actualización silenciosa y **una sola
navegación** (verificado en `:8080`). La "recarga sin parar" restante es del
**Live Reload ambiental del entorno de desarrollo**, no del aplicativo. En
producción (servidor de estáticos sin livereload) no ocurre.

### 1c. Origen histórico V2 (eliminado en Sprint 4)

`generar-acta.html` (V2) y la "Vista Previa" navegaban múltiples veces hacia
`frontend/templates/*.html` que **nunca existieron** (QA-42). Ese flujo fue
eliminado; no contribuye al síntoma actual.

### 1d. Carencia de feedback de la firma (PROBLEMA 2, corregida en Java 5.5)

El botón **Firmar Acta** arrancaba el POST sin pantalla de procesamiento; 3-10 s
de regeneración de documentos sin feedback empujaban al usuario a recargar
creyendo un fallo, y a dobles envíos (token de un solo uso → 400 en el segundo
POST).

## 2. Lugares donde ocurren (auditoría completa del frontend)

Auditoría exhaustiva de `frontend/js/` + `frontend/pages/`:

| Módulo | Hallazgo | Estado |
|---|---|---|
| `app.js` / `devolucion.js` / `formateo.js` (generadores) | Botón `type="button"` + `onclick`, sin `<form>`. Tras POST: toast + `setTimeout(600) → actas.html`. **Una sola navegación** (confirmada en `:8080`). | Correcto |
| `actas.js` | Listado `fetch` + render parcial (`renderTable`), polling 3 s (fetch, sin navegación). Sin `location.reload`. | Correcto |
| `acta-view.js` | Detalle `loadPdfViewer`, placeholder `GENERANDO_DOCUMENTOS`, polling 3 s. Redirección pública por token → `firma.html` (seguridad). | Correcto |
| `firmas.js` | Listado `fetch` + modals, `await loadActas()` silencioso. | Correcto |
| `firma.js` | **Falta de feedback durante el POST de firma**; `btnReject`/`btnOtpReenviar` activos durante envío. | **Corregido** |
| `login-ui.js` / `home.js` / `admin-layout.js` / `usuarios.js` / `perfil.js` / `cambiar-password.js` / `auditoria.js` | Redirección por auth única. Sin loop. | Correcto |

**Grep de verificación:** `location.reload` → **0** en todo el frontend. Sin
meta-refresh. `setInterval` solo en countdown OTP (cuenta regresiva, no navega).

Los `reason=reload` repetidos **no los emite el código del repo** (0 reloads en
grep y sin `location.reload()` en el stack del navegador): los emite el cliente
de Live Reload inyectado por Live Server.

## 3. Correcciones realizadas

### `frontend/pages/firma.html` (Java 5.5)
- **Estado `#stateProcessing`**: spinner + *"Estamos finalizando su firma. Por
  favor espere mientras se actualizan los documentos y evidencias asociadas."*
- Estado `#stateSuccess`: *"Firma registrada correctamente. Los documentos y
  evidencias ya fueron actualizados."*

### `frontend/js/firma.js` (Java 5.5)
- `mostrarProcesando()`: bloquea `btnSubmit`, `btnReject`, `btnOtpReenviar`,
  detiene el countdown OTP y muestra la pantalla de procesamiento.
- Guardia `procesandoFirma` en `submitFirma` y `submitRechazo` (evita dobles
  envíos y el 400 del token de un solo uso).

### Cache-bust global (Java 5.5)
- Todas las etiquetas `<script src="../js/*.js">` de las 14 páginas con
  `?v=20260907`.

### Live Reload del entorno (Java 5.6) — causa raíz
- **`.vscode/settings.json`**: `liveServer.settings.ignoreFiles =
  ["**/storage/**", "**/target/**", "**/*.log"]`. Con esto la extensión Live
  Server ignora las escrituras del backend y **deja de ordenar el reload** al
  generar y al firmar (se aplica al abrir el workspace del repo en VS Code;
  no requiere reiniciar el navegador).
- **CLAUDE.md**: nueva nota de entorno con la causa y cómo evitarla (servir por
  otro puerto: `python -m http.server 8080` — permitido por CORS — o usar el
  `ignoreFiles`).

## 4. Evidencia de reducción de reloads

`location.reload` → 0 en el frontend. Verificación A/B instrumentada del flujo
completo (login + llenado + clic en Generar):

```
—— Live Server :5500 (con livereload) ——
schedule: reload  acta-entrega.html     (loop: 9→14 cargas en 30 s)
schedule: reload  acta-entrega.html
schedule: scriptInitiated  actas.html   (pisada por el reload siguiente)

—— Servidor plano :8080 (sin livereload) ——
schedule: scriptInitiated  actas.html   (1 sola navegación)
contador de cargas: 2 → 3 (goto + destino), estable
```

La navegación del repositorio es **una por acción**: post-generación → listado;
post-firma → estado de éxito en la misma pantalla (sin navegación).

## 5. Nuevo flujo visual de firma (antes/después)

### ANTES
1. Firma + foto + (checklist) → clic **Firmar Acta**.
2. Botón `.loading` ("Enviando firma..."); 3-10 s de procesamiento **sin
   feedback global**, y Live Reload **recargaba la página** al guardarse firma/
   foto/PDF en `storage`.
3. Usuario: "parece que falló" → recarga manual → repite acciones.

### DESPUÉS
1. Clave → clic **Firmar Acta**; la guardia `procesandoFirma` bloquea doble envío.
2. **Pantalla de procesamiento** (spinner + mensaje). `btnReject`, `btnOtpReenviar`
   y `btnSubmit` deshabilitados; countdown OTP detenido.
3. Sin recargas ambientales: `storage` fuera del watch de Live Server.
4. Al completarse: **"Firma registrada correctamente. Los documentos y
   evidencias ya fueron actualizados."** En error: se restaura la tarjeta y se
   rehabilitan los controles.

## 6. Pantalla de procesamiento implementada

Estado nuevo `#stateProcessing` en `firma.html` (reutiliza `.firma-state`):

```html
<div class="firma-state" id="stateProcessing" style="display:none">
  <div class="spinner-lg"></div>
  <h2 class="state-title">Procesando Firma</h2>
  <p class="state-text">Estamos finalizando su firma. Por favor espere
     mientras se actualizan los documentos y evidencias asociadas.</p>
  <p class="state-text state-text--muted">No cierre esta ventana ni recargue
     la pagina.</p>
</div>
```

Controlado por `mostrarProcesando()` / `ocultarProcesando()` en `firma.js`.

## 7. Impacto en rendimiento percibido

- **Generación**: el parpadeo a repetido era el Live Reload + la recarga de cada
  navegación. Con `ignoreFiles`/servidor plano queda **una sola navegación**:
  POST → toast → listado.
- **Firma**: antes "falló/parece colgado" + recargas manuales + dobles envíos;
  ahora estado visible que avanza y concluye. Se eliminan recargas ambientales y
  latencia percibida.
- Cero requests extra en el frontend (la pantalla de procesamiento es DOM puro).

## 8. Validación auditoría OWASP intacta

Cambios acotados a: `firma.html` (markup de estado), `firma.js` (feedback/DOM +
guardia de duplicidad), `.vscode/settings.json` (config de editor local) y
documentación. **Ningún archivo de backend o de seguridad tocado:**

| Item | Estado |
|---|---|
| **JWT** (`jjwt`, `SecurityConfig`, `JwtAuthenticationFilter`) | Sin cambios |
| **OTP** (validación, expiración, envío, cooldown, reenvíos) | Sin cambios |
| **Password Reset / Recuperación / Cambio contraseña** | Sin cambios |
| **Protección XSS** | Pantalla nueva usa `textContent`; textos estáticos del HTML |
| **Path Traversal** | Sin cambios |
| **Validaciones OWASP / GlobalExceptionHandler** | Sin cambios |
| **Auditoría** | Sin cambios |
| **Autorización / Roles** | Sin cambios |
| **Rate Limiting** | Sin cambios |
| **CSP / Tokens de firma (un solo uso)** | Sin cambios; la guardia refuerza el no-uso repetido |
| **Rutas públicas** (`/firma/*`, `/uploads/*`) | Sin cambios |

## 9. Confirmación: sin regresiones de seguridad

- **Sin superficies de ataque**: el estado nuevo es markup estático; la lógica
  de seguridad (otpHeaders, `X-OTP-Sesion`, token de un solo uso) intacta.
- **Sin bypass**: la guardia de duplicidad no salta validaciones ni reduce
  verificaciones; solo evita un segundo POST simultáneo.
- **Sin exposición de información**: mensajes de negocio estáticos, sin IDs,
  correos ni rutas.
- **Sin relajación de controles**: firma y rechazo siguen exigiendo OTP válido y
  token no usado.
- `.vscode/settings.json` es configuración local del editor, no toca la app.

## 10. Resumen de archivos modificados

| Archivo | Cambio |
|---|---|
| `frontend/pages/firma.html` | Estado `#stateProcessing` + mensaje de éxito actualizado |
| `frontend/js/firma.js` | `mostrarProcesando`/`ocultarProcesando` + guardia `procesandoFirma` en firma y rechazo |
| `frontend/pages/*.html` (14 páginas) | Cache-bust `?v=20260907` en todos los `<script src="../js/...">` |
| `.vscode/settings.json` | `liveServer.settings.ignoreFiles` → `storage`, `target`, logs (causa raíz del parpadeo) |
| `CLAUDE.md` | Nota de entorno: causa del parpadeo y cómo evitarlo |
| `docs/MEJORA_UX_ACTAS_FIRMA.md` | Este documento (actualizado con causa raíz y evidencia A/B) |

Backend **sin cambios**. Auditoría OWASP intacta. Sin regresiones.

## 11. Cómo reproducir el diagnóstico (instrumentación usada)

- Puppeteer-core + Edge headless, `Page.frameScheduledNavigation`/`frameNavigated`/`Network.*`,
  contador de cargas persistente en `sessionStorage['__lc']` (sobrevive reloads).
- Experimento clave: mismo script, dos orígenes — Live Server `:5500` vs
  `python -m http.server 8080`. Solo el primero muestra `reason=reload` en bucle.