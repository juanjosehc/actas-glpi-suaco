package com.empresa.actas.controller;

import com.empresa.actas.dto.response.UsuarioGlpiResponse;
import com.empresa.actas.service.UsuarioGlpiService;
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
public class UsuarioGlpiController {

    private final UsuarioGlpiService usuarioGlpiService;

    public UsuarioGlpiController(UsuarioGlpiService usuarioGlpiService) {
        this.usuarioGlpiService = usuarioGlpiService;
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
