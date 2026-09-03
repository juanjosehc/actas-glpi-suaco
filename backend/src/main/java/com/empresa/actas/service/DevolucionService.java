package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.dto.request.DevolucionRequest;
import com.empresa.actas.dto.response.ActaResponse;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.usuario.service.UsuarioService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    private final GeneracionDocumentalAsyncService generacionDocumentalAsyncService;
    private final UsuarioService usuarioService;

    public DevolucionService(
            DocumentoWordService wordService,
            ZipService zipService,
            ObjectMapper objectMapper,
            LibreOfficePdfService libreOfficePdfService,
            ActaRepository actaRepository,
            GeneracionDocumentalAsyncService generacionDocumentalAsyncService,
            UsuarioService usuarioService
    ) {
        this.wordService = wordService;
        this.zipService = zipService;
        this.objectMapper = objectMapper;
        this.libreOfficePdfService = libreOfficePdfService;
        this.actaRepository = actaRepository;
        this.generacionDocumentalAsyncService = generacionDocumentalAsyncService;
        this.usuarioService = usuarioService;
    }

    /**
     * Flujo asincrono (request thread): persiste el acta en
     * GENERANDO_DOCUMENTOS y encola la generacion real; el POST responde en
     * milisegundos. El hilo de {@link GeneracionDocumentalAsyncService} cierra
     * el ciclo con GENERADA (o GENERACION_FALLIDA).
     *
     * @param request Datos del acta validados previamente por el controller.
     * @return ActaResponse con success=true y id_acta, o success=false con error.
     */
    public ActaResponse generarDevolucion(DevolucionRequest request) {
        try {
            // Capturar SOLO aqui (SecurityContext presente): la tarea async no
            // tiene SecurityContext para leer el usuario autenticado.
            Long idTecnico = tecnicoAutenticado();
            if (idTecnico == null) {
                return ActaResponse.error("No se pudo identificar el tecnico autenticado.");
            }
            byte[] firmaTecnico = usuarioService.obtenerFirmaBytesDe(idTecnico);

            Map<String, Object> datos = objectMapper.convertValue(
                    request,
                    new TypeReference<Map<String, Object>>() {}
            );

            String serial = "SinSerial";
            if (request.getEquipos() != null && !request.getEquipos().isEmpty()) {
                serial = NombreArchivoSeguro.segmento(request.getEquipos().get(0).getSerial());
            }
            String motivo = NombreArchivoSeguro.segmento(request.getMotivo());
            String nombreZip = "Devolucion_" + serial + "_" + motivo + "_" + sufijoUnico() + ".zip";

            Long idActa = persistirActaEnProceso(request, idTecnico, nombreZip);

            generacionDocumentalAsyncService.encolar(() ->
                    generarDevolucionEnSegundoPlano(idActa, datos, firmaTecnico, nombreZip));

            return ActaResponse.procesando(idActa);

        } catch (Exception e) {
            return ActaResponse.error("Error generando devolucion: " + e.getMessage());
        }
    }

    /** Tarea async: DOCX de devolucion -> ZIP -> PDF (misma logica, otro hilo). */
    private void generarDevolucionEnSegundoPlano(Long idActa, Map<String, Object> datos,
                                                 byte[] firmaTecnico, String nombreZip) {
        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Path rutaDevolucion = wordService.generarDevolucion(datos);
            DocxImageReplacer.reemplazarFirmaTecnico(rutaDevolucion.toString(), firmaTecnico);
            DocxImageReplacer.reemplazarFirmaYFoto(rutaDevolucion.toString(), null, null);

            Path rutaZip = outputDir.resolve(nombreZip);
            zipService.crearZip(rutaZip, rutaDevolucion);

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileName = libreOfficePdfService.convertirDocxAPdf(rutaDevolucion, pdfDir);
            String rutaPdfUrl = "uploads/pdf/" + pdfFileName;

            generacionDocumentalAsyncService.marcarGenerada(idActa, rutaPdfUrl, null, nombreZip);
        } catch (Exception e) {
            generacionDocumentalAsyncService.marcarFallida(idActa, e.getMessage());
        }
    }

    /** Re-encola la generacion desde datosOriginales (reintento / reinicio JVM). */
    public void reintentarGeneracion(Long idActa) {
        Acta acta = actaRepository.findById(idActa)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada con id: " + idActa));
        if (acta.getEstado() != EstadoActa.GENERACION_FALLIDA
                && acta.getEstado() != EstadoActa.GENERANDO_DOCUMENTOS) {
            throw new IllegalArgumentException("Solo se puede reintentar una acta en GENERACION_FALLIDA"
                    + " o GENERANDO_DOCUMENTOS. Estado actual: " + acta.getEstado());
        }
        if (acta.getDatosOriginales() == null || acta.getDatosOriginales().isBlank()) {
            throw new IllegalArgumentException("La acta no tiene datosOriginales: no se puede regenerar.");
        }

        byte[] firmaTecnico = usuarioService.obtenerFirmaBytesDe(acta.getIdTecnico());
        String nombreZip = acta.getRutaZip() != null
                ? acta.getRutaZip()
                : "Devolucion_" + sufijoUnico() + ".zip";
        Map<String, Object> datos;
        try {
            datos = objectMapper.readValue(acta.getDatosOriginales(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("datosOriginales invalidos: " + e.getMessage(), e);
        }

        acta.setEstado(EstadoActa.GENERANDO_DOCUMENTOS);
        actaRepository.save(acta);

        generacionDocumentalAsyncService.encolar(() ->
                generarDevolucionEnSegundoPlano(idActa, datos, firmaTecnico, nombreZip));
    }

    /** Persiste el acta en GENERANDO_DOCUMENTOS con el nombre del ZIP ya decidido. */
    private Long persistirActaEnProceso(DevolucionRequest request, Long idTecnico, String nombreZip) {
        Acta acta = Acta.builder()
                .idTecnico(idTecnico)
                .ticketGlpi(null)
                .tipoActa(TipoActa.DEVOLUCION)
                .estado(EstadoActa.GENERANDO_DOCUMENTOS)
                // Regla de negocio Devolucion: Usuario = quien ENTREGA el equipo.
                // El tecnico (quien recibe la devolucion) queda en idTecnico (JWT).
                .cedulaUsuario(request.getCedula())
                .nombreUsuario(request.getEntregado_por())
                .correoUsuario(blankToNull(request.getCorreo()))
                .serialEquipo(primerSerial(request))
                .placaEquipo(primerInventario(request))
                .descripcionEquipo(descripcionEquipo(request))
                .contenidoHtml(null)
                .rutaZip(nombreZip)
                .datosOriginales(serializar(request))
                .build();

        return actaRepository.save(acta).getIdActa();
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
