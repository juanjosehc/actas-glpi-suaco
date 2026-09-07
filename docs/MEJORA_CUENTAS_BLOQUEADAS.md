# Mejora de Gestión de Cuentas Bloqueadas en SAUCO

Sistema: **SAUCO — Gestión de Actas de Entrega/Devolución (Coltefinanciera)**
Fecha: **2026-09-07**

## 1. Causa exacta del problema original

El bloqueo de cuenta existía (`Usuario.bloqueado`) y se reflejaba en
`UserSecurity.isAccountNonLocked()`, pero el flujo estaba roto en **dos** puntos:

- **Escenario 1 (login)**: `CustomUserDetailsService.loadUserByUsername` lanzaba
  `LockedException` cuando `usuario.getBloqueado()` era `true`. Spring
  `DaoAuthenticationProvider.retrieveUser` envuelve cualquier excepción no
  reconocida de `loadUserByUsername` (incluida `LockedException`) en
  `InternalAuthenticationServiceException`. Como ese wrapper NO extiende
  `AccountStatusException`, el `@ExceptionHandler(AccountStatusException)`
  del `GlobalExceptionHandler` no lo capturaba y el flujo caía en el handler
  genérico → **500 "Error interno del servidor"**.

  Evidencia original (reproducida):
  ```
  POST /auth/login  bloctest1/Clave123!  (bloqueado, password correcta)
  -> {"success":false,"mensaje":"Error interno del servidor"}   HTTP 500
  ```

- **Escenario 2 (sesión activa)**: `JwtAuthenticationFilter` recarga el usuario
  de la BD en cada request con `loadUserByUsername`. Al lanzar `LockedException`
  para una cuenta recién bloqueada, el `catch (Exception ignored)` del filtro la
  tragaba: no se creaba `SecurityContext` y el endpoint protegido producía un
  **403 genérico de Spring Security**, sin mensaje claro ni código accionable.

  Evidencia original:
  ```
  GET /actas  Authorization: Bearer <JWT emitido antes del bloqueo>
  -> {"status":403,"error":"Forbidden"}   HTTP 403
  ```

## 2. Flujo actual vs corregido

### Escenario 1 — Login de cuenta bloqueada

| | Flujo original | Flujo corregido |
|---|---|---|
| `loadUserByUsername` | Lanza `LockedException` | Devuelve `UserSecurity` con `isAccountNonLocked()=false` |
| `DaoAuthenticationProvider` | Envuelve en `InternalAuthenticationServiceException` (raro) | Pre-check `preAuthenticationChecks` lanza `LockedException` desenvuelta |
| Handler | 500 genérico | 401 `CUENTA_BLOQUEADA` con mensaje claro |
| Frontend | "Error interno del servidor" | Muestra el mensaje de la respuesta 401 |

### Escenario 2 — Bloqueo durante sesión activa

| | Flujo original | Flujo corregido |
|---|---|---|
| `JwtAuthenticationFilter` | `catch (Exception ignored)` traga `LockedException` | Verifica `isAccountNonLocked()`; si bloqueado corta |
| Respuesta | 403 `Forbidden` genérico | 401 `CUENTA_BLOQUEADA` con mensaje claro |
| Auditoría | Sin evento | `ACCESO_DENEGADO` registrado |
| Frontend | Sin manejo (403 no se interpreta) | Interceptor global: toast + limpieza sesión + redirect login |

## 3. Códigos HTTP usados

| Caso | Código | Cuerpo (`success/mensaje/codigo`) |
|---|---|---|
| Login cuenta bloqueada | **401** | `false / "Su cuenta ha sido bloqueada. Comuníquese con un administrador." / "CUENTA_BLOQUEADA"` |
| Request con JWT de cuenta bloqueada (sesión) | **401** | `false / "Su cuenta ha sido bloqueada. Comuníquese con un administrador." / "CUENTA_BLOQUEADA"` |
| Login credenciales inválidas (usuario inexistente / password errónea) | **401** | `false / "Credenciales invalidas"` (sin `codigo`) |
| Endpoint protegido sin JWT | **403** | responde Spring Security (sin tipificación de bloqueo) |
| Recuperación de cuenta bloqueada | **200** | `true` genérico (anti-enumeración, no revela bloqueo/estado) |

El `codigo` es opcional (`@JsonInclude(NON_NULL)`): solo aparece en respuestas
"bloqueada", sin alterar el contrato existente de los demás errores.

## 4. Manejo de errores en backend

- `CustomUserDetailsService`: dejó de lanzar `LockedException` en
  `loadUserByUsername`. El estado se refleja en `UserSecurity`
  (`isAccountNonLocked()` / `isEnabled()`).
- `GlobalExceptionHandler.handleAccountStatus`: usa `ErrorResponse.ofConCodigo`
  con `codigo = "CUENTA_BLOQUEADA"` para `LockedException` y el mensaje exacto
  de negocio.
- `JwtAuthenticationFilter`: si el usuario del JWT tiene `isAccountNonLocked()=false`,
  responde 401 con `codigo=CUENTA_BLOQUEADA` sin crear `SecurityContext`, y
  registra `ACCESO_DENEGADO` en la CAPA 2 de auditoría.
- `ErrorResponse`: nuevo campo `codigo` (NON_NULL) y factory `ofConCodigo`.

## 5. Manejo de mensajes en frontend

