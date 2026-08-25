package com.empresa.actas.firma.service;

import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.FirmaTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Validacion comun del token de firma, compartida por FirmaService y OtpService.
 * Un solo punto: la logica de token invalido/usado/vencido vive aqui y todos los
 * flujos que consumen un token pasan por aca.
 */
@Component
@RequiredArgsConstructor
public class TokenFirmaValidador {

    private final FirmaTokenRepository firmaTokenRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Valida el token de firma: debe existir, no estar usado y no vencido.
     * Antes de bloquear registra el evento correspondiente en la CAPA 2:
     *   - no existe / alterado                  -> TOKEN_INVALIDO
     *   - ya utilizado                          -> TOKEN_INVALIDO
     *   - vencido (fecha_expiracion pasada)     -> TOKEN_EXPIRADO
     */
    public FirmaToken validar(String token) {
        FirmaToken firmaToken = firmaTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    auditoriaService.registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null,
                            "PORTAL_FIRMA", "FIRMA_TOKEN", token, "/firma/" + token,
                            "Token inexistente o alterado");
                    return new IllegalArgumentException("Token no valido");
                });

        if (firmaToken.getUtilizado()) {
            auditoriaService.registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", token, "/firma/" + token,
                    "Token ya utilizado");
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }

        LocalDateTime expiracion = firmaToken.getFechaExpiracion();
        if (expiracion != null && expiracion.isBefore(LocalDateTime.now())) {
            auditoriaService.registrar(TipoEventoAuditoria.TOKEN_EXPIRADO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", token, "/firma/" + token,
                    "Token vencido (expiro el " + expiracion + ")");
            throw new IllegalArgumentException("Este enlace ha expirado, solicite uno nuevo");
        }

        return firmaToken;
    }
}