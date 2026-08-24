package com.empresa.actas.auditoria.controller;

import com.empresa.actas.auditoria.entity.AuditoriaSistema;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.repository.AuditoriaSistemaRepository;
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

/**
 * Consulta de auditoria de sistema (CAPA 2). Solo lectura para
 * ADMINISTRADOR / AUDITOR; los eventos quedan registrados por el flujo.
 */
@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
@Tag(name = "Auditoria de sistema", description = "Consulta de eventos de seguridad, accesos y autenticacion")
public class AuditoriaController {

    private final AuditoriaSistemaRepository repository;

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
}