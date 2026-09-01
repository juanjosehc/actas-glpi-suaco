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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DocxActaService.class);

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
            // La firma/foto del USUARIO aun no existe: se dejan en blanco para que
            // el DOCX/PDF inicial no muestre {{firma_usuario}} / {{foto_usuario}} crudos.
            byte[] firmaTecnico = usuarioService.obtenerFirmaBytesDe(tecnicoAutenticado());
            DocxImageReplacer.reemplazarFirmaTecnico(rutaActa.toString(), firmaTecnico);
            DocxImageReplacer.reemplazarFirmaYFoto(rutaActa.toString(), null, null);

            Path rutaChecklist = wordService.generarChecklist(datos);

            // El checklist tambien trae {{firma_tecnico}} y {{firma_usuario}}:
            // misma regla — tecnico insertado o en blanco, usuario en blanco.
            DocxImageReplacer.reemplazarFirmaTecnico(rutaChecklist.toString(), firmaTecnico);
            DocxImageReplacer.reemplazarFirmaYFoto(rutaChecklist.toString(), null, null);

            String asunto = NombreArchivoSeguro.segmento(request.getAsunto());

            String serial = "SinSerial";
            if (request.getEquipos() != null && !request.getEquipos().isEmpty()) {
                serial = NombreArchivoSeguro.segmento(request.getEquipos().get(0).getSerial());
            }

            String nombreZip = "ActaLista_" + serial + "_" + asunto + "_" + sufijoUnico() + ".zip";
            Path rutaZip = outputDir.resolve(nombreZip);

            zipService.crearZip(rutaZip, rutaActa, rutaChecklist);

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileName = libreOfficePdfService.convertirDocxAPdf(rutaActa, pdfDir);
            String rutaPdfUrl = "uploads/pdf/" + pdfFileName;

            // Expediente documental (ENTREGA): el checklist tambien se convierte
            // a PDF y queda vinculado a la acta (rutaPdfChecklist), igual que el
            // acta. Desde el inicio ambos documentos existen como PDF.
            String checklistPdfFileName = libreOfficePdfService.convertirDocxAPdf(rutaChecklist, pdfDir);
            String rutaChecklistPdfUrl = "uploads/pdf/" + checklistPdfFileName;

            Long idActa = persistirActa(request, rutaPdfUrl, rutaChecklistPdfUrl);

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
    private Long persistirActa(ActaRequest request, String rutaPdfUrl, String rutaChecklistPdfUrl) {
        Long idTecnico = tecnicoAutenticado();

        Acta acta = Acta.builder()
                .idTecnico(idTecnico)
                .ticketGlpi(parseLongNullable(request.getNumero_sac()))
                .tipoActa(TipoActa.ENTREGA)
                .estado(EstadoActa.GENERADA)
                .cedulaUsuario(null)
                .nombreUsuario(request.getEntregado_a())
                .correoUsuario(blankToNull(request.getCorreo()))
                .serialEquipo(primerSerial(request))
                .placaEquipo(primerInventario(request))
                .descripcionEquipo(descripcionEquipo(request))
                .contenidoHtml(null)
                .rutaPdf(rutaPdfUrl)
                .rutaPdfChecklist(rutaChecklistPdfUrl)
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
                "Acta de entrega generada: " + rutaPdfUrl
                        + " ; checklist de entrega: " + rutaChecklistPdfUrl);

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

    /** Sufijo aleatorio corto: evita colisiones de nombre entre actas iguales. */
    private static String sufijoUnico() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private Long parseLongNullable(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Long.valueOf(v.trim());
        } catch (NumberFormatException e) {
            // Defensa: el DTO ya valida \d{1,18}; si aun asi llega un valor no
            // numerico, se loguea en vez de tragarlo en silencio (QA-35).
            log.warn("numero_sac invalido '{}' — se persiste ticket_glpi como null", v);
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

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
