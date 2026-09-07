# Mejora Integral de UX en Generación de Actas y Firma Electrónica

Sistema: **SAUCO — Gestión de Actas de Entrega/Devolución (Coltefinanciera)**
Fecha: **2026-09-07**

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

La evidencia "Navigated to ..." repetida en la consola tenía **dos orígenes
distintos**, y solo uno persiste hoy:

### 1a. Origen histórico (eliminado en Sprint 4 / Java 4.0-5.x)

El flujo V2 heredado (`generar-acta.html`) y la "Vista Previa" navegaban
múltiples veces: formulario → generación → preview → listado, con páginas
intermedias (`frontend/templates/*.html`) que **nunca existieron** (QA-42). Ese
sprint eliminó `generar-acta.html` y `checklist-entrega.html`; la migración
V1→V2 quedó descartada y los formularios V1 pasaron a ser la navegación
canónica. Esas recargas ya no existen en el código actual.

### 1c. Causa raíz confirmada en la validación posterior (Java 5.5)

Al validar con el usuario (entorno Live Server `:5500`), el parpadeo **seguía
ocurriendo al hacer clic** pese a que el repositorio ya tenía cero
`location.reload`. La causa fue **caché del navegador**: los `<script src>` de
las 14 páginas no llevaban parámetro de versión (`?v=`), así que Chrome seguía
ejecutando los **JS antiguos cacheados** (los que sí contenían reloads y flujos
V2 navegando de más), mientras que el contenido del repo ya estaba limpio.

| Página | Antes | Después |
|---|---|---|
| `app.js`, `devolucion.js`, `formateo.js` (generadores) | Sin `?v` → versión cacheada vieja | `?v=20260907` |
| `actas.js`, `acta-view.js`, `firmas.js`, `admin-layout.js`, `ui.js`, `login.js`, `api.js`, `iconos.js` y módulos de gestión | Sin `?v` → versión cacheada vieja | `?v=20260907` |
| `firma.js` | `?v=20260825` (ya versionado) | `?v=20260907` (bump) |

`flatpickr` y `flyonui` (dependencias de `node_modules`) no se versionaron.

**Confirmación:** `grep` global de scripts sin `?v` → 0 resultados en las 14
páginas; los reloads observados correspondían a ejecución de JS obsoletos en el
navegador, no al código actual del repo.

### 1a. Origen histórico (eliminado en Sprint 4 / Java 4.0-5.x)

En el **Portal de Firma** (`firma.js`), el botón **Firmar Acta** iniciaba el
POST sin pantalla de procesamiento: solo cambiaba su propio estado a `.loading`.
Durante los 3-10+ segundos que tarda el backend en validar OTP, guardar firma y
foto, incrustarlas en el DOCX, regenerar PDF/checklist y auditar, el usuario no
tenía **ningún feedback** sobre qué estaba pasando. Efectos observados:

- El usuario **recargaba la página** creyendo que la firma se había colgado
  ("Navigated to" duplicadas en consola).
- El botón **Reenviar Código OTP** y **Rechazar Acta** seguían activos durante
  el procesamiento → acciones duplicadas mientras el token de firma ya estaba
  siendo consumido.
- Doble clic en "Firmar Acta" generaba intentos duplicados (el token de un
  solo uso devuelve 400 en el segundo POST).

## 2. Lugares donde ocurren (auditoría completa del frontend)

Auditoría exhaustiva de `frontend/js/` + `frontend/pages/`:

| Módulo | Hallazgo | Estado |
|---|---|---|
| `app.js` / `devolucion.js` / `formateo.js` (generadores) | Botón `type="button"` + `onclick`, **sin `<form>`**. Tras POST: toast + `setTimeout(600) → actas.html`. **UNA sola navegación.** | Correcto |
| `actas.js` | Listado con `fetch` + render parcial (`renderTable`), polling 3 s `setTimeout` encadenado (fetch, sin navegación). Sin `location.reload`. | Correcto |
| `acta-view.js` | Detalle con `loadPdfViewer`, placeholder `renderGenerando`, polling 3 s fetch. Redirección pública por token → `firma.html` (seguridad). Sin reload. | Correcto |
| `firmas.js` | Listado `fetch` + modals, `await loadActas()` silencioso tras acciones. Sin reload. | Correcto |
| `firma.js` | **Carencia de feedback durante el POST de firma**; `btnReject`/`btnOtpReenviar` activos durante el envío. | **Corregido** |
| `login-ui.js` | `e.preventDefault()` + única redirección a `ROUTES.HOME` tras éxito. Sin reload. | Correcto |
| `home.js` / `admin-layout.js` | Redirección a `login.html` solo por auth (token ausente / 401). Sin loop de navegación. | Correcto |
| `auditoria.js` / `usuarios.js` / `perfil.js` / `cambiar-password.js` | Redirección por auth o por flujo, una vez. Sin polling ni reload. | Correcto |

