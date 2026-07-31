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
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.empresa.actas.firma.repository.FirmaTokenRepository;
import com.empresa.actas.security.UserSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .datosOriginales(request.datosOriginales())
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

    public Page<ActaResponse> listarActas(Pageable pageable) {
        return actaRepository.findAll(pageable)
                .map(this::toResponseWithToken);
    }

    public ActaResponse obtenerActaPorId(Long id) {
        Acta acta = actaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + id));
        return toResponseWithToken(acta);
    }

    public List<ActaHistorialResponse> obtenerHistorial(Long id) {
        actaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + id));

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

    @Transactional
    public void aprobarActa(Long id) {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();

        Acta acta = actaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + id));

        if (acta.getEstado() != EstadoActa.FIRMADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden aprobar actas en estado FIRMADA. Estado actual: " + acta.getEstado());
        }

        EstadoActa estadoAnterior = acta.getEstado();

        String rutaPdf = acta.getRutaPdf();
        if (rutaPdf == null || rutaPdf.isBlank()) {
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

        Acta acta = actaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + id));

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
        actaRepository.findById(idActa)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + idActa));

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
