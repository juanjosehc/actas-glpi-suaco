package com.empresa.actas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class LibreOfficePdfService {

    private static final Logger log = LoggerFactory.getLogger(LibreOfficePdfService.class);

    private final String libreOfficePath;

    public LibreOfficePdfService(@Value("${libreoffice.path}") String libreOfficePath) {
        this.libreOfficePath = libreOfficePath;
    }

    public String convertirDocxAPdf(Path docxPath, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        String pdfName = docxPath.getFileName().toString()
                .replaceAll("(?i)\\.docx$", ".pdf");
        Path pdfPath = outputDir.resolve(pdfName);

        ProcessBuilder pb = new ProcessBuilder(
                libreOfficePath,
                "--headless",
                "--convert-to", "pdf",
                "--outdir", outputDir.toAbsolutePath().toString(),
                docxPath.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        log.info("Ejecutando LibreOffice: {} --headless --convert-to pdf --outdir {} {}",
                libreOfficePath, outputDir, docxPath);

        Process process = pb.start();

        try {
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("LibreOffice no respondio en 120 segundos");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Proceso LibreOffice interrumpido", e);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String error = new String(process.getInputStream().readAllBytes());
            throw new IOException("LibreOffice fallo con codigo " + exitCode + ": " + error);
        }

        if (!Files.exists(pdfPath)) {
            throw new IOException("LibreOffice no genero el PDF: " + pdfPath);
        }

        log.info("PDF generado exitosamente: {}", pdfPath);
        return pdfName;
    }
}
