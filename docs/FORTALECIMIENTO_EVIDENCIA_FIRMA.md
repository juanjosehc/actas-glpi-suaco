# FORTALECIMIENTO DE EVIDENCIA PROBATORIA DEL PROCESO DE FIRMA ELECTRÓNICA

> Estado: implementado (IMPLEMENTAR A, C, D). No modifica el flujo funcional de firma,
> el OTP, la autorización ni los controles de seguridad. Solo suma trazabilidad y
> evidencia objetiva del envío SMTP y del correo objetivo.
>
> Decisión arquitectónica: la evidencia se consolida en `auditoria_sistema` como
> **única fuente de verdad** (columnas tipadas `message_id`, `hash_correo`,
> `estado_envio`), eliminando la tabla redundante `correo_envio` que duplicaba a
> `auditoria_sistema` (ver docs/REVISION_ARQUITECTURA_EVIDENCIA_CORREO.md).

---

## 1. Diseño técnico propuesto

Tres adiciones independientes, sin tocar la experiencia de usuario:

- **A — Evidencia de envío SMTP.** En cada intento de envío del correo de
  firma/OTP, `MailService.enviarCorreoFirma` deja de devolver solo `boolean` y
  pasa a devolver un `ResultadoEnvio` (resultado + `message_id` + fecha real de
  aceptación del SMTP + estado + motivo). Esa evidencia se persiste en
  **columnas tipadas de `auditoria_sistema`** (`message_id`, `estado_envio`)
  junto al `hash_correo`.
- **C — Compromiso criptográfico del correo objetivo.** Columna
  `firma_otp.hash_correo` con SHA-256 (hex, 64 chars) del correo exacto usado
  para emitir cada OTP.
- **D — Auditoría técnica ampliada.** Los eventos `OTP_ENVIADO`,
  `OTP_ENVIO_FALLIDO` y `OTP_VALIDADO` incorporan `hash_correo` (y los de envío,
  `message_id` + `estado_envio`) en columnas tipadas, no solo en el detalle. La
  interfaz conserva el enmascarado (`j***@empresa.com`).

No se implementa IP / User-Agent / fingerprint del dispositivo (excluido
explícitamente en el alcance; ver §9).

## 2. Columnas involucradas

| Tabla / Entidad | Cambio | Tipo | Null |
|---|---|---|---|
| `auditoria_sistema` / `AuditoriaSistema` | `message_id` | `varchar(255)` | null |
| `auditoria_sistema` / `AuditoriaSistema` | `hash_correo` | `varchar(64)` | null |
| `auditoria_sistema` / `AuditoriaSistema` | `estado_envio` | `varchar(30)` | null |
| `firma_otp` / `FirmaOtp` | `hash_correo` | `varchar(64)` | NOT NULL |

`estado_envio` toma valores `ENVIADO | FALLIDO | NO_CONFIGURADO |
DESTINATARIO_VACIO` (solo en los eventos de envío; null en el resto).

*Nota histórica:* la primera iteración usó una tabla especializada `correo_envio`.
Tras la revisión arquitectónica se descartó por duplicar `auditoria_sistema`; la
evidencia ya registrada se migró a `auditoria_sistema` y la tabla se eliminó.
El script `migracion_evidencia_correo_firma.sql` todavía ejecuta esa migración
idempotente en entornos ya usados con `JPA_DDL_AUTO=validate`.

## 3. Justificación del almacenamiento elegido

**Columnas tipadas en `auditoria_sistema` en lugar de una tabla `correo_envio`.**
Razones:

- **Unicidad de fuente de verdad.** `correo_envio` reescribía los mismos hechos
  que ya registra la auditoría (qué correo, a quién, con qué resultado). Tener
  dos tablas para el mismo evento invita a divergencias y complica la traza
  probatoria en lugar de aclararla.
- **Cardinalidad cubierta por la auditoría.** La auditoría ya registra **un
  evento por envío** (`OTP_ENVIADO` / `OTP_ENVIO_FALLIDO`), así que cada envío
  (emisión inicial + reenvíos) tiene su propia fila con su `message_id` y su
  `fecha_evento`; no hay pérdida de la traza histórica de reenvíos.
- **Menos superficie y menos riesgo.** Una tabla menos, una entidad menos, un
  repositorio menos. Menos objetos que mantener y menos filas que el
  `ddl-auto/validate` tenga que validar en producción.
