package com.empresa.actas.auditoria.service;

import com.empresa.actas.auditoria.entity.AuditoriaSistema;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.repository.AuditoriaSistemaRepository;
import com.empresa.actas.security.AccesoService;
import com.empresa.actas.security.UserSecurity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * CAPA 2 de auditoria: seguridad, accesos, autenticacion y visualizaciones.
 * Persiste en {@code auditoria_sistema}, independiente de {@code acta_historial}.
 *
 * {@code REQUIRES_NEW}: el evento se persiste aunque la operacion que audita
 * falle/reviente — un registro de auditoria no debe perderse con un rollback.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaSistemaRepository repository;
    private final AccesoService accesoService;

    /**
     * SEC-121: la IP de auditoria confia en X-Forwarded-For SOLO si el servidor
     * esta detras de un proxy de confianza (mismo flag que RateLimitFilter).
     * Si se expone directo, una cabecera falsificada no debe contaminar la
     * auditoria (registraria la IP del atacante inventada).
     */
    @Value("${security.rate-limit.trust-x-forwarded-for:false}")
    private boolean trustXForwardedFor;

    /**
     * Registra un evento tomando el usuario autenticado del contexto
     * (null si el flujo es anonimo, ej. portal publico de firma).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(TipoEventoAuditoria tipo, String entidad, String entidadId,
                          String recurso, String detalle) {
        UserSecurity us = accesoService.usuarioActual();
        Long id = us != null ? us.getUsuario().getIdUsuario() : null;
        String nombre = us != null ? us.getUsername() : null;
        registrar(tipo, id, nombre, entidad, entidadId, recurso, detalle);
    }

    /**
     * Registra un evento con actor explicito (login fallido: usuario intentado;
     * tokens del portal: sin usuario del sistema).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(TipoEventoAuditoria tipo, Long usuarioId, String usuarioNombre,
                          String entidad, String entidadId, String recurso, String detalle) {
        registrarConEvidencia(tipo, usuarioId, usuarioNombre, entidad, entidadId, recurso, detalle,
                null, null, null);
    }

    /**
     * FORTALECIMIENTO_EVIDENCIA_FIRMA: variante que ademas deja evidencia
     * estructurada del envio SMTP (message_id integro, hash_correo, estado) en
     * columnas tipadas de auditoria_sistema. Usado por los eventos OTP
     * (ENVIADO/ENVIO_FALLIDO/VALIDADO) para consolidar la trazabilidad del correo
     * en la unica fuente de verdad, sin una tabla redundante.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarConEvidencia(TipoEventoAuditoria tipo, Long usuarioId, String usuarioNombre,
                                      String entidad, String entidadId, String recurso, String detalle,
                                      String messageId, String hashCorreo, String estadoEnvio) {
        repository.save(AuditoriaSistema.builder()
                .tipoEvento(tipo)
                .usuarioId(usuarioId)
                .usuarioNombre(usuarioNombre)
                .entidad(entidad)
                .entidadId(entidadId)
                .recurso(recurso)
                .detalle(detalle)
                .messageId(messageId)
                .hashCorreo(hashCorreo)
                .estadoEnvio(estadoEnvio)
                .ipDireccion(obtenerIp())
                .build());
    }

    /** IP del request actual; X-Forwarded-For solo si el flag de proxy de confianza esta activo. */
    private String obtenerIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            if (trustXForwardedFor) {
                String fwd = req.getHeader("X-Forwarded-For");
                if (fwd != null && !fwd.isBlank()) {
                    return fwd.split(",")[0].trim();
                }
            }
            return req.getRemoteAddr();
        }
        return null;
    }
}