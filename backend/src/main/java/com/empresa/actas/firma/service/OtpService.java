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
import com.empresa.actas.firma.support.FirmaUrlBuilder;
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

        FirmaOtp fila = FirmaOtp.builder()
                .idTokenFirma(firmaToken.getIdToken())
                .codigoHash(passwordEncoder.encode(codigo))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expiraMinutos))
                .correoDestino(correoDelToken(firmaToken))
                .usado(false)
                .intentos(0)
                .build();
        otpRepository.save(fila);

        auditoriaService.registrar(TipoEventoAuditoria.OTP_GENERADO, null,
                "PORTAL_FIRMA", "FIRMA_TOKEN", firmaToken.getToken(),
                "/firma/" + firmaToken.getToken() + "/otp",
                "Codigo OTP emitido para firma_token id=" + firmaToken.getIdToken());

        boolean enviado = enviarCorreoOtp(firmaToken, codigo);
        auditoriaService.registrar(enviado ? TipoEventoAuditoria.OTP_ENVIADO : TipoEventoAuditoria.OTP_ENVIO_FALLIDO,
                null, "PORTAL_FIRMA", "FIRMA_TOKEN", firmaToken.getToken(),
                "/firma/" + firmaToken.getToken() + "/otp",
                enviado ? "Correo OTP enviado a " + enmascararCorreo(fila.getCorreoDestino())
                        : "Fallo el envio de correo OTP a " + enmascararCorreo(fila.getCorreoDestino()));
        return enviado;
    }

    private String correoDelToken(FirmaToken firmaToken) {
        return actaRepository.findById(firmaToken.getIdActa())
                .map(Acta::getCorreoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada para el token de firma"));
    }

    private boolean enviarCorreoOtp(FirmaToken firmaToken, String codigo) {
        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada para el token de firma"));
        return mailService.enviarCorreoFirma(
                acta.getCorreoUsuario(),
                acta.getNombreUsuario(),
                acta.getTipoActa() != null ? acta.getTipoActa().name() : null,
                acta.getSerialEquipo(),
                firmaUrlBuilder.construir(firmaToken.getToken()),
                codigo,
                expiraMinutos);
    }

    /**
     * Estado del paso OTP. GET con efecto lateral: genera y envia un codigo si
     * el token nunca tuvo uno (legacy). {@code valido=true} solo si el cliente
     * presenta la sesion correcta (header): si no la tiene, se le da un codigo
     * nuevo aunque exista una sesion vigente en el servidor (ej. otro dispositivo).
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

        // Legacy: token sin fila -> generar y enviar uno nuevo (fallback).
        if (ultima.isEmpty()) {
            boolean enviado = generarYEnviarParaToken(firmaToken);
            FirmaOtp fresca = otpRepository
                    .findFirstByIdTokenFirmaOrderByFechaCreacionDesc(firmaToken.getIdToken()).orElseThrow();
            return new FirmaOtpEstadoResponse(false, correoEnmascarado, enviado,
                    expiraSegundos(fresca), reenviosRestantes(firmaToken.getIdToken()), 0L, false);
        }

        // Fila usada/bloqueada sin sesion vigente: el usuario debe reenviar.
        return new FirmaOtpEstadoResponse(false, correoEnmascarado, null,
                null, reenviosRestantes(firmaToken.getIdToken()), 0L, false);
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
                    "PORTAL_FIRMA", "FIRMA_TOKEN", firmaToken.getToken(),
                    "/firma/" + firmaToken.getToken() + "/otp/validar",
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
                    "PORTAL_FIRMA", "FIRMA_TOKEN", firmaToken.getToken(),
                    "/firma/" + firmaToken.getToken() + "/otp/validar",
                    "Reintento de codigo OTP ya usado (" + enmascararCorreo(fila.getCorreoDestino()) + ")");
            throw new IllegalArgumentException("Codigo incorrecto o no valido");
        }

        auditoriaService.registrar(TipoEventoAuditoria.OTP_VALIDADO, null,
                "PORTAL_FIRMA", "FIRMA_TOKEN", firmaToken.getToken(),
                "/firma/" + firmaToken.getToken() + "/otp/validar",
                "Codigo OTP validado, sesion " + sesion.substring(0, 8) + "... ("
                        + enmascararCorreo(fila.getCorreoDestino()) + ")");
        return new FirmaOtpValidarResponse(sesion);
    }

    /** Genera y envia un codigo nuevo, invalidando el anterior. */
    @Transactional
    public void reenviar(String token) {
        FirmaToken firmaToken = validadorToken.validar(token);

        FirmaOtp ultima = otpRepository
                .findFirstByIdTokenFirmaOrderByFechaCreacionDesc(firmaToken.getIdToken())
                .orElseThrow(() -> new IllegalArgumentException("No hay codigo previo; abra el enlace nuevamente"));

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
                "PORTAL_FIRMA", "FIRMA_TOKEN", firmaToken.getToken(),
                "/firma/" + firmaToken.getToken() + "/otp/reenviar",
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