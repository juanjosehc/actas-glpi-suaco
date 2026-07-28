package com.empresa.actas.acta.controller;

import com.empresa.actas.acta.dto.ActaResponse;
import com.empresa.actas.acta.dto.CrearActaRequest;
import com.empresa.actas.acta.dto.EvidenciaResponse;
import com.empresa.actas.acta.dto.RechazarRequest;
import com.empresa.actas.acta.service.ActaService;
import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.firma.dto.EnviarActaResponse;
import com.empresa.actas.firma.service.FirmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/actas")
@RequiredArgsConstructor
@Tag(name = "Actas", description = "Gestion de actas de entrega/devolucion de equipos")
public class ActaController {

    private final ActaService actaService;
    private final FirmaService firmaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Listar actas", description = "Lista todas las actas con paginacion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actas listadas"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    public ResponseEntity<ErrorResponse> listarActas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaCreacion") String sort) {

        Page<ActaResponse> actas = actaService.listarActas(
                PageRequest.of(page, size, Sort.by(sort).descending()));
        return ResponseEntity.ok(ErrorResponse.ok("Actas listadas", actas));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener acta por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta encontrada"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada")
    })
    public ResponseEntity<ErrorResponse> obtenerActa(@PathVariable Long id) {
        ActaResponse acta = actaService.obtenerActaPorId(id);
        return ResponseEntity.ok(ErrorResponse.ok("Acta encontrada", acta));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @Operation(summary = "Crear acta", description = "Crea una nueva acta de entrega/devolucion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Acta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    public ResponseEntity<ErrorResponse> crearActa(
            @Valid @RequestBody CrearActaRequest request) {
        ActaResponse acta = actaService.crearActa(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ErrorResponse.ok("Acta creada exitosamente", acta));
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @Operation(summary = "Enviar acta para firma", description = "Genera token de firma y cambia estado a ENVIADA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta enviada correctamente"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada"),
            @ApiResponse(responseCode = "400", description = "Estado invalido para envio")
    })
    public ResponseEntity<ErrorResponse> enviarActa(@PathVariable Long id) {
        EnviarActaResponse response = firmaService.enviarActa(id);
        return ResponseEntity.ok(ErrorResponse.ok("Acta enviada correctamente", response));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @Operation(summary = "Aprobar acta firmada", description = "Aprueba un acta firmada, genera PDF y registra evidencia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta aprobada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada"),
            @ApiResponse(responseCode = "400", description = "Estado invalido para aprobacion")
    })
    public ResponseEntity<ErrorResponse> aprobarActa(@PathVariable Long id) {
        actaService.aprobarActa(id);
        return ResponseEntity.ok(ErrorResponse.ok("Acta aprobada exitosamente"));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @Operation(summary = "Rechazar acta firmada", description = "Rechaza un acta firmada con observacion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta rechazada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada"),
            @ApiResponse(responseCode = "400", description = "Estado invalido para rechazo")
    })
    public ResponseEntity<ErrorResponse> rechazarActa(
            @PathVariable Long id,
            @Valid @RequestBody RechazarRequest request) {
        actaService.rechazarActa(id, request);
        return ResponseEntity.ok(ErrorResponse.ok("Acta rechazada exitosamente"));
    }

    @GetMapping("/{id}/evidencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener evidencias de un acta", description = "Lista todas las evidencias (firma, foto, PDF) de un acta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evidencias listadas"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada")
    })
    public ResponseEntity<ErrorResponse> obtenerEvidencias(@PathVariable Long id) {
        List<EvidenciaResponse> evidencias = actaService.obtenerEvidencias(id);
        return ResponseEntity.ok(ErrorResponse.ok("Evidencias listadas", evidencias));
    }
}
