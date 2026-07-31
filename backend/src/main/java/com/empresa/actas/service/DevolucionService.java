package com.empresa.actas.service;

import com.empresa.actas.dto.request.DevolucionRequest;
import com.empresa.actas.dto.response.ActaResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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

    public DevolucionService(
            DocumentoWordService wordService,
            ZipService zipService,
            ObjectMapper objectMapper,
            LibreOfficePdfService libreOfficePdfService
    ) {
        this.wordService = wordService;
        this.zipService = zipService;
        this.objectMapper = objectMapper;
        this.libreOfficePdfService = libreOfficePdfService;
    }

    /**
     * Genera el acta de devolución empaquetada en ZIP,
     * junto con el PDF desde la plantilla DOCX.
     *
     * @param request Datos del acta validados previamente por el controller.
     * @return ActaResponse con success=true, nombre_zip y ruta_pdf, o success=false con error.
     */
    public ActaResponse generarDevolucion(DevolucionRequest request) {
        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Map<String, Object> datos = objectMapper.convertValue(
                    request,
                    new TypeReference<Map<String, Object>>() {}
            );

            Path rutaDevolucion = wordService.generarDevolucion(datos);

            String serial = "SinSerial";
            if (request.getEquipos() != null && !request.getEquipos().isEmpty()) {
                serial = request.getEquipos().get(0).getSerial();
            }

            String motivo = request.getMotivo()
                    .replaceAll("[^a-zA-Z0-9]", "");

            String nombreZip = "Devolucion_" + serial + "_" + motivo + ".zip";
            Path rutaZip = outputDir.resolve(nombreZip);

            zipService.crearZip(rutaZip, rutaDevolucion);

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileName = libreOfficePdfService.convertirDocxAPdf(rutaDevolucion, pdfDir);
            String rutaPdfUrl = "uploads/pdf/" + pdfFileName;

            return ActaResponse.ok(nombreZip, rutaPdfUrl);

        } catch (Exception e) {
            return ActaResponse.error("Error generando devolucion: " + e.getMessage());
        }
    }
}
