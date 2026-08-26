package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import com.empresa.actas.dto.request.ActaRequest;
import com.empresa.actas.dto.request.EquipoItem;
import com.empresa.actas.dto.response.ActaResponse;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.usuario.service.UsuarioService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class DocxActaService {

    @Value("${app.generated-dir}")
    private String generatedDir;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    private final DocumentoWordService wordService;
    private final ZipService zipService;
    private final ObjectMapper objectMapper;
    private final LibreOfficePdfService libreOfficePdfService;
    private final ActaRepository actaRepository;
    private final ActaHistorialService actaHistorialService;
    private final UsuarioService usuarioService;

    public DocxActaService(
            DocumentoWordService wordService,
            ZipService zipService,
            ObjectMapper objectMapper,
            LibreOfficePdfService libreOfficePdfService,
            ActaRepository actaRepository,
            ActaHistorialService actaHistorialService,
            UsuarioService usuarioService
    ) {
        this.wordService = wordService;
        this.zipService = zipService;
        this.objectMapper = objectMapper;
        this.libreOfficePdfService = libreOfficePdfService;
        this.actaRepository = actaRepository;
        this.actaHistorialService = actaHistorialService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public ActaResponse generarActa(ActaRequest request) {
        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Map<String, Object> datos = objectMapper.convertValue(
                    request,
                    new TypeReference<Map<String, Object>>() {}
            );

            Path rutaActa = wordService.generarActa(datos);

            // Firma permanente del tecnico: se inserta en el DOCX antes de
            // empaquetar/convertir. Si no tiene firma, el placeholder queda en blanco.
            byte[] firmaTecnico = usuarioService.obtenerFirmaBytesDe(tecnicoAutenticado());
            DocxImageReplacer.reemplazarFirmaTecnico(rutaActa.toString(), firmaTecnico);

            Path rutaChecklist = wordService.generarChecklist(datos);

            String asunto = request.getAsunto()
                    .replaceAll("[^a-zA-Z0-9]", "");

            String serial = "SinSerial";
            if (request.getEquipos() != null && !request.getEquipos().isEmpty()) {
                serial = request.getEquipos().get(0).getSerial();
            }

            String nombreZip = "ActaLista_" + serial + "_" + asunto + ".zip";
            Path rutaZip = outputDir.resolve(nombreZip);

            zipService.crearZip(rutaZip, rutaActa, rutaChecklist);

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileName = libreOfficePdfService.convertirDocxAPdf(rutaActa, pdfDir);
            String rutaPdfUrl = "uploads/pdf/" + pdfFileName;

            Long idActa = persistirActa(request, rutaPdfUrl);

            return ActaResponse.ok(nombreZip, rutaPdfUrl, idActa);

        } catch (Exception e) {
            return ActaResponse.error("Error generando documentacion: " + e.getMessage());
        }
    }

    /**
     * Persiste la entidad Acta generada (entrega) en el mismo flujo que la
     * generacion de documentos, para que si el PDF se genera la acta quede
     * registrada en PostgreSQL de forma atomica (ya no depende de una llamada
     * /actas aparte del frontend).
     */
    private Long persistirActa(ActaRequest request, String rutaPdfUrl) {
        Long idTecnico = tecnicoAutenticado();

        Acta acta = Acta.builder()
                .idTecnico(idTecnico)
                .ticketGlpi(parseLongNullable(request.getNumero_sac()))
                .tipoActa(TipoActa.ENTREGA)
                .estado(EstadoActa.GENERADA)
                .cedulaUsuario(null)
                .nombreUsuario(request.getEntregado_a())
                .serialEquipo(primerSerial(request))
                .placaEquipo(primerInventario(request))
                .descripcionEquipo(descripcionEquipo(request))
                .contenidoHtml(null)
                .rutaPdf(rutaPdfUrl)
                .datosOriginales(serializar(request))
                .build();

        Acta guardada = actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                guardada.getIdActa(),
                TipoEventoActa.ACTA_GENERADA,
                null,
                EstadoActa.GENERADA,
                idTecnico,
                idTecnico != null ? String.valueOf(idTecnico) : "SISTEMA",
                null,
                "Acta de entrega generada: " + rutaPdfUrl);

        return guardada.getIdActa();
    }

    private Long tecnicoAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof UserSecurity us) {
            return us.getUsuario().getIdUsuario();
        }
        return null;
    }

    private Long parseLongNullable(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Long.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String primerSerial(ActaRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty() || r.getEquipos().get(0).getSerial() == null) return null;
        return r.getEquipos().get(0).getSerial();
    }

    private String primerInventario(ActaRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty()) return null;
        EquipoItem eq = r.getEquipos().get(0);
        if (eq.getInventario() == null) return null;
        return eq.getInventario();
    }

    private String descripcionEquipo(ActaRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty()) return null;
        EquipoItem eq = r.getEquipos().get(0);
        return eq.getMarca() + " " + eq.getModelo();
    }

    private String serializar(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }
}
