package com.empresa.actas.config;

import com.empresa.actas.rol.entity.Rol;
import com.empresa.actas.rol.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        List<String> roles = List.of("ADMINISTRADOR", "TECNICO", "AUDITOR");
        roles.forEach(nombre -> {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(Rol.builder().nombre(nombre).build());
                log.info("Rol creado: {}", nombre);
            }
        });
    }
}
