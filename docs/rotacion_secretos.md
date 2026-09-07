# Rotación de Secretos — Sauco pre-producción

**Contexto (SEC-002 / SEC-101):** la auditoría OWASP pre-producción
(`docs/auditoria_owasp_preproduccion_2026-09-07.md`) halló que secretos históricos
quedaron en el historial de git antes de SEC-002 (password DB, JWT secret, tokens
GLPI). Además, el path traversal leído (SEC-101, ya corregido) permitía leer
`backend/.env` vía `rutaPdf` manipulada. Por ambos caminos se considera **todos los
secretos como potencialmente comprometidos** y deben rotarse antes de producción.

Regla de oro: **si alguna vez fue commitado, estando o no en el último archivo, se rota.**

---

## 1. Inventario de secretos

| Secreto | Variable | Dónde se usa | Historial git |
|---|---|---|---|
| Password PostgreSQL | `DB_PASSWORD` | `application.yml` → `spring.datasource.password` | ⚠️ comprometida |
| Clave JWT (HS256, Base64 ≥ 256 bits) | `JWT_SECRET` | `JwtService` (`Keys.hmacShaKeyFor`) | ⚠️ comprometida |
| App-Token GLPI | `GLPI_APP_TOKEN` | `EquipoService` header `App-Token` | ⚠️ comprometida |
| User-Token GLPI | `GLPI_USER_TOKEN` | `EquipoService` header `User-Token` | ⚠️ comprometida |
| Password SMTP | `MAIL_PASSWORD` | Spring Mail (`mail.password`) | revisar histórico |

Origen de la exposición: `git log --all --oneline -- backend/.env` y búsqueda en
historial de `DB_PASSWORD=`, `JWT_SECRET=`, `GLPI_APP_TOKEN=`.

---

## 2. Rotación de `DB_PASSWORD`

```sql
ALTER USER postgres WITH PASSWORD 'NUEVA_clave_fuerte_2026';
```

1. Generar nueva clave (≥ 24 chars, mayúscula/minúscula/número/especial).
2. Ejecutar el `ALTER USER` (o la política del DBA del entorno).
3. Actualizar `backend/.env`: `DB_PASSWORD=NUEVA_clave_fuerte_2026`.
4. Reiniciar backend y verificar `GET /health` (o login) contra la nueva credencial.
5. Aplicar también en cualquier pila de orquestación (Docker Compose, secrets manager).

> No hay migración de datos: la password del rol no se almacena en la app; solo la usa el datasource.

---

## 3. Rotación de `JWT_SECRET`

```bash
# Linux/macOS
openssl rand -base64 48
# PowerShell (Windows)
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

1. Generar Base64 de **al menos 48 bytes** (384 bits; mínimo requerido 256 bits / 44 chars Base64).
2. Actualizar `backend/.env`: `JWT_SECRET=<nuevo_base64>`.
3. Reiniciar backend.

**Efecto colateral obligatorio:** todos los JWT emitidos con la clave vieja quedan
inválidos. Todos los usuarios deben volver a autenticarse (login). No hay token de
firma afectado: `FirmaToken` es UUID propio, independiente del JWT.

---

## 4. Rotación de tokens GLPI (`GLPI_APP_TOKEN`, `GLPI_USER_TOKEN`)

1. En GLPI → Administración → General → API: revocar App-Token actual (o regenerarlo).
2. En GLPI → Usuarios → (usuario API) → token: regenerar User-Token.
3. Actualizar `backend/.env`:
   ```
   GLPI_APP_TOKEN=<nuevo>
   GLPI_USER_TOKEN=<nuevo>
   ```
4. Reiniciar backend y verificar una consulta de equipo:
   `GET http://127.0.0.1:8001/equipo/{serial}` devuelve datos (200/404 con equipo,
   no 401 de GLPI).

---

## 5. Rotación de `MAIL_PASSWORD`

1. Regenerar clave de aplicación / password SMTP en el proveedor (ej. Brevo, M365).
2. Actualizar `backend/.env`: `MAIL_PASSWORD=<nueva>`.
3. Reiniciar y enviar una acta de prueba (flujo `/actas/{id}/enviar`) para confirmar entrega.

---

## 6. Limpieza del historial git (opcional pero recomendado)

El secreto rotado ya es inútil; sin embargo, `git history` conserva el valor y puede
reutilizarse mal. Si el repo es interno y monorepo cerrado, la rotación basta. Si hay
riesgo de filtración del repo, considerar reescritura de historial (BFG Repo-Cleaner)
y rotación adicional tras la reescritura.

```bash
# Requiere fuerza; coordinar con todo el equipo (rewrite de todos los clones).
java -jar bfg.jar --replace-text passwords.txt
git reflog expire --expire=now --all && git gc --prune=now --aggressive
git push --force --all && git push --force --tags
```

> Poner `backend/.env` (fuera de git) y verificar `.gitignore` lo ignore antes de cualquier commit futuro.

---

## 7. Checklist post-rotación

- [ ] `DB_PASSWORD` nueva y backend arranca (no fail-fast).
- [ ] `JWT_SECRET` nueva ≥ 256 bits; usuarios re-loguean.
- [ ] Tokens GLPI renovados; `GET /equipo/{serial}` funciona.
- [ ] `MAIL_PASSWORD` renovada; correo de firma llega.
- [ ] Historial git sin `backend/.env` ni secretos en commits viejos (o decidido el rewrite).
- [ ] `.env` en `.gitignore` (`backend/.env` y `${user.dir}/.env`).

---

## Notas de operación

- Todos los secretos van por **variables de entorno** o `backend/.env` (gitignoreado).
  `application.yml` no tiene default para `DB_PASSWORD`/`JWT_SECRET` (fail-fast).
- Ejecutar la rotación en **ventana de mantenimiento corta**: solo revoca sesiones JWT
  y deja la app unos minutos sin su credencial GLPI/SMTP mientras se actualiza `.env`.
- Puede hacerse progresivo: primero JWT (bloquea a todos), después DB/GLPI/MAIL en la
  misma ventana.