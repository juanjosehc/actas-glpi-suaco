package com.empresa.actas.controller;

import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.dto.request.ActaRequest;
import com.empresa.actas.dto.response.ActaResponse;
import com.empresa.actas.dto.response.ErrorResponse;
import com.empresa.actas.security.UserSecurity;
import com.empresa.actas.service.DocxActaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final ActaRepository actaRepository;

    public DocxActaController(DocxActaService docxActaService, ActaRepository actaRepository) {
        this.docxActaService = docxActaService;
        this.actaRepository = actaRepository;
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

        // SEC-106: el ZIP (DOCX del acta) solo lo descarga un ADMINISTRADOR o el
        // tecnico autor (acta.idTecnico == usuario autenticado). Sin esta
        // verificacion, cualquier TECNICO/AUDITOR podia descargar el ZIP de
        // cualquier acta (Broken Object Level Authorization).
        UserSecurity user = (UserSecurity) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        boolean esAdministrador = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        if (!esAdministrador) {
            boolean esAutor = actaRepository.findByRutaZip(soloNombre)
                    .filter(a -> a.getIdTecnico() != null
                            && a.getIdTecnico().equals(user.getUsuario().getIdUsuario()))
                    .isPresent();
            if (!esAutor) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
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
