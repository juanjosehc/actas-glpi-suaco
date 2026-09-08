package com.empresa.actas.firma.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.firma.dto.FirmaOtpEstadoResponse;
import com.empresa.actas.firma.dto.FirmaOtpValidarResponse;
import com.empresa.actas.firma.entity.FirmaOtp;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.FirmaOtpRepository;
import com.empresa.actas.firma.support.EvidenceHash;
import com.empresa.actas.firma.support.FirmaUrlBuilder;
import com.empresa.actas.mail.dto.ResultadoEnvio;
import com.empresa.actas.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Segunda capa de seguridad del portal de firma: OTP de 6 digitos, un solo uso,
 * hasheado con BCrypt (jamas se persiste el codigo en claro), asociado al token
 * de firma y al destinatario.
 *
 * Auditoria en AUDITORIA_SISTEMA (CAPA 2): OTP_GENERADO, OTP_ENVIADO,
 * OTP_ENVIO_FALLIDO, OTP_VALIDADO, OTP_INVALIDO, OTP_BLOQUEADO, OTP_EXPIRADO,
 * OTP_REENVIADO. Nada de OTP en acta_historial (documental).
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final TokenFirmaValidador validadorToken;
    private final FirmaOtpRepository otpRepository;
    private final ActaRepository actaRepository;
    private final MailService mailService;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;
    private final FirmaUrlBuilder firmaUrlBuilder;
    private final OtpIntentoService intentoService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.firma-otp-expira-minutos:10}")
    private int expiraMinutos;

    @Value("${app.firma-otp-sesion-minutos:30}")
    private int sesionMinutos;

    @Value("${app.firma-otp-max-intentos:5}")
    private int maxIntentos;

    @Value("${app.firma-otp-max-reenvios:3}")
    private int maxReenvios;

    @Value("${app.firma-otp-cooldown-segundos:60}")
    private long cooldownSegundos;

    /**
     * Genera y envia el OTP para un token de firma recien creado (o un reenvio).
     * Correo unico: enlace + codigo + vigencia.
     *
     * @return {@code true} si el correo salio; registra OTP_ENVIADO/OTP_ENVIO_FALLIDO según el caso.
     */
    @Transactional
    public boolean generarYEnviarParaToken(FirmaToken firmaToken) {
        otpRepository.invalidarNoValidadas(firmaToken.getIdToken());

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        String correo = correoDelToken(firmaToken);

        // FORTALECIMIENTO_EVIDENCIA_FIRMA (IMPLEMENTAR C): huella criptografica
        // inmutable del correo exacto usado para emitir este OTP. No expone PII
        // (SHA-256 unidireccional) y sobrevive a ediciones posteriores de
        // acta.correo_usuario: compromete a cual direccion fue este codigo.
        FirmaOtp fila = FirmaOtp.builder()
                .idTokenFirma(firmaToken.getIdToken())
                .codigoHash(passwordEncoder.encode(codigo))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expiraMinutos))
                .correoDestino(correo)
                .hashCorreo(EvidenceHash.sha256(correo))
                .usado(false)
                .intentos(0)
                .build();
        otpRepository.save(fila);

        // SEC-105: el FirmaToken en auditoria va enmascarado (es una capability
        // de un solo uso); el id numerico del token de BD sirve para correlacionar.
        String tokenMask = enmascararToken(firmaToken.getToken());

        // FORTALECIMIENTO_EVIDENCIA_FIRMA (IMPLEMENTAR A): se persiste la
        // evidencia del envio SMTP en columnas tipadas de auditoria_sistema
        // (message_id integro, hash_correo, estado) — unica fuente de verdad.
        ResultadoEnvio resultado = enviarCorreoOtp(firmaToken, codigo, correo);

        String hashCorreo = fila.getHashCorreo();
        String estado = resultado.estado();
        auditoriaService.registrarConEvidencia(
                resultado.esEnviado() ? TipoEventoAuditoria.OTP_ENVIADO
                        : TipoEventoAuditoria.OTP_ENVIO_FALLIDO,
                null, "PORTAL_FIRMA", "FIRMA_TOKEN", tokenMask,
                "/firma/otp",
                resultado.esEnviado()
                        ? "Correo OTP enviado a " + enmascararCorreo(correo)
                            + " | hash_correo=" + hashCorreo
                        : "Fallo el envio de correo OTP a " + enmascararCorreo(correo)
                            + " | estado=" + estado
                            + " | hash_correo=" + hashCorreo,
                resultado.messageId(), hashCorreo, estado);
        return resultado.esEnviado();
    }

    private String correoDelToken(FirmaToken firmaToken) {
        return actaRepository.findById(firmaToken.getIdActa())
                .map(Acta::getCorreoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada para el token de firma"));
    }

    private ResultadoEnvio enviarCorreoOtp(FirmaToken firmaToken, String codigo, String correo) {
        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada para el token de firma"));
        return mailService.enviarCorreoFirma(
                correo,
                acta.getNombreUsuario(),
                acta.getTipoActa() != null ? acta.getTipoActa().name() : null,
                acta.getSerialEquipo(),
                firmaUrlBuilder.construir(firmaToken.getToken()),
                codigo,
                expiraMinutos);
    }

    /**
     * Estado del paso OTP. GET SIN efecto lateral (SEC-119): nunca genera ni
     * envia un codigo; para tokens legacy sin fila devuelve "sin codigo" y el
     * frontend dispara POST /otp/reenviar. {@code valido=true} solo si el
     * cliente presenta la sesion correcta (header): si no la tiene, no hay
     * sesion que aprovechar desde otro dispositivo y se pide un codigo nuevo.
     */
    @Transactional
    public FirmaOtpEstadoResponse estado(String token, String sesionSolicitada) {
        FirmaToken firmaToken = validadorToken.validar(token);
        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));
        String correo = acta.getCorreoUsuario();
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("La acta no tiene correo de destinatario asociado");
        }
        String correoEnmascarado = enmascararCorreo(correo);

        Optional<FirmaOtp> ultima = otpRepository
                .findFirstByIdTokenFirmaOrderByFechaCreacionDesc(firmaToken.getIdToken());

        // Sesion ya validada, vigente y probada por el cliente -> el portal carga directo.
        if (ultima.isPresent() && sesionVigente(ultima.get())
                && sesionSolicitada != null && sesionSolicitada.equals(ultima.get().getSesion())) {
            return new FirmaOtpEstadoResponse(true, correoEnmascarado, null, null, null, null, false);
        }

        // Cooldown pendiente desde la ultima emision -> no emitir otra.
        if (ultima.isPresent() && !ultima.get().getUsado() && ultima.get().getFechaValidacion() == null) {
            long cooldown = cooldownRestante(ultima.get());
            if (cooldown > 0) {
                return new FirmaOtpEstadoResponse(false, correoEnmascarado, null,
                        expiraSegundos(ultima.get()), reenviosRestantes(firmaToken.getIdToken()), cooldown, false);
            }
        }

        // Fila vigente sin cooldown -> el mismo codigo sigue activo.
        if (ultima.isPresent() && !ultima.get().getUsado() && ultima.get().getFechaValidacion() == null) {
            boolean vencido = ultima.get().getFechaExpiracion().isBefore(LocalDateTime.now());
            return new FirmaOtpEstadoResponse(false, correoEnmascarado, true,
                    expiraSegundos(ultima.get()), reenviosRestantes(firmaToken.getIdToken()), 0L, vencido);
        }

        // Legacy: token sin fila -> NO se genera ni envia aqui (SEC-119: GET sin
        // efecto lateral; un GET publico no debe disparar correos). Se devuelve
        // el estado "sin codigo" y el frontend llama POST /otp/reenviar cuando
        // el usuario lo solicita (reenviar cubre el caso legacy con fila vacia).
        return new FirmaOtpEstadoResponse(false, correoEnmascarado, null,
                null, reenviosRestantes(firmaToken.getIdToken()), 0L, null);
    }

    /** Valida el codigo OTP. Solo devuelve la sesion; los errores son genericos (no filtrar el motivo). */
    @Transactional
    public FirmaOtpValidarResponse validar(String token, String codigo) {
        FirmaToken firmaToken = validadorToken.validar(token);
        FirmaOtp fila = otpRepository
                .findFirstByIdTokenFirmaOrderByFechaCreacionDesc(firmaToken.getIdToken())
                .orElseThrow(() -> new IllegalArgumentException("Codigo incorrecto o no valido"));

        if (fila.getUsado() || fila.getIntentos() >= maxIntentos) {
            throw new IllegalArgumentException("Codigo incorrecto o no valido");
        }

        if (fila.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            auditoriaService.registrar(TipoEventoAuditoria.OTP_EXPIRADO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", enmascararToken(firmaToken.getToken()),
                    "/firma/otp/validar",
                    "Codigo OTP vencido (" + enmascararCorreo(fila.getCorreoDestino()) + ")");
            throw new IllegalArgumentException("El codigo expiro, solicite uno nuevo");
        }

        if (!passwordEncoder.matches(codigo, fila.getCodigoHash())) {
            // Transaccion propia: el throw posterior (rollback de esta TX) no debe
            // revertir el incremento de intentos; si no, el bloqueo jamas se alcanza.
            intentoService.registrarIncorrecto(fila, firmaToken.getToken(), maxIntentos);
            throw new IllegalArgumentException("Codigo incorrecto o no valido");
        }

        // Marca + sesion en un solo UPDATE atomico: un ganador ante validaciones
        // simultaneas (replay). Nada de save() posterior: un merge pisaria usado=false.
        String sesion = UUID.randomUUID().toString();
        if (otpRepository.validarSesionAtomico(fila.getIdOtp(), LocalDateTime.now(), sesion) == 0) {
            auditoriaService.registrar(TipoEventoAuditoria.OTP_INVALIDO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", enmascararToken(firmaToken.getToken()),
                    "/firma/otp/validar",
                    "Reintento de codigo OTP ya usado (" + enmascararCorreo(fila.getCorreoDestino()) + ")");
            throw new IllegalArgumentException("Codigo incorrecto o no valido");
        }

        // FORTALECIMIENTO_EVIDENCIA_FIRMA (IMPLEMENTAR D): se asocia hash_correo a
        // la validacion, reforzando la trazabilidad de a quien fue emitido el
        // codigo que ahora se valida. Sin message_id aqui: la ultima evidencia de
        // envio ya queda en la fila OTP_ENVIADO/OTP_ENVIO_FALLIDO de este token.
        auditoriaService.registrarConEvidencia(TipoEventoAuditoria.OTP_VALIDADO, null,
                "PORTAL_FIRMA", "FIRMA_TOKEN", enmascararToken(firmaToken.getToken()),
                "/firma/otp/validar",
                "Codigo OTP validado, sesion " + sesion.substring(0, 8) + "... ("
                        + enmascararCorreo(fila.getCorreoDestino()) + ") | hash_correo="
                        + fila.getHashCorreo(),
                null, fila.getHashCorreo(), null);
        return new FirmaOtpValidarResponse(sesion);
    }

    /** Genera y envia un codigo nuevo, invalidando el anterior. */
    @Transactional
    public void reenviar(String token) {
        FirmaToken firmaToken = validadorToken.validar(token);

        // SEC-119: el GET /otp/estado ya no genera codigo (sin efecto lateral);
        // el reenvio es el UNICO camino para emitir. Un token legacy sin fila
        // entra aqui (ultima vacia) y genera el codigo inicial.
        FirmaOtp ultima = otpRepository
                .findFirstByIdTokenFirmaOrderByFechaCreacionDesc(firmaToken.getIdToken())
                .orElse(null);

        if (ultima == null) {
            boolean enviado = generarYEnviarParaToken(firmaToken);
            if (!enviado) {
                throw new IllegalArgumentException("No se pudo enviar el codigo. Verifique el correo e intente de nuevo.");
            }
            auditoriaService.registrar(TipoEventoAuditoria.OTP_REENVIADO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", enmascararToken(firmaToken.getToken()),
                    "/firma/otp/reenviar",
                    "Codigo OTP inicial emitido para token legacy (" + enmascararToken(token) + ")");
            return;
        }

        // Sin guardia de "sesion activa": un usuario sin la sesion en este
        // dispositivo (otra pestana/equipo) debe poder pedir un codigo nuevo;
        // el cooldown y el limite de reenvios acotan el abuso.
        long cooldown = cooldownRestante(ultima);
        if (cooldown > 0) {
            throw new IllegalArgumentException("Debe esperar " + cooldown
                    + " segundos antes de solicitar un nuevo codigo");
        }

        long reenvios = otpRepository.countByIdTokenFirmaAndFechaValidacionIsNull(firmaToken.getIdToken()) - 1;
        if (reenvios >= maxReenvios) {
            throw new IllegalArgumentException("Se alcanzo el limite de reenvios de codigo");
        } 

        generarYEnviarParaToken(firmaToken);
        auditoriaService.registrar(TipoEventoAuditoria.OTP_REENVIADO, null,
                "PORTAL_FIRMA", "FIRMA_TOKEN", enmascararToken(firmaToken.getToken()),
                "/firma/otp/reenviar",
                "Codigo OTP reenviado (" + enmascararCorreo(ultima.getCorreoDestino()) + ")");
    }

    /**
     * Prueba de sesion: valida que exista una sesion emitida para este token
     * tras un OTP correcto y dentro de la vigencia. Devuelve false indistintamente
     * si el token o la sesion fallan (no filtrar cual de los dos).
     */
    public boolean verificarSesion(String token, String sesion) {
        if (sesion == null || sesion.isBlank()) {
            return false;
        }
        FirmaToken firmaToken;
        try {
            firmaToken = validadorToken.validar(token);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return otpRepository.findBySesionAndIdTokenFirma(sesion, firmaToken.getIdToken())
                .map(this::sesionVigente)
                .orElse(false);
    }

    private boolean sesionVigente(FirmaOtp fila) {
        return Boolean.TRUE.equals(fila.getUsado())
                && fila.getFechaValidacion() != null
                && fila.getSesion() != null
                && fila.getFechaValidacion().isAfter(LocalDateTime.now().minusMinutes(sesionMinutos));
    }

    private long cooldownRestante(FirmaOtp fila) {
        long restante = Duration.between(LocalDateTime.now(),
                fila.getFechaCreacion().plusSeconds(cooldownSegundos)).getSeconds();
        return Math.max(restante, 0);
    }

    private Long expiraSegundos(FirmaOtp fila) {
        long seg = Duration.between(LocalDateTime.now(), fila.getFechaExpiracion()).getSeconds();
        return Math.max(seg, 0);
    }

    private int reenviosRestantes(Long idTokenFirma) {
        long usados = otpRepository.countByIdTokenFirmaAndFechaValidacionIsNull(idTokenFirma) - 1;
        return (int) Math.max(maxReenvios - usados, 0);
    }

    /** SEC-105: un FirmaToken completo es una capability; en auditoria/logs solo el prefijo. */
    private String enmascararToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }

    /** Enmascara el correo a ca***@dominio; nunca viaja el correo completo en interfaz. */
    private String enmascararCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return "";
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }
}