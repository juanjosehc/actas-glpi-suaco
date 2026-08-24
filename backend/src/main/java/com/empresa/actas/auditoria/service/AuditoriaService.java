package com.empresa.actas.auditoria.service;

import com.empresa.actas.auditoria.entity.AuditoriaSistema;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.repository.AuditoriaSistemaRepository;
import com.empresa.actas.security.AccesoService;
import com.empresa.actas.security.UserSecurity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
        repository.save(AuditoriaSistema.builder()
                .tipoEvento(tipo)
                .usuarioId(usuarioId)
                .usuarioNombre(usuarioNombre)
                .entidad(entidad)
                .entidadId(entidadId)
                .recurso(recurso)
                .detalle(detalle)
                .ipDireccion(obtenerIp())
                .build());
    }

    /** IP del request actual; prioriza X-Forwarded-For (normalmente null en local). */
    private String obtenerIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            String fwd = req.getHeader("X-Forwarded-For");
            if (fwd != null && !fwd.isBlank()) {
                return fwd.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        }
        return null;
    }
}