package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import com.empresa.actas.dto.request.DevolucionRequest;
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
 * Servicio orquestador para la generación del acta de devolución.
 *
 * Flujo:
 * 1. Crear directorio de salida si no existe.
 * 2. Convertir DevolucionRequest a Map<String, Object> para el motor de templates.
 * 3. Generar acta de devolución (DOCX) vía DocumentoWordService.
 * 4. Empaquetar el DOCX en un ZIP vía ZipService.
 * 5. Generar PDF desde el DOCX generado vía DocxToPdfService.
 * 6. Retornar ActaResponse con nombre del ZIP y ruta del PDF.
 *
 * A diferencia de ActaService, solo genera un DOCX (no checklist).
 * Naming del ZIP: Devolucion_{serial}_{motivo}.zip
 */
@Service
public class DevolucionService {

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

    public DevolucionService(
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
     * Genera el acta de devolución empaquetada en ZIP,
     * junto con el PDF desde la plantilla DOCX.
     *
     * @param request Datos del acta validados previamente por el controller.
     * @return ActaResponse con success=true, nombre_zip y ruta_pdf, o success=false con error.
     */
    @Transactional
    public ActaResponse generarDevolucion(DevolucionRequest request) {
        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Map<String, Object> datos = objectMapper.convertValue(
                    request,
                    new TypeReference<Map<String, Object>>() {}
            );

            Path rutaDevolucion = wordService.generarDevolucion(datos);

            // Firma permanente del tecnico: se inserta en el DOCX antes de
            // empaquetar/convertir. Si no tiene firma, el placeholder queda en blanco.
            // La firma/foto del USUARIO aun no existe: se dejan en blanco para que
            // el DOCX/PDF inicial no muestre {{firma_usuario}} / {{foto_usuario}} crudos.
            byte[] firmaTecnico = usuarioService.obtenerFirmaBytesDe(tecnicoAutenticado());
            DocxImageReplacer.reemplazarFirmaTecnico(rutaDevolucion.toString(), firmaTecnico);
            DocxImageReplacer.reemplazarFirmaYFoto(rutaDevolucion.toString(), null, null);

            String serial = "SinSerial";
            if (request.getEquipos() != null && !request.getEquipos().isEmpty()) {
                serial = NombreArchivoSeguro.segmento(request.getEquipos().get(0).getSerial());
            }

            String motivo = NombreArchivoSeguro.segmento(request.getMotivo());

            String nombreZip = "Devolucion_" + serial + "_" + motivo + "_" + sufijoUnico() + ".zip";
            Path rutaZip = outputDir.resolve(nombreZip);

            zipService.crearZip(rutaZip, rutaDevolucion);

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileName = libreOfficePdfService.convertirDocxAPdf(rutaDevolucion, pdfDir);
            String rutaPdfUrl = "uploads/pdf/" + pdfFileName;

            Long idActa = persistirActa(request, rutaPdfUrl);

            return ActaResponse.ok(nombreZip, rutaPdfUrl, idActa);

        } catch (Exception e) {
            return ActaResponse.error("Error generando devolucion: " + e.getMessage());
        }
    }

    /**
     * Persiste la entidad Acta de devolucion en el mismo flujo que la
     * generacion de documentos, de forma atomica (si el PDF se genera la
     * acta queda registrada; ya no depende de una llamada /actas aparte).
     */
    private Long persistirActa(DevolucionRequest request, String rutaPdfUrl) {
        Long idTecnico = tecnicoAutenticado();

        Acta acta = Acta.builder()
                .idTecnico(idTecnico)
                .ticketGlpi(null)
                .tipoActa(TipoActa.DEVOLUCION)
                .estado(EstadoActa.GENERADA)
                // Regla de negocio Devolucion: Usuario = quien ENTREGA el equipo.
                // El tecnico (quien recibe la devolucion) queda en idTecnico (JWT).
                .cedulaUsuario(request.getCedula())
                .nombreUsuario(request.getEntregado_por())
                .correoUsuario(blankToNull(request.getCorreo()))
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
                "Acta de devolucion generada: " + rutaPdfUrl);

        return guardada.getIdActa();
    }

    /** Sufijo aleatorio corto: evita colisiones de nombre entre actas iguales. */
    private static String sufijoUnico() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
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

    private String primerSerial(DevolucionRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty() || r.getEquipos().get(0).getSerial() == null) return null;
        return r.getEquipos().get(0).getSerial();
    }

    private String primerInventario(DevolucionRequest r) {
        if (r.getEquipos() == null || r.getEquipos().isEmpty()) return null;
        var eq = r.getEquipos().get(0);
        return eq.getInventario() != null ? eq.getInventario() : null;
    }

    private String descripcionEquipo(DevolucionRequest r) {
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

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
