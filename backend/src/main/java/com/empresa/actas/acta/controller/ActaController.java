package com.empresa.actas.acta.controller;

import com.empresa.actas.acta.dto.ActaHistorialResponse;
import com.empresa.actas.acta.dto.ActaResponse;
import com.empresa.actas.acta.dto.CrearActaRequest;
import com.empresa.actas.acta.dto.EvidenciaResponse;
import com.empresa.actas.acta.dto.RechazarRequest;
import com.empresa.actas.acta.service.ActaService;
import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.firma.dto.EnviarActaRequest;
import com.empresa.actas.firma.dto.EnviarActaResponse;
import com.empresa.actas.firma.service.FirmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.Set;

@RestController
@RequestMapping("/actas")
@RequiredArgsConstructor
@Tag(name = "Actas", description = "Gestion de actas de entrega/devolucion de equipos")
public class ActaController {

    private final ActaService actaService;
    private final FirmaService firmaService;

    /** SEC-013: orden por defecto y campo sobre el que se ordena si no viene/non-valido. */
    static final String SORT_DEFAULT = "fechaCreacion";

    /**
     * SEC-013: allow-list de propiedades de {@code Acta} ordenables. {@code Sort.by}
     * acepta cualquier nombre de propiedad y lanza InvalidDataAccessApiUsageException
     * ante una inexistente; ademas abre superficie a nombres arbitrarios. Solo se
     * permiten columnas reales de la entidad; lo no listado cae al default.
     */
    private static final Set<String> CAMPOS_ORDEN = Set.of(
            "idActa", "idTecnico", "ticketGlpi", "tipoActa", "estado",
            "cedulaUsuario", "nombreUsuario", "correoUsuario", "serialEquipo", "placaEquipo",
            "descripcionEquipo", "observacionRechazo",
            "fechaRechazo", "rutaPdf", "rutaPdfChecklist", "datosOriginales", "fechaCreacion",
            "fechaEnvio", "fechaFirma", "fechaAprobacion");

    /** SEC-013: normaliza el parametro de orden a un campo valido de la whitelist. */
    static String sortPermitido(String sort) {
        if (sort == null) {
            return SORT_DEFAULT;
        }
        return CAMPOS_ORDEN.contains(sort) ? sort : SORT_DEFAULT;
    }

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
            @RequestParam(defaultValue = "fechaCreacion") String sort,
            @RequestParam(required = false) String q) {

        Page<ActaResponse> actas = actaService.listarActas(
                q, PageRequest.of(page, size, Sort.by(sortPermitido(sort)).descending()));
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

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener PDF del acta", description = "Sirve el PDF del acta validando acceso por rol/propietario")
    public ResponseEntity<Resource> obtenerPdf(@PathVariable Long id) {
        Resource recurso = actaService.obtenerPdfConAcceso(id);
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"acta_" + id + ".pdf\"")
                .body(recurso);
    }

    @GetMapping("/{id}/checklist/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener PDF del checklist de entrega", description = "Sirve el PDF del checklist de entrega validando acceso por rol/propietario")
    public ResponseEntity<Resource> obtenerChecklistPdf(@PathVariable Long id) {
        Resource recurso = actaService.obtenerChecklistPdfConAcceso(id);
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"checklist_" + id + ".pdf\"")
                .body(recurso);
    }

    @GetMapping("/{id}/zip")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener ZIP del acta", description = "Sirve el ZIP generado (DOCX) validando acceso por rol/propietario")
    public ResponseEntity<Resource> obtenerZip(
            @PathVariable Long id) {
        Resource recurso = actaService.obtenerZipConAcceso(id);
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + (recurso.getFilename() != null ? recurso.getFilename() : "acta_" + id + ".zip") + "\"")
                .body(recurso);
    }

    @PostMapping("/{id}/reintentar-generacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @Operation(summary = "Reintentar generacion de documentos", description = "Re-encola la generacion DOCX/ZIP/PDF de una acta en GENERACION_FALLIDA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Generacion re-encolada"),
            @ApiResponse(responseCode = "400", description = "Estado invalido para reintento"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada")
    })
    public ResponseEntity<ErrorResponse> reintentarGeneracion(@PathVariable Long id) {
        actaService.reintentarGeneracion(id);
        return ResponseEntity.ok(ErrorResponse.ok("Generacion de documentos re-encolada"));
    }

    @GetMapping("/{id}/firma")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener imagen de firma del acta", description = "Sirve la firma del acta validando acceso por rol/propietario")
    public ResponseEntity<Resource> obtenerFirma(@PathVariable Long id) {
        Resource recurso = actaService.obtenerFirmaConAcceso(id);
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"firma_" + id + ".png\"")
                .body(recurso);
    }

    @GetMapping("/{id}/foto")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener fotografia del acta", description = "Sirve la foto del acta validando acceso por rol/propietario")
    public ResponseEntity<Resource> obtenerFoto(@PathVariable Long id) {
        Resource recurso = actaService.obtenerFotoConAcceso(id);
        if (recurso == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"foto_" + id + ".jpg\"")
                .body(recurso);
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
    public ResponseEntity<ErrorResponse> enviarActa(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) EnviarActaRequest request) {
        String correo = request != null ? request.correo() : null;
        EnviarActaResponse response = firmaService.enviarActa(id, correo);
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

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    @Operation(summary = "Obtener historial de auditoria de un acta", description = "Lista todos los eventos de auditoria de un acta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial listado"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada")
    })
    public ResponseEntity<ErrorResponse> obtenerHistorial(@PathVariable Long id) {
        List<ActaHistorialResponse> historial = actaService.obtenerHistorial(id);
        return ResponseEntity.ok(ErrorResponse.ok("Historial de acta", historial));
    }
}
