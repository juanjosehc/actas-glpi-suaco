package com.empresa.actas.auditoria.controller;

import com.empresa.actas.auditoria.dto.EventosAuditoriaResponse;
import com.empresa.actas.auditoria.entity.AuditoriaSistema;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.repository.AuditoriaSistemaRepository;
import com.empresa.actas.auditoria.service.AuditoriaConsultaService;
import com.empresa.actas.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Consulta de auditoria. Solo lectura para ADMINISTRADOR / AUDITOR;
 * los eventos quedan registrados por el flujo.
 *
 * - GET /auditoria          : auditoria de sistema cruda (CAPA 2, historico).
 * - GET /auditoria/eventos  : visor unificado acta_historial + auditoria_sistema.
 * - GET /auditoria/estadisticas : contadores agregados.
 */
@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
@Tag(name = "Auditoria", description = "Consulta de eventos de seguridad, accesos, autenticacion y actas")
public class AuditoriaController {

    private final AuditoriaSistemaRepository repository;
    private final AuditoriaConsultaService consultaService;

    @GetMapping
    @Operation(summary = "Listar eventos de auditoria de sistema",
            description = "Lista paginada de eventos; opcional filtrar por tipo_evento")
    public ResponseEntity<ErrorResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String tipo) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by("fechaEvento").descending());

        Page<AuditoriaSistema> resultado;
        if (tipo != null && !tipo.isBlank()) {
            // Tipo invalido lanza IllegalArgumentException -> 400 (GlobalExceptionHandler)
            resultado = repository.findByTipoEvento(TipoEventoAuditoria.valueOf(tipo.trim().toUpperCase()), pageable);
        } else {
            resultado = repository.findAll(pageable);
        }

        return ResponseEntity.ok(ErrorResponse.ok("Auditoria de sistema", resultado));
    }

    @GetMapping("/eventos")
    @Operation(summary = "Visor unificado de auditoria",
            description = "Fusiona acta_historial y auditoria_sistema en una sola vista con "
                    + "filtros por fecha, usuario, rol, tipo de evento, acta, estado y busqueda general")
    public ResponseEntity<ErrorResponse> eventos(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) Long idActa,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        EventosAuditoriaResponse resultado = consultaService.consultar(
                parseFecha(desde), parseFecha(hasta), usuario, rol, tipoEvento,
                idActa, estado, q, page, size);
        return ResponseEntity.ok(ErrorResponse.ok("Eventos de auditoria", resultado));
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Estadisticas de auditoria",
            description = "Contadores agregados (eventos por dia, firmas, accesos, errores, accesos denegados) "
                    + "sobre los eventos que cumplen los filtros")
    public ResponseEntity<ErrorResponse> estadisticas(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) Long idActa,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q) {

        Map<String, Object> resultado = consultaService.estadisticas(
                parseFecha(desde), parseFecha(hasta), usuario, rol, tipoEvento,
                idActa, estado, q);
        return ResponseEntity.ok(ErrorResponse.ok("Estadisticas de auditoria", resultado));
    }

    private LocalDate parseFecha(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}