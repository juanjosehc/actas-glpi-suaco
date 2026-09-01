package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import com.empresa.actas.dto.request.FormateoSeguroRequest;
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

/**
 * Servicio orquestador para la generación del acta de formateo seguro.
 *
 * Flujo:
 * 1. Crear directorio de salida si no existe.
 * 2. Convertir FormateoSeguroRequest a Map<String, Object> para el motor de templates.
 * 3. Generar acta de formateo (DOCX) vía DocumentoWordService.
 * 4. Empaquetar el DOCX en un ZIP vía ZipService.
 * 5. Generar PDF desde el DOCX generado vía LibreOfficePdfService.
 * 6. Persistir la entidad Acta y retornar ActaResponse con nombre del ZIP y ruta del PDF.
 *
 * Naming del ZIP: FormateoSeguro_{serial}_{asunto}.zip
 */
@Service
public class FormateoSeguroService {

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

    public FormateoSeguroService(
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

    /**
     * Genera el acta de formateo seguro empaquetada en ZIP,
     * junto con el PDF desde la plantilla DOCX.
     *
     * @param request Datos del acta validados previamente por el controller.
     * @return ActaResponse con success=true, nombre_zip y ruta_pdf, o success=false con error.
     */
    @Transactional
    public ActaResponse generarFormateoSeguro(FormateoSeguroRequest request) {
        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Map<String, Object> datos = objectMapper.convertValue(
                    request,
                    new TypeReference<Map<String, Object>>() {}
            );

            Path rutaActa = wordService.generarFormateoSeguro(datos);

            // Firma permanente del tecnico: se inserta en el DOCX antes de
            // empaquetar/convertir (patron DocxActaService/DevolucionService).
            // Si no tiene firma, el placeholder queda en blanco.
            byte[] firmaTecnico = usuarioService.obtenerFirmaBytesDe(tecnicoAutenticado());
            DocxImageReplacer.reemplazarFirmaTecnico(rutaActa.toString(), firmaTecnico);

            String asunto = request.getAsunto()
                    .replaceAll("[^a-zA-Z0-9]", "");

            String serial = "SinSerial";
            if (request.getEquipos() != null && !request.getEquipos().isEmpty()) {
                serial = request.getEquipos().get(0).getSerial();
            }

            String nombreZip = "FormateoSeguro_" + serial + "_" + asunto + ".zip";
            Path rutaZip = outputDir.resolve(nombreZip);

            zipService.crearZip(rutaZip, rutaActa);

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileName = libreOfficePdfService.convertirDocxAPdf(rutaActa, pdfDir);
            String rutaPdfUrl = "uploads/pdf/" + pdfFileName;

            Long idActa = persistirActa(request, rutaPdfUrl);

            return ActaResponse.ok(nombreZip, rutaPdfUrl, idActa);

        } catch (Exception e) {
            return ActaResponse.error("Error generando formateo: " + e.getMessage());
        }
    }

    /**
     * Persiste la entidad Acta de formateo seguro en el mismo flujo que la
     * generacion de documentos, de forma atomica (si el PDF se genera la
     * acta queda registrada; ya no depende de una llamada /actas aparte).
     */
    private Long persistirActa(FormateoSeguroRequest request, String rutaPdfUrl) {
        Long idTecnico = tecnicoAutenticado();

        Acta acta = Acta.builder()
                .idTecnico(idTecnico)
                .ticketGlpi(null)
                .tipoActa(TipoActa.FORMATEO)
                .estado(EstadoActa.GENERADA)
                .cedulaUsuario(null)
                // Usuario principal = ENTREGADO A (dueño del equipo). El
                // tecnico (ENTREGADO POR) queda en idTecnico.
                .nombreUsuario(request.getEntregado_a())
                .correoUsuario(null)
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
                "Acta de formateo seguro generada: " + rutaPdfUrl);

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

    private String primerSerial(FormateoSeguroRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty() || r.getEquipos().get(0).getSerial() == null) return null;
        return r.getEquipos().get(0).getSerial();
    }

    private String primerInventario(FormateoSeguroRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty()) return null;
        var eq = r.getEquipos().get(0);
        return eq.getInventario() != null ? eq.getInventario() : null;
    }

    private String descripcionEquipo(FormateoSeguroRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty()) return null;
        var eq = r.getEquipos().get(0);
        return String.join(" ", eq.getMarca(), eq.getModelo()).trim();
    }

    private String serializar(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }
}