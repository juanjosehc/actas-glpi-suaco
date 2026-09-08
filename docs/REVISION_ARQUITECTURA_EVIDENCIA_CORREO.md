# Revisión arquitectónica: tabla correo_envio → consolidación en auditoria_sistema

> Decisión (2026-09-08): **revertir la tabla `correo_envio`** y consolidar toda la
> evidencia del envío de correo en `auditoria_sistema` como única fuente de verdad.
> Ver `docs/FORTALECIMIENTO_EVIDENCIA_FIRMA.md`.

## Contexto

La primera implementación de FORTALECIMIENTO_EVIDENCIA_FIRMA creó una tabla
especializada `correo_envio` (entidad `CorreoEnvio`) para registrar la evidencia
objetiva de cada envío SMTP del correo de firma/OTP (message_id, fecha real,
estado) además del `hash_correo` en `firma_otp`.

Esa tabla duplicaba el papel de `auditoria_sistema`, que ya registra un evento
por envío (`OTP_ENVIADO` / `OTP_ENVIO_FALLIDO`).

## Preguntas de la revisión

| # | Pregunta | Respuesta |
|---|---|---|
| 1 | ¿Qué aporta `correo_envio` que no tenga `auditoria_sistema`? | Nada esencial. `auditoria_sistema` ya guarda fecha del evento, correo (enmascarado), y correlación por token. Solo faltaban columnas tipadas para `message_id` y `estado_envio`. |
| 2 | ¿Qué se perdería al consolidar? | La relación 1:N fila-específica del transporte. Pero la auditoría ya tiene **una fila por envío**, así que la traza de reenvíos se conserva sin tablas extra. |
| 3 | ¿Hay justificación sólida para mantener la tabla separada? | No: un mismo hecho (un envío) en dos tablas = riesgo de divergencia, más entidades/repositorios que mantener, y `firma_otp` sigue siendo la entidad del código (un OTP por ciclo), no del transporte. |
| 4 | Recomendación final | **Revertir `correo_envio`.** Consolidar en `auditoria_sistema` con columnas tipadas `message_id`, `hash_correo`, `estado_envio`. Migrar la evidencia ya registrada antes de eliminar la tabla. |

## Decisión tomada

1. Se agregaron columnas tipadas a `auditoria_sistema` / `AuditoriaSistema`:
   `message_id varchar(255)`, `hash_correo varchar(64)`, `estado_envio varchar(30)`.
2. Nuevo overload `AuditoriaService.registrarConEvidencia(...)` para persistirlas.
3. `OtpService` registra cada envío (`OTP_ENVIADO`/`OTP_ENVIO_FALLIDO`) y la
   validación (`OTP_VALIDADO`) vía ese overload.
4. Se eliminaron `CorreoEnvio` y `CorreoEnvioRepository`.
5. La migración SQL (`migracion_evidencia_correo_firma.sql`) agrega las columnas,
   **migra la evidencia histórica de `correo_envio` → `auditoria_sistema`** y
   dropea la tabla. Idempotente, para `JPA_DDL_AUTO=validate` (SEC-109).
6. `firma_otp.hash_correo` (NOT NULL) se conserva: es atributo del código OTP.

## Resultado

Una sola fuente de verdad (`auditoria_sistema`) para la cadena probatoria
**OTP → envío SMTP (message_id + fecha + estado + hash_correo) → validación**,
sin tabla redundante y sin exponer PII en interfaz.