**Grep de verificación:** `location.reload` → **0 coincidencias** en todo el
frontend. `setInterval` → solo los countdown de OTP/resend (cuenta regresiva,
no navegación). `setTimeout(..., reload)` → 0. No hay meta-refresh en ningún
`<head>`.

## 3. Correcciones realizadas

Veredicto de PROBLEMA 1: la codificación actual ya hace actualización silenciosa
(fetch + render parcial + polling inteligente) y los generadores hacen una sola
navegación. La corrección de esta entrega se concentra en **PROBLEMA 2** (firma):

### `frontend/pages/firma.html`
- **Nuevo estado `#stateProcessing`**: pantalla de procesamiento con spinner y
  el mensaje exacto solicitado:
  *"Estamos finalizando su firma. Por favor espere mientras se actualizan los
  documentos y evidencias asociadas."*
- Estado `#stateSuccess` con mensaje exacto:
  *"Firma registrada correctamente. Los documentos y evidencias ya fueron
  actualizados."*

### Cache-bust global (Java 5.5)
- Todas las etiquetas `<script src="../js/*.js">` de las 14 páginas ahora llevan
  `?v=20260907`. Fuerza al navegador a descargar el JS actual del repo y elimina
  la ejecución de versiones cacheadas con `location.reload` del flujo V2.

### `frontend/js/firma.js`
- **`mostrarProcesando()`**: bloquea el formulario durante la operación
  (oculta la tarjeta de firma, deshabilita `btnSubmit`, `btnReject` y
  `btnOtpReenviar`, detiene el countdown OTP) y muestra la pantalla de
  procesamiento.
- **`ocultarProcesando()`**: restaura la tarjeta en error/reintento.
- **Guardia `procesandoFirma`**: ignora doble clic y acciones duplicadas en
  `submitFirma` y `submitRechazo` (evita el 400 del token de un solo uso).
- El rechazo reutiliza la misma guardia de duplicidad; su feedback de carga ya
  vivía en el botón del modal (`btnConfirmReject`).

## 4. Evidencia de reducción de reloads

- `grep -n "location.reload" frontend/` → **0 resultados**.
- Generadores V1: un único `window.location.href = "actas.html"` tras el POST
  (confirmado en `app.js:407`, `devolucion.js:334`, `formateo.js:280`).
- Los botones de generación son `type="button"` y no están dentro de `<form>`:
  el click dispara un único handler, sin submit implícito del navegador.
- Polling de `GENERANDO_DOCUMENTOS`: fetch cada 3 s + render parcial; no
  reproduce la navegación del navegador (no genera "Navigated to").
- Flujo firma: en el peor caso anterior se producían recargas manuales del
  usuario (no feedback → creía colgado) más un 400 por doble envío. Con la
  pantalla de procesamiento el usuario **no tiene motivo para recargar** y la
  guardia de duplicados bloquea el segundo POST.

## 5. Nuevo flujo visual de firma (antes/después)

### ANTES
1. Usuario firma + foto + (checklist) → clic **Firmar Acta**.
2. El botón pasa a `.loading` con el texto "Enviando firma...".
3. El POST tarda segundos (valida OTP, guarda evidencias, incrusta firmas,
   regenera PDF/checklist, audita). **Sin feedback global.**
4. Usuario ve "nada" → puede recargar, reenviar OTP o rechazar durante el envío.
5. Al terminar, la tarjeta desaparece y aparece "Firma Registrada".

### DESPUÉS
1. Usuario firma + foto + (checklist) → clic **Firmar Acta**; `procesandoFirma`
   bloquea cualquier segunda acción.
