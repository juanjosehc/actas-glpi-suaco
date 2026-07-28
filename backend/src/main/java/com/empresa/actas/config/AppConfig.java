package com.empresa.actas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
public class AppConfig {

    @Value("${app.generated-dir}")
    private String generatedDir;

    @PostConstruct
    public void init() {
        File dir = new File(generatedDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
