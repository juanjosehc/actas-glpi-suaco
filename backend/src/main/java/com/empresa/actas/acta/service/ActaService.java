package com.empresa.actas.acta.service;

import com.empresa.actas.acta.dto.ActaHistorialResponse;
import com.empresa.actas.acta.dto.ActaResponse;
import com.empresa.actas.acta.dto.CrearActaRequest;
import com.empresa.actas.acta.dto.EvidenciaResponse;
import com.empresa.actas.acta.dto.RechazarRequest;
import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.mapper.ActaMapper;
import com.empresa.actas.acta.repository.ActaHistorialRepository;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.empresa.actas.firma.repository.FirmaTokenRepository;
import com.empresa.actas.security.AccesoService;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.service.SignedDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActaService {

    private final ActaRepository actaRepository;
    private final ActaHistorialRepository actaHistorialRepository;
    private final ActaHistorialService actaHistorialService;
    private final EvidenciaRepository evidenciaRepository;
    private final FirmaTokenRepository firmaTokenRepository;
    private final ActaMapper actaMapper;
    private final PdfService pdfService;
    private final AccesoService accesoService;
    private final AuditoriaService auditoriaService;
    private final SignedDocumentService signedDocumentService;
    private final ObjectMapper objectMapper;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    @Transactional
    public ActaResponse crearActa(CrearActaRequest request) {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();

        TipoActa tipoActa = parseTipoActa(request.tipoActa());

        Acta acta = Acta.builder()
                .idTecnico(userSecurity.getUsuario().getIdUsuario())
                .ticketGlpi(request.ticketGlpi())
                .tipoActa(tipoActa)
                .estado(EstadoActa.GENERADA)
                .cedulaUsuario(request.cedulaUsuario())
                .nombreUsuario(request.nombreUsuario())
                .correoUsuario(request.correoUsuario())
                .serialEquipo(request.serialEquipo())
                .placaEquipo(request.placaEquipo())
                .descripcionEquipo(request.descripcionEquipo())
                .contenidoHtml(request.contenidoHtml())
                .rutaPdf(request.rutaPdf())
                .datosOriginales(datosOriginalesO(request))
                .build();

        Acta actaGuardada = actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                actaGuardada.getIdActa(),
                TipoEventoActa.ACTA_GENERADA,
                null,
                EstadoActa.GENERADA,
                userSecurity.getUsuario().getIdUsuario(),
                userSecurity.getUsername(),
                null,
                "Acta generada");

        return actaMapper.toResponse(actaGuardada);
    }

    /**
     * QA-04: el documento firmado se regenera desde datosOriginales. Si el
     * request no los trae (p.ej. acta creada a mano sin plantilla), se
     * serializa el request completo para que la regeneracion SIEMPRE tenga
     * base y nunca quede una acta firmada sin documento.
     */
    private String datosOriginalesO(CrearActaRequest request) {
        if (request.datosOriginales() != null && !request.datosOriginales().isBlank()) {
            return request.datosOriginales();
        }
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            return null;
        }
    }

    public Page<ActaResponse> listarActas(String q, Pageable pageable) {
        // ROL TECNICO: solo ve actas que el mismo creo (idTecnico == su id).
        // ADMINISTRADOR / AUDITOR: ve todas.
        Long idTecnico = accesoService.esTecnico()
                ? accesoService.usuarioActual().getUsuario().getIdUsuario()
                : null;

        if (q == null || q.isBlank()) {
            if (idTecnico != null) {
                return actaRepository.findByIdTecnico(idTecnico, pageable)
                        .map(this::toResponseWithToken);
            }
            return actaRepository.findAll(pageable)
                    .map(this::toResponseWithToken);
        }
        return actaRepository.buscar(q.trim(), idTecnico, pageable)
                .map(this::toResponseWithToken);
    }

    public ActaResponse obtenerActaPorId(Long id) {
        Acta acta = cargarActaConAcceso(id);
        return toResponseWithToken(acta);
    }

    public List<ActaHistorialResponse> obtenerHistorial(Long id) {
        cargarActaConAcceso(id);

        return actaHistorialRepository.findByIdActaOrderByFechaCambioDesc(id).stream()
                .map(h -> ActaHistorialResponse.builder()
                        .idHistorial(h.getIdHistorial())
                        .idActa(h.getIdActa())
                        .tipoEvento(h.getTipoEvento() != null ? h.getTipoEvento().name() : null)
                        .estadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().name() : null)
                        .estadoNuevo(h.getEstadoNuevo() != null ? h.getEstadoNuevo().name() : null)
                        .actorId(h.getActorId())
                        .actorNombre(h.getActorNombre())
                        .idTokenFirma(h.getIdTokenFirma())
                        .fechaCambio(h.getFechaCambio())
                        .observacion(h.getObservacion())
                        .build())
                .toList();
    }

    private ActaResponse toResponseWithToken(Acta acta) {
        ActaResponse resp = actaMapper.toResponse(acta);
        if (acta.getEstado() == EstadoActa.ENVIADA) {
            firmaTokenRepository.findFirstByIdActaOrderByFechaCreacionDesc(acta.getIdActa())
                    .ifPresent(t -> resp.setTokenFirma(t.getToken()));
        }
        return resp;
    }

    /**
     * Carga el acta y valida acceso segun rol/propietario.
     * TECNICO no puede ver/operar actas de otro tecnico (403).
     */
    private Acta cargarActaConAcceso(Long id) {
        Acta acta = actaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + id));
        accesoService.verificarAccesoActa(acta);
        return acta;
    }

    /**
     * Sirve el PDF del acta validando acceso. TECNICO solo el de sus actas.
     * Registra DOCUMENTO_VISTO (CAPA 2) solo si el archivo existe.
     * Devuelve null si el archivo no existe.
     */
    public Resource obtenerPdfConAcceso(Long idActa) {
        Acta acta = cargarActaConAcceso(idActa);
        Resource recurso = resolverArchivo(acta.getRutaPdf());
        if (recurso != null) {
            auditoriaService.registrar(TipoEventoAuditoria.DOCUMENTO_VISTO,
                    "ACTA", String.valueOf(idActa), "/actas/" + idActa + "/pdf",
                    "PDF del acta visualizado/descargado");
        }
        return recurso;
    }

    /**
     * Sirve el PDF del checklist de entrega validando acceso (expediente
     * documental de ENTREGA). Mismas reglas que {@link #obtenerPdfConAcceso}.
     * Devuelve null si el acta no tiene checklist o el archivo no existe.
     */
    public Resource obtenerChecklistPdfConAcceso(Long idActa) {
        Acta acta = cargarActaConAcceso(idActa);
        Resource recurso = resolverArchivo(acta.getRutaPdfChecklist());
        if (recurso != null) {
            auditoriaService.registrar(TipoEventoAuditoria.DOCUMENTO_VISTO,
                    "ACTA", String.valueOf(idActa), "/actas/" + idActa + "/checklist/pdf",
                    "Checklist de Entrega visualizado/descargado (documento asociado del expediente)");
        }
        return recurso;
    }

    /** Sirve la imagen de firma del acta validando acceso (firma_{id}.png). */
    public Resource obtenerFirmaConAcceso(Long idActa) {
        Acta acta = cargarActaConAcceso(idActa);
        Resource recurso = resolverArchivo("uploads/firmas/firma_" + acta.getIdActa() + ".png");
        if (recurso != null) {
            auditoriaService.registrar(TipoEventoAuditoria.EVIDENCIA_VISTA,
                    "ACTA", String.valueOf(idActa), "/actas/" + idActa + "/firma",
                    "Evidencia FIRMA (firma_" + idActa + ".png) visualizada");
        }
        return recurso;
    }

    /** Sirve la fotografia del acta validando acceso (foto_{id}.jpg). */
    public Resource obtenerFotoConAcceso(Long idActa) {
        Acta acta = cargarActaConAcceso(idActa);
        Resource recurso = resolverArchivo("uploads/fotos/foto_" + acta.getIdActa() + ".jpg");
        if (recurso != null) {
            auditoriaService.registrar(TipoEventoAuditoria.EVIDENCIA_VISTA,
                    "ACTA", String.valueOf(idActa), "/actas/" + idActa + "/foto",
                    "Evidencia FOTO (foto_" + idActa + ".jpg) visualizada");
        }
        return recurso;
    }

    /**
     * Resuelve una ruta virtual ({@code uploads/<...>}) al archivo en disco.
     * Devuelve null si no existe o la ruta no es de uploads.
     */
    private Resource resolverArchivo(String rutaVirtual) {
        if (rutaVirtual == null || !rutaVirtual.startsWith("uploads/")) {
            return null;
        }
        Path archivo = Paths.get(uploadsDir).resolve(rutaVirtual.substring("uploads/".length()));
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo)) {
            return null;
        }
        return new FileSystemResource(archivo.toFile());
    }

    @Transactional
    public void aprobarActa(Long id) {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();

        Acta acta = cargarActaConAcceso(id);

        if (acta.getEstado() != EstadoActa.FIRMADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden aprobar actas en estado FIRMADA. Estado actual: " + acta.getEstado());
        }

        // QA-06: antes de aprobar se garantiza que el PDF del acta sea el
        // documento FIRMADO (regenerado con la firma embebida). Si la
        // regeneracion async sigue en curso, este metodo espera o regenera
        // aqui mismo; la aprobacion ya no cae en la ventana de la carrera.
        signedDocumentService.regenerarDocumentoFirmadoParaAprobacion(acta);

        EstadoActa estadoAnterior = acta.getEstado();

        String rutaPdf = acta.getRutaPdf();
        if (rutaPdf == null || rutaPdf.isBlank()) {
            // Solo llega aqui un acta legacy sin datosOriginales ni documento:
            // se genera un PDF_FINAL basico para no dejar el acta sin archivo.
            rutaPdf = pdfService.generarPdfFinal(acta);

            Evidencia evidenciaPdf = Evidencia.builder()
                    .idActa(id)
                    .tipo(Evidencia.TipoEvidencia.PDF_FINAL)
                    .rutaArchivo(rutaPdf)
                    .build();
            evidenciaRepository.save(evidenciaPdf);

            actaHistorialService.registrarEvento(
                    id,
                    TipoEventoActa.EVIDENCIA_CARGADA,
                    null,
                    acta.getEstado(),
                    null,
                    "SISTEMA",
                    null,
                    "Tipo: PDF_FINAL - " + rutaPdf);
        }

        acta.setEstado(EstadoActa.APROBADA);
        acta.setFechaAprobacion(LocalDateTime.now());
        acta.setRutaPdf(rutaPdf);
        actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                id,
                TipoEventoActa.ACTA_APROBADA,
                estadoAnterior,
                EstadoActa.APROBADA,
                userSecurity.getUsuario().getIdUsuario(),
                userSecurity.getUsername(),
                null,
                "Acta aprobada");
    }

    @Transactional
    public void rechazarActa(Long id, RechazarRequest request) {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();

        Acta acta = cargarActaConAcceso(id);

        if (acta.getEstado() != EstadoActa.FIRMADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden rechazar actas en estado FIRMADA. Estado actual: " + acta.getEstado());
        }

        EstadoActa estadoAnterior = acta.getEstado();

        acta.setEstado(EstadoActa.RECHAZADA);
        acta.setFechaRechazo(LocalDateTime.now());
        acta.setObservacionRechazo(request.observacion());
        actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                id,
                TipoEventoActa.ACTA_RECHAZADA_ADMIN,
                estadoAnterior,
                EstadoActa.RECHAZADA,
                userSecurity.getUsuario().getIdUsuario(),
                userSecurity.getUsername(),
                null,
                request.observacion());
    }

    public List<EvidenciaResponse> obtenerEvidencias(Long idActa) {
        cargarActaConAcceso(idActa);

        return evidenciaRepository.findByIdActa(idActa).stream()
                .map(e -> EvidenciaResponse.builder()
                        .id(e.getIdEvidencia())
                        .tipo(e.getTipo().name())
                        .rutaArchivo(e.getRutaArchivo())
                        .build())
                .toList();
    }

    private TipoActa parseTipoActa(String tipo) {
        try {
            return TipoActa.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Tipo de acta invalido: " + tipo + ". Valores permitidos: ENTREGA, DEVOLUCION");
        }
    }
}
