package com.empresa.actas.firma.service;

import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.firma.entity.FirmaOtp;
import com.empresa.actas.firma.repository.FirmaOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste un intento OTP fallido en transaccion aparte (REQUIRES_NEW).
 * {@code OtpService.validar} lanza una excepcion al detectar codigo incorrecto
 * y su transaccion se revierte; si el incremento viviera en esa misma
 * transaccion, los intentos jamas se acumularian y el bloqueo por fuerza bruta
 * nunca se alcanzaria. Aqui el contador (y su auditoria) quedan a salvo.
 */
@Service
@RequiredArgsConstructor
public class OtpIntentoService {

    private final FirmaOtpRepository otpRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIncorrecto(FirmaOtp fila, String token, int maxIntentos) {
        otpRepository.incrementarIntentos(fila.getIdOtp());
        int actual = otpRepository.obtenerIntentos(fila.getIdOtp());

        auditoriaService.registrar(TipoEventoAuditoria.OTP_INVALIDO, null,
                "PORTAL_FIRMA", "FIRMA_TOKEN", token,
                "/firma/" + token + "/otp/validar",
                "Codigo OTP incorrecto, intento " + actual + "/" + maxIntentos
                        + " (" + enmascararCorreo(fila.getCorreoDestino()) + ")");

        if (actual >= maxIntentos) {
            auditoriaService.registrar(TipoEventoAuditoria.OTP_BLOQUEADO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", token,
                    "/firma/" + token + "/otp/validar",
                    "Codigo OTP bloqueado por alcanzar el maximo de intentos ("
                            + enmascararCorreo(fila.getCorreoDestino()) + ")");
        }
    }

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