- **Todos los eventos OTP ya se persiguen en `auditoria_sistema`.** La evidencia
  del transporte encaja naturalmente ahí; `firma_otp` sigue siendo la entidad
  del **código** (un solo OTP por ciclo) y `hash_correo` vive en ella porque es
  atributo intrínseco del código emitido (compromete a qué correo fue ese OTP),
  quedando disponible en el mismo registro que se consulta en `validar`.

## 4. Cambios en MailService y flujo OTP

`MailService`:

- `enviarCorreoFirma(...7 args)` devuelve ahora `ResultadoEnvio` en lugar de
  `boolean`. Tras `javaMailSender.send(message)` se lee `message.getMessageID()`
  (identificador del mensaje aceptado por el transporte/Javamail) y se registra
  `LocalDateTime.now()` como fecha real de envío.
- Sigue siendo tolerante: si SMTP no está configurado o el envío falla, devuelve
  `ResultadoEnvio.fallo(...)` sin interrumpir el flujo (mismo comportamiento que
  el antiguo `return false`, solo que ahora con estado y motivo).
- Se mantiene el overload de 5 args (compat) delegando y reduciendo a
  `.esEnviado()`.

`OtpService.generarYEnviarParaToken`:

1. Resuelve el correo una sola vez (`correoDelToken`) y lo usa **tanto** para
   el envío **como** para calcular `hash_correo` (garantiza que la huella
   corresponde exactamente al valor enviado — requisito de IMPLEMENTAR C).
2. Persiste el `FirmaOtp` con `hashCorreo = EvidenceHash.sha256(correo)`.
3. Envía el correo obteniendo `ResultadoEnvio`, y registra el evento
   `OTP_ENVIADO`/`OTP_ENVIO_FALLIDO` vía
   `auditoriaService.registrarConEvidencia(...)` pasando `resultado.messageId()`,
   `hashCorreo` y `resultado.estado()` como columnas tipadas (siempre: éxito y
   fallo, dejando constancia del intento y su resultado).

`OtpService.enviarCorreoOtp` recibe y reenvía el correo ya resuelto (misma
fuente que el hash). En `validar` el `OTP_VALIDADO` se registra con
`hash_correo` tipado; el `message_id` del envío ya quedó en su propio evento
`OTP_ENVIADO`/`OTP_ENVIO_FALLIDO`.

Nuevos/ajustados:
- `mail/dto/ResultadoEnvio.java` (record de resultado de envío).
- `auditoria/service/AuditoriaService.registrarConEvidencia(...)` (variante con
  columnas tipadas de evidencia).
- `auditoria/entity/AuditoriaSistema` (columnas `message_id`, `hash_correo`,
  `estado_envio`).
- `firma/support/EvidenceHash.java` (SHA-256 hex, sin PII).
- Eliminados: `firma/entity/CorreoEnvio.java`, `firma/repository/CorreoEnvioRepository.java`,
  tabla `correo_envio`.

## 5. Evidencia SMTP adicional obtenida

De cada envío se persiste ahora en `auditoria_sistema`:

- `message_id` — identificador del mensaje (lo asigna el transporte SMTP o
  JavaMail al componer; se lee tras el `send`).
- `fecha_evento` — fecha real en que el SMTP aceptó el mensaje (timestamp de la
  fila de auditoría).
- `estado_envio` — ENVIADO / FALLIDO / NO_CONFIGURADO / DESTINATARIO_VACIO.
- `hash_correo` — compromiso del destinatario.
- `entidad_id` — el `FirmaToken` enmascarado (SEC-105) que correlaciona todos
  los envíos/validaciones del mismo proceso de firma.

Nota de limitación honesta: si el proveedor SMTP no expone su propio
Message-ID en la respuesta (JavaMail genera uno local), el valor persiste de
todos modos como identificador del mensaje del lado emisor. No se implementa un
bounce/DRS en segundo plano; el `estado_envio` refleja la aceptación del SMTP
para envío, no una confirmación de lectura (fuera de alcance y sin cambios al
flujo).

## 6. Implementación de hash_correo

- `EvidenceHash.sha256(String)` → SHA-256 en hex minúscula (64 chars). Es
  unidireccional y no se considera dato personal.
