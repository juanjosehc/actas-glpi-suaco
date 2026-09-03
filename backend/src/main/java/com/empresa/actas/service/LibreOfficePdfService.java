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
import java.nio.file.Paths;
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
    private final String storageRoot;

    public LibreOfficePdfService(
            @Value("${libreoffice.path}") String libreOfficePath,
            @Value("${storage.root:${user.dir}/storage}") String storageRoot) {
        this.libreOfficePath = libreOfficePath;
        this.storageRoot = storageRoot;
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

        // Fase 2: perfil reutilizable bajo storage.root (entraña menos arranque
        // de LibreOffice que un perfi temp nuevo por conversión). Si falla —
        // lock huerfano de un proceso muerto, perfil corrupto — se cae a un
        // perfil temporal limpio (misma poliitica de aislamiento que antes).
        Path perfilCompartido = perfilCompartido();

        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Path perfilTemporal = null;
            try {
                LIBRE_OFFICE_LOCK.acquire();
                try {
                    // 1) Perfil compartido, con auto-limpieza de lock huerfano.
                    if (perfilCompartido != null) {
                        try {
                            Files.createDirectories(perfilCompartido);
                            convertirUnaVez(docxPath, outputDir, perfilCompartido, pdfPath);
                            log.info("PDF generado exitosamente (intento {}, perfil compartido {}): {}",
                                    attempt, perfilCompartido, pdfPath);
                            return pdfName;
                        } catch (IOException e) {
                            // Un .lock huerfano (proceso anterior muerto) hace que
                            // soffice rechace el perfil: se limpia y se reintenta.
                            // Bajo el semáforo nadie mas lo usa; un lock ahi solo
                            // puede ser residuo de un proceso ya terminado.
                            Files.deleteIfExists(pdfPath);
                            limpiarLockHuerfano(perfilCompartido);
                            try {
                                convertirUnaVez(docxPath, outputDir, perfilCompartido, pdfPath);
                                log.info("PDF generado exitosamente (intento {}, perfil compartido tras limpiar lock): {}",
                                        attempt, pdfPath);
                                return pdfName;
                            } catch (IOException e2) {
                                throw new IOException("Perfil compartido fallo: " + e2.getMessage(), e2);
                            }
                        }
                    }

                    // 2) Perfil temporal aislado (fallback del compartido).
                    perfilTemporal = Files.createTempDirectory("lo-profile-");
                    convertirUnaVez(docxPath, outputDir, perfilTemporal, pdfPath);
                    log.info("PDF generado exitosamente (intento {}, perfil temporal {}): {}",
                            attempt, perfilTemporal, pdfPath);
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
                borrarDirectorio(perfilTemporal);
            }
        }
        throw new IOException("LibreOffice fallo tras " + MAX_ATTEMPTS + " intentos: "
                + (lastError == null ? "desconocido" : lastError.getMessage()), lastError);
    }

    /** Perfil LibreOffice persistente: {@code storage.root/lo-profile}. null si el storage no es escribible. */
    private Path perfilCompartido() {
        try {
            if (storageRoot == null || storageRoot.isBlank()) {
                return null;
            }
            Path perfil = Paths.get(storageRoot).resolve("lo-profile").toAbsolutePath().normalize();
            Files.createDirectories(perfil);
            return perfil;
        } catch (IOException e) {
            log.warn("No se puede usar perfil LibreOffice compartido en {}: {}. Se usara perfil temporal.",
                    storageRoot, e.getMessage());
            return null;
        }
    }

    /** Elimina un {@code .lock} huerfano dentro del perfil (residuo de un soffice muerto). */
    private static void limpiarLockHuerfano(Path perfil) {
        Path lock = perfil.resolve(".lock");
        if (Files.exists(lock)) {
            borrarDirectorio(lock);
        }
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
