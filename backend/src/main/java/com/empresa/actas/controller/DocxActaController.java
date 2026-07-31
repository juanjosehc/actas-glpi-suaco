package com.empresa.actas.controller;

import com.empresa.actas.dto.request.ActaRequest;
import com.empresa.actas.dto.response.ActaResponse;
import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.service.DocxActaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class DocxActaController {

    @Value("${app.generated-dir}")
    private String generatedDir;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    private final DocxActaService docxActaService;

    public DocxActaController(DocxActaService docxActaService) {
        this.docxActaService = docxActaService;
    }

    @PostMapping("/generar-acta")
    public ActaResponse generarActa(@Valid @RequestBody ActaRequest request) {
        return docxActaService.generarActa(request);
    }

    @GetMapping("/descargar-acta/{nombreZip}")
    public ResponseEntity<?> descargarActa(@PathVariable String nombreZip) {
        Path rutaZip = Paths.get(generatedDir, nombreZip);

        if (!rutaZip.toFile().exists()) {
            return ResponseEntity.ok(ErrorResponse.of("Archivo no encontrado"));
        }

        Resource resource = new FileSystemResource(rutaZip.toFile());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreZip + "\"")
                .body(resource);
    }

    @GetMapping("/uploads/pdf/{fileName:.+}")
    public ResponseEntity<?> servirPdf(@PathVariable String fileName) {
        Path rutaPdf = Paths.get(uploadsDir, "pdf", fileName);

        if (!rutaPdf.toFile().exists()) {
            return ResponseEntity.ok(ErrorResponse.of("PDF no encontrado"));
        }

        Resource resource = new FileSystemResource(rutaPdf.toFile());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