- **Login**: `login-ui.js` ya muestra `data.mensaje` de la respuesta; al ser 401
  con mensaje claro, el usuario ve "Su cuenta ha sido bloqueada. Comuníquese con
  un administrador." (login.html no carga `ui.js`; no hay doble manejo).
- **Sesión activa**: `ui.js` instala un interceptor global de `fetch` (todos los
  módulos autenticados lo cargan). Cuando una respuesta JSON trae
  `codigo === "CUENTA_BLOQUEADA"`:
  1. Muestra el toast profesional de 5 s:
     "Su cuenta ha sido bloqueada por un administrador. Debe volver a iniciar
     sesión y comunicarse con soporte si considera que esto es un error."
  2. Limpia por completo la sesión local: `sessionStorage` (JWT) y `localStorage`
     (`username`, `role`, `token`).
  3. Redirige automáticamente a `login.html` (2.5 s, para que el toast sea visible).
  - La respuesta original se devuelve intacta (se lee un `clone`), sin romper a
    los consumidores de `fetch`.
  - Se ejecuta una sola vez (`bloqueoEjecutado`) para evitar cascadas de toasts.

## 6. Cambios en autenticación / autorización

- El JWT de una cuenta bloqueada **nunca** crea `SecurityContext` en el filtro:
  no hay autorización ni acceso a datos con un token revocado-por-bloqueo.
- El `pwv` (SEC-103) y la denylist (SEC-011/118) quedan intactos: no se tocaron.
- El bloqueo se aplica por igual en login y en cada request JWT, sin debilitar la
  autenticación ni abrir vías alternativas.
- NO se añadió ningún endpoint nuevo de desbloqueo público; el desbloqueo sigue
  siendo decisión del administrador.

## 7. Evidencia funcional — login de cuenta bloqueada

```
POST /auth/login  bloctest1/Clave123!  (bloqueado, password correcta)
{"success":false,"mensaje":"Su cuenta ha sido bloqueada. Comuníquese con un administrador.","codigo":"CUENTA_BLOQUEADA"}
HTTP 401

POST /auth/login  bloctest1/WrongPass!1  (bloqueado, password incorrecta)
{"success":false,"mensaje":"Su cuenta ha sido bloqueada. Comuníquese con un administrador.","codigo":"CUENTA_BLOQUEADA"}
HTTP 401
```
Ambas contraseñas (correcta e incorrecta) devuelven el MISMO 401 de bloqueo: la
pre-check supera la verificación de password, no hay distinción que enuncie al
atacante (sin enumeración).

## 8. Evidencia funcional — bloqueo durante la sesión

```
1) bloctest1 desbloqueado, login OK -> JWT válido.
2) GET /actas con ese JWT                     -> HTTP 200 (control sano)
3) UPDATE usuario SET bloqueado=true ...      (bloqueo)
4) GET /actas con el MISMO JWT (aún vigente)  ->
   {"success":false,"mensaje":"Su cuenta ha sido bloqueada. Comuníquese con un administrador.","codigo":"CUENTA_BLOQUEADA"}
   HTTP 401
```
Auditoría CAPA 2 confirma el rechazo:
```
ACCESO_DENEGADO | bloctest1 | /actas | 11:53:04
```

## 9. Compatibilidad OWASP (validación explícita)

| Garantía | Estado |
|---|---|
| **Autenticación JWT** | Intacta. Solo se añadió el corte por bloqueo; `pwv`/denylist sin cambios. |
| **Anti-User-Enumeration** | Intacta. Usuario inexistente y password errónea → mismo 401 "Credenciales invalidas". Cuenta bloqueada → 401 "bloqueada" (mensaje de estado, no de existencia). No se distingue "existe pero bloqueado" de "existe y no bloqueado" con clave errónea: ambos dependen del estado, no del listado de cuentas. |
| **Autorización por roles** | Intacta. `@PreAuthorize`, `@RequireRol` y matchers sin cambios. |
| **Auditoría existente** | Conservada y ampliada: el rechazo por bloqueo en sesión registra `ACCESO_DENEGADO`. |
| **Gestión de sesiones** | Intacta. El logout único `/sesiones/revocar` no se tocó. |
| **Seguridad de recuperación de contraseña** | Intacta. `solicitarRecuperacion` para cuenta bloqueada no envía enlace y responde 200 genérico (anti-enumeración ya validada). |
| **Controles de acceso actuales** | Sin cambios en `SecurityConfig`. Rutas públicas (`/firma/`, `/uploads/`, `/auth/*`) siguen públicas; el filtro solo corta peticiones con JWT de cuenta bloqueada. |

**Sin regresiones introducidas.** Los datos de prueba (`bloctest1`, `blocfree`)
se eliminaron de la BD; la cuenta `admin` quedó intacta.

## 10. Confirmación: sin User Enumeration ni anomalías

- Usuario inexistente (`noexiste99`) y password incorrecta (`admin/Nope!1234`)
  devuelven ambos `401 "Credenciales invalidas"` sin `codigo`.
- La cuenta bloqueada devuelve 401 "bloqueada" con `codigo`; este estado es
  legítimo del negocio y no revela existencia de otras cuentas.
- La recuperación de una cuenta bloqueada devuelve `200` genérico idéntico al de
  un correo no registrado: no permite distinguir estados.
- Ninguna respuesta expone datos internos (IDs, correos, rutas, stacks).
- Auditoría sin anomalías: `LOGIN_FALLIDO` para credenciales erróneas, `LOGIN_EXITOSO`
  para aciertos, `ACCESO_DENEGADO` para rechazo por bloqueo en sesión.