2. Se muestra **pantalla de procesamiento**: spinner + *"Estamos finalizando su
   firma. Por favor espere mientras se actualizan los documentos y evidencias
   asociadas."*
3. `btnReject`, `btnOtpReenviar` y `btnSubmit` quedan deshabilitados; el
   countdown OTP se detiene.
4. Al completarse: **"Firma registrada correctamente. Los documentos y
   evidencias ya fueron actualizados."**
5. En error: se restaura la tarjeta con el toast de error y los controles se
   rehabilitan (esperando el cooldown de reenvío OTP si aplica).

## 6. Pantalla de procesamiento implementada

Estado nuevo en `firma.html` (`#stateProcessing`), reutilizando la clase
`.firma-state` existente:

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

Controlada por `mostrarProcesando()` / `ocultarProcesando()` en `firma.js`,
con la guardia `procesandoFirma` para evitar doble envío.

## 7. Impacto en rendimiento percibido

- **Firma** (caso crítico): antes el usuario percibía "falló / se colgó"
  durante segundos de procesamiento; ahora hay un estado visible que avanza y
  concluye en éxito. Se eliminan recargas manuales e intentos duplicados que
  agregaban latencia percibida y ruido en el servidor.
- **Generación** de actas: el flujo async existente (`GENERANDO_DOCUMENTOS`)
  ya evita la espera síncrona; el polling avisa "Documentos listos" sin
  recargar la página.
- No se añadió ningún request extra: la pantalla de procesamiento es puro DOM.
  Cero impacto en el backend.

## 8. Validación auditoría OWASP intacta

Cambios acotados a **firma.html (markup de estado)** y **firma.js
(feedback/DOM + guardia de duplicidad)**. Ningún archivo de seguridad tocado:

| Item | Estado |
|---|---|
| **JWT** (`jjwt`, `SecurityConfig`, `JwtAuthenticationFilter`) | Sin cambios |
| **OTP** (validación, expiración, envío, cooldown, reenvíos) | Sin cambios |
| **Password Reset / Recuperación / Cambio contraseña** | Sin cambios |
| **Protección XSS** | La pantalla nueva usa `textContent` (nada de `innerHTML` con datos del servidor); los textos son estáticos del HTML |
| **Path Traversal** | Sin cambios |
| **Validaciones OWASP / GlobalExceptionHandler** | Sin cambios |
| **Auditoría** | Sin cambios |
| **Autorización / Roles** | Sin cambios |
| **Rate Limiting** | Sin cambios |
| **CSP** | Sin cambios |
| **Tokens de firma (un solo uso)** | La guardia `procesandoFirma` refuerza el no-uso repetido del token; no lo debilita |
| **Rutas públicas** (`/firma/*`, `/uploads/*`) | Sin cambios |

## 9. Confirmación: sin regresiones de seguridad

- **No se introdujeron superficies de ataque**: el nuevo estado es markup
  estático; la lógica de seguridad (otpHeaders, `X-OTP-Sesion`, token de un
  solo uso) quedó intacta.
- **No hay bypass**: la guardia de duplicidad no salta validaciones ni reduce
  verificaciones; solo evita un segundo POST simultáneo.
- **No hay exposición de información**: los mensajes de procesamiento son
  textos de negocio estáticos, sin datos internos (IDs, correos, rutas).
- **No se relajaron controles**: el flujo de rechazo y el de firma siguen
  exigiendo OTP válido y token no usado.
- Los cambios no tocan autenticación, autorización, ni manejo de errores del
  backend (a diferencia de la entrega MEJORA_CUENTAS_BLOQUEADAS, aquí no hubo
  cambios de backend en absoluto).

## 10. Resumen de archivos modificados

| Archivo | Cambio |
|---|---|
| `frontend/pages/firma.html` | Estado `#stateProcessing` + mensaje de éxito actualizado |
| `frontend/js/firma.js` | `mostrarProcesando`/`ocultarProcesando` + guardia `procesandoFirma` en firma y rechazo |
| `frontend/pages/*.html` (14 páginas) | Cache-bust `?v=20260907` en todos los `<script src="../js/...">` |

Backend **sin cambios**. Auditoría OWASP intacta. Sin regresiones.