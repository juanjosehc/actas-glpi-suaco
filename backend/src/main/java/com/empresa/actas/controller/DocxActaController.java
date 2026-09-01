package com.empresa.actas.controller;

import com.empresa.actas.dto.request.ActaRequest;
import com.empresa.actas.dto.response.ActaResponse;
import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.service.DocxActaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final DocxActaService docxActaService;

    public DocxActaController(DocxActaService docxActaService) {
        this.docxActaService = docxActaService;
    }

    @PostMapping("/generar-acta")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ActaResponse generarActa(@Valid @RequestBody ActaRequest request) {
        return docxActaService.generarActa(request);
    }

    @GetMapping("/descargar-acta/{nombreZip}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO', 'AUDITOR')")
    public ResponseEntity<?> descargarActa(@PathVariable String nombreZip) {
        // Contencion de ruta: se sirve solo el nombre de archivo dentro de
        // generatedDir. getFileName() descarta segmentos (../, subcarpetas,
        // encodings) y el startsWith evita escapar de la base.
        Path baseDir = Paths.get(generatedDir).toAbsolutePath().normalize();
        String soloNombre = Paths.get(nombreZip).getFileName().toString();
        Path rutaZip = baseDir.resolve(soloNombre).normalize();

        if (!rutaZip.startsWith(baseDir)) {
            return ResponseEntity.badRequest().build();
        }

        if (!rutaZip.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(rutaZip.toFile());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + soloNombre + "\"")
                .body(resource);
    }

    }