- Se calcula sobre el **mismo valor exacto** usado para enviar el OTP
  (`correoDelToken(firmaToken)`), en el momento de emisión.
- Se persiste en `firma_otp.hash_correo` (columna NOT NULL) y se replica en
  `auditoria_sistema.hash_correo` en cada evento OTP.
- Backfill del histórico: la migración SQL computa el hash sobre
  `correo_destino` ya persistido para los OTP previos, dejando la serie
  histórica cubierta por la misma huella.

## 7. Cambios realizados en auditoría

- `OTP_ENVIADO` / `OTP_ENVIO_FALLIDO` → columnas tipadas `message_id`,
  `hash_correo`, `estado_envio` + detalle legible
  (`Correo OTP enviado a j***@dominio | hash_correo=...`).
- `OTP_VALIDADO` → columna `hash_correo` + detalle
  (`Codigo OTP validado, sesion ... | hash_correo=...`).

El correo en los logs/auditoría **sigue enmascarado** (`j***@dominio`,
`enmascararCorreo`); el hash no expone la dirección. El valor completo vive solo
en almacenamiento interno (`firma_otp.correo_destino`), no en pantallas ni
reportes funcionales.

## 8. Validación de compatibilidad con OWASP

- **No se toca la autenticación ni la autorización.** El OTP sigue de un solo
  uso (`validarSesionAtomico`), con límite de intentos, expiración, cooldown y
  tope de reenvíos. El flujo de firma exige sesión OTP (`X-OTP-Sesion`) en todos
  los endpoints.
- **Sin PII nueva expuesta.** `hash_correo` es un digest unidireccional; no
  amplía la superficie de datos personales ni se muestra en interfaces.
- **Sigilo de errores conservado.** `validar` sigue devolviendo errores
  genéricos ("Codigo incorrecto o no valido") sin filtrar el motivo; los hash y
  message-id van solo al detalle de auditoría interna, no al cliente.
- **Rate limiting intacto** (`firma-otp` por IP). La persistencia de evidencia
  no agrega endpoints públicos ni rutas nuevas.
- **Enmascarado SEC-105** del token y del correo en logs se mantiene; el
  `message_id` íntegro vive en columnas de auditoría, no en pantallas.
- No se introducen secrets nuevos: MAIL_* ya parametrizado en YAML.

## 9. Validación de compatibilidad con privacidad de datos

- La interfaz y los reportes funcionales siguen mostrando el correo enmascarado
  (`j***@empresa.com`). No se expone la dirección completa en pantallas.
- El correo completo ya se persiste en `firma_otp.correo_destino` (comportamiento
  previo); la auditoría conserva solo el `hash_correo` para rastrear el objetivo
  sin almacenar PII adicional en la traza.
- `hash_correo` (SHA-256) no es reversible: no constituye dato personal y puede
  registrarse en auditoría sin riesgo.
- No se incorpora IP, User-Agent ni fingerprint del dispositivo (excluido del
  alcance). Si más adelante el área legal lo exige para no-repudio fuerte, se
  evaluará como cambio aparte con su propia evaluación de privacidad.

## 10. Valor probatorio añadido frente al diseño actual

| Capacidad de demostrar hoy | Tras esta implementación |
|---|---|
| El OTP fue generado | igual, + `hash_correo` del objetivo |
| El OTP fue enviado a una dirección | + **evidencia SMTP**: `message_id`, `fecha_evento_real`, `estado_envio` en `auditoria_sistema` |
| El OTP fue validado | + auditoría `OTP_VALIDADO` con `hash_correo` (tipado) |
| A qué correo exacto fue enviado | + compromiso criptográfico inmutable (`hash_correo`), aun si se editara `acta.correo_usuario` luego |
| Cuándo se envió | + `fecha_evento` del evento de envío |
| Qué identificador entregó el SMTP | + `auditoria_sistema.message_id` |
| A qué proceso de firma pertenece | + `entidad_id` = `FirmaToken` enmascarado correlaciona envíos y validaciones |

La cadena probatoria queda: **OTP (hash_correo + código BCrypt) → envío SMTP
(message_id + fecha + estado) → validación (sesión) → firma desde sesión
autorizada**, todo consolidado en `auditoria_sistema` como única fuente de
verdad, sin exponer datos personales en interfaz, ni modificar el flujo
funcional ni la seguridad existente.
