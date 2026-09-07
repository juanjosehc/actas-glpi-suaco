package com.empresa.actas.controller;

import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.dto.response.UsuarioGlpiResponse;
import com.empresa.actas.service.UsuarioGlpiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador para la consulta de usuarios desde GLPI.
 *
 * Endpoints:
 * - GET /usuario?q={texto} → Busca usuarios por cualquier dato asociado
 *   (login, nombre, apellido, nombre completo, correo o combinaciones).
 * - GET /usuario/diagnostico → Estado del cache de usuarios GLPI
 *   (cantidad cargada, ultima carga, ultimo error) para depurar por que
 *   el autocompletado devuelve vacio.
 *
 * Utilizado por el frontend para auto completar el usuario receptor
 * en los formularios de entrega y devolucion, reutilizando el campo
 * existente (entregado_a / recibido_por) sin agregar campos nuevos.
 */
@RestController
@Tag(name = "GLPI", description = "Consultas de catalogo contra GLPI (usuarios, equipos)")
public class UsuarioGlpiController {

    private final UsuarioGlpiService usuarioGlpiService;
    private final AuditoriaService auditoriaService;

    public UsuarioGlpiController(UsuarioGlpiService usuarioGlpiService,
                                 AuditoriaService auditoriaService) {
        this.usuarioGlpiService = usuarioGlpiService;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Busca usuarios en GLPI por texto.
     *
     * @param q texto de busqueda (nombre, apellido, correo o login)
     * @return lista de usuarios activos, con o sin correo
     */
    @GetMapping("/usuario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public List<UsuarioGlpiResponse> buscarUsuarios(
            @RequestParam(name = "q", required = false, defaultValue = "") String q) {
        // SEC-125: auditoria de consultas al catalogo GLPI (personas = datos
        // personales). Solo consultas reales (>=2 chars, como valida el servicio);
        // las consultas vacias/cortas del autocompletado no ensucian la auditoria.
        if (q != null && q.length() >= 2) {
            auditoriaService.registrar(TipoEventoAuditoria.CONSULTA_GLPI_USUARIOS,
                    "USUARIO_GLPI", String.valueOf(q.length()),
                    "/usuario", "Consulta de usuarios GLPI q=" + q);
        }
        return usuarioGlpiService.buscarUsuarios(q);
    }

    /**
     * Diagnostico del cache de usuarios GLPI.
     */
    @GetMapping("/usuario/diagnostico")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> diagnostico() {
        return usuarioGlpiService.getDiagnostico();
    }
}
