package com.empresa.actas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class LibreOfficePdfService {

    private static final Logger log = LoggerFactory.getLogger(LibreOfficePdfService.class);

    private static final Semaphore LIBRE_OFFICE_LOCK = new Semaphore(1);
    private static final int MAX_ATTEMPTS = 2;
    private static final long CONVERSION_TIMEOUT_SECONDS = 120;

    private final String libreOfficePath;

    public LibreOfficePdfService(@Value("${libreoffice.path}") String libreOfficePath) {
        this.libreOfficePath = libreOfficePath;
    }

    public String convertirDocxAPdf(Path docxPath, Path outputDir) throws IOException {
        // Preflight (QA-26): si la ruta de soffice no existe, el fallo de
        // ProcessBuilder seria un "Cannot run program" poco diagnostico. Se
        // detecta temprano con un mensaje claro para el usuario.
        if (libreOfficePath == null || libreOfficePath.isBlank()
                || !Files.exists(Path.of(libreOfficePath))) {
            throw new IOException("LibreOffice no encontrado en: '" + libreOfficePath
                    + "'. Verifique la propiedad libreoffice.path (soffice.exe).");
        }

        Files.createDirectories(outputDir);

        String pdfName = docxPath.getFileName().toString()
                .replaceAll("(?i)\\.docx$", ".pdf");
        Path pdfPath = outputDir.resolve(pdfName);

        Files.deleteIfExists(pdfPath);

        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Path profileDir = null;
            try {
                LIBRE_OFFICE_LOCK.acquire();
                try {
                    profileDir = Files.createTempDirectory("lo-profile-");
                    convertirUnaVez(docxPath, outputDir, profileDir, pdfPath);
                    log.info("PDF generado exitosamente (intento {}): {}", attempt, pdfPath);
                    return pdfName;
                } finally {
                    LIBRE_OFFICE_LOCK.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrumpido esperando turno de LibreOffice", e);
            } catch (IOException e) {
                lastError = e;
                log.warn("Intento {} de conversion LibreOffice fallo: {}", attempt, e.getMessage());
                Files.deleteIfExists(pdfPath);
            } finally {
                borrarDirectorio(profileDir);
            }
        }
        throw new IOException("LibreOffice fallo tras " + MAX_ATTEMPTS + " intentos: "
                + (lastError == null ? "desconocido" : lastError.getMessage()), lastError);
    }

    private void convertirUnaVez(Path docxPath, Path outputDir, Path profileDir, Path pdfPath)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(libreOfficePath);
        command.add("-env:UserInstallation=" + aFileUrl(profileDir));
        command.add("--headless");
        command.add("--convert-to");
        command.add("pdf");
        command.add("--outdir");
        command.add(outputDir.toAbsolutePath().toString());
        command.add(docxPath.toAbsolutePath().toString());

        log.info("Ejecutando LibreOffice con perfil aislado {}: {}", profileDir, command);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        byte[] salida = leerSalidaEnHilo(process.getInputStream());

        boolean finished = process.waitFor(CONVERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("LibreOffice no respondio en " + CONVERSION_TIMEOUT_SECONDS + " segundos");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("LibreOffice fallo con codigo " + exitCode + ": " + new String(salida));
        }

        if (!Files.exists(pdfPath) || Files.size(pdfPath) == 0) {
            throw new IOException("LibreOffice no genero el PDF: " + pdfPath);
        }
    }

    private static byte[] leerSalidaEnHilo(InputStream in) {
        AtomicReference<byte[]> referencia = new AtomicReference<>(new byte[0]);
        Thread lector = new Thread(() -> {
            try (InputStream is = in) {
                referencia.set(is.readAllBytes());
            } catch (IOException ignorada) {
            }
        });
        lector.setDaemon(true);
        lector.start();
        try {
            lector.join(TimeUnit.SECONDS.toMillis(CONVERSION_TIMEOUT_SECONDS) + 5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return referencia.get();
    }

    private static String aFileUrl(Path path) {
        return new File(path.toAbsolutePath().toString()).toURI().toString();
    }

    private static void borrarDirectorio(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var rutas = Files.walk(dir)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignorada) {
                }
            });
        } catch (IOException ignorada) {
        }
    }
}
