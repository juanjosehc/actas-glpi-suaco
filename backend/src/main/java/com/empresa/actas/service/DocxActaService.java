package com.empresa.actas.service;

import com.empresa.actas.dto.request.ActaRequest;
import com.empresa.actas.dto.response.ActaResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public DocxActaService(
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

    public ActaResponse generarActa(ActaRequest request) {
        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Map<String, Object> datos = objectMapper.convertValue(
                    request,
                    new TypeReference<Map<String, Object>>() {}
            );

            Path rutaActa = wordService.generarActa(datos);

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

            return ActaResponse.ok(nombreZip, rutaPdfUrl);

        } catch (Exception e) {
            return ActaResponse.error("Error generando documentacion: " + e.getMessage());
        }
    }
}
