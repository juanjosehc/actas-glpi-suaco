package com.empresa.actas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class AppConfig {

    @Value("${app.generated-dir}")
    private String generatedDir;

    @Value("${app.uploads-dir}")
    private String uploadsDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(generatedDir));
            Files.createDirectories(Path.of(uploadsDir, "pdf"));
            Files.createDirectories(Path.of(uploadsDir, "firmas"));
            Files.createDirectories(Path.of(uploadsDir, "fotos"));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el almacenamiento de archivos", e);
        }
    }
}
