package com.empresa.actas.security;

import com.empresa.actas.acta.entity.Acta;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Reglas de visibilidad por rol y propietario.
 *
 * ROL TECNICO: solo ve/opera actas donde {code acta.idTecnico == su id}.
 *   (firmas, evidencias e historial cuelgan del acta, se restringen igual)
 * ROL ADMINISTRADOR / AUDITOR: acceso total a todas las actas y firmas.
 */
@Service
public class AccesoService {

    /** Rol tal como lo guarda Rol.nombre (ROLE_ en authorities). */
    public static final String ROL_TECNICO = "TECNICO";

    /** Usuario autenticado; null si es flujo anonimo (portal publico). */
    public UserSecurity usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserSecurity us) {
            return us;
        }
        return null;
    }

    public boolean esTecnico() {
        UserSecurity us = usuarioActual();
        return us != null && us.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + ROL_TECNICO));
    }

    /**
     * Verifica que el usuario autenticado pueda acceder al acta.
     * TECNICO solo si es el creador (idTecnico). Administrador/Auditor siempre.
     * Flujo anonimo (portal publico) no restringe.
     *
     * @throws AccessDeniedException si un TECNICO intenta acceder a acta de otro.
     */
    public void verificarAccesoActa(Acta acta) {
        if (!esTecnico()) {
            return; // ADMINISTRADOR, AUDITOR o anonimo: acceso total
        }
        UserSecurity us = usuarioActual();
        Long idTecnicoActa = acta.getIdTecnico();
        Long idTecnicoActual = us != null ? us.getUsuario().getIdUsuario() : null;

        if (idTecnicoActa == null || !idTecnicoActa.equals(idTecnicoActual)) {
            throw new AccessDeniedException(
                    "No tiene permisos para acceder a esta acta (pertenece a otro tecnico)");
        }
    }
}
