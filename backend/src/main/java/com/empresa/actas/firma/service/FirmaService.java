package com.empresa.actas.firma.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import com.empresa.actas.firma.dto.EnviarActaResponse;
import com.empresa.actas.firma.dto.FirmaPublicaResponse;
import com.empresa.actas.firma.dto.FirmaRequest;
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.empresa.actas.firma.repository.FirmaTokenRepository;
import com.empresa.actas.security.UserSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FirmaService {

    private final ActaRepository actaRepository;
    private final ActaHistorialService actaHistorialService;
    private final FirmaTokenRepository firmaTokenRepository;
    private final EvidenciaRepository evidenciaRepository;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    @Transactional
    public EnviarActaResponse enviarActa(Long idActa) {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();

        Acta acta = actaRepository.findById(idActa)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + idActa));

        if (acta.getEstado() != EstadoActa.GENERADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden enviar actas en estado GENERADA. Estado actual: " + acta.getEstado());
        }

        String token = UUID.randomUUID().toString();

        FirmaToken firmaToken = FirmaToken.builder()
                .idActa(idActa)
                .token(token)
                .utilizado(false)
                .build();
        firmaTokenRepository.save(firmaToken);

        EstadoActa estadoAnterior = acta.getEstado();
        acta.setEstado(EstadoActa.ENVIADA);
        acta.setFechaEnvio(LocalDateTime.now());
        actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                idActa,
                TipoEventoActa.ACTA_ENVIADA,
                estadoAnterior,
                EstadoActa.ENVIADA,
                userSecurity.getUsuario().getIdUsuario(),
                userSecurity.getUsername(),
                firmaToken.getIdToken(),
                "Acta enviada para firma");

        return new EnviarActaResponse(token, "/firma/" + token);
    }

    @Transactional
    public FirmaPublicaResponse obtenerActaPorToken(String token) {
        FirmaToken firmaToken = firmaTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Token no valido"));

        if (firmaToken.getUtilizado()) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }

        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada"));

        if (acta.getEstado() != EstadoActa.ENVIADA) {
            throw new IllegalArgumentException(
                    "Esta acta no esta disponible para firma. Estado: " + acta.getEstado());
        }

        actaHistorialService.registrarEvento(
                acta.getIdActa(),
                TipoEventoActa.ACTA_ABIERTA_USUARIO,
                null,
                acta.getEstado(),
                null,
                identidadFirmante(acta),
                firmaToken.getIdToken(),
                "El usuario abrio el enlace de firma");

        return FirmaPublicaResponse.builder()
                .idActa(acta.getIdActa())
                .tipoActa(acta.getTipoActa().name())
                .estado(acta.getEstado().name())
                .rutaPdf(acta.getRutaPdf())
                .nombreUsuario(acta.getNombreUsuario())
                .cedulaUsuario(acta.getCedulaUsuario())
                .correoUsuario(acta.getCorreoUsuario())
                .descripcionEquipo(acta.getDescripcionEquipo())
                .serialEquipo(acta.getSerialEquipo())
                .placaEquipo(acta.getPlacaEquipo())
                .ticketGlpi(acta.getTicketGlpi())
                .contenidoHtml(acta.getContenidoHtml())
                .fechaRechazo(acta.getFechaRechazo())
                .observacionRechazo(acta.getObservacionRechazo())
                .build();
    }

    @Transactional
    public void rechazarActa(String token, String motivo) {
        FirmaToken firmaToken = firmaTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Token no valido"));

        if (firmaToken.getUtilizado()) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }

        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada"));

        if (acta.getEstado() != EstadoActa.ENVIADA) {
            throw new IllegalArgumentException(
                    "Esta acta no esta disponible para firma. Estado: " + acta.getEstado());
        }

        firmaToken.setUtilizado(true);
        firmaToken.setFechaUtilizacion(LocalDateTime.now());
        firmaTokenRepository.save(firmaToken);

        EstadoActa estadoAnterior = acta.getEstado();
        acta.setEstado(EstadoActa.RECHAZADA);
        acta.setFechaRechazo(LocalDateTime.now());
        acta.setObservacionRechazo(motivo);
        actaRepository.save(acta);

        actaHistorialService.registrarEvento(
                acta.getIdActa(),
                TipoEventoActa.ACTA_RECHAZADA_USUARIO,
                estadoAnterior,
                EstadoActa.RECHAZADA,
                null,
                identidadFirmante(acta),
                firmaToken.getIdToken(),
                motivo);
    }

    @Transactional
    public void firmarActa(String token, FirmaRequest request) {
        FirmaToken firmaToken = firmaTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Token no valido"));

        if (firmaToken.getUtilizado()) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }

        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada"));

        if (acta.getEstado() != EstadoActa.ENVIADA) {
            throw new IllegalArgumentException(
                    "Esta acta no esta disponible para firma. Estado: " + acta.getEstado());
        }

        try {
            Path directorioFirmas = Paths.get(uploadsDir, "firmas");
            Path directorioFotos = Paths.get(uploadsDir, "fotos");
            Files.createDirectories(directorioFirmas);
            Files.createDirectories(directorioFotos);

            byte[] firmaBytes = Base64.getDecoder().decode(request.firmaBase64());
            Path rutaFirma = directorioFirmas.resolve("firma_" + acta.getIdActa() + ".png");
            Files.write(rutaFirma, firmaBytes);

            byte[] fotoBytes = Base64.getDecoder().decode(request.fotoBase64());
            Path rutaFoto = directorioFotos.resolve("foto_" + acta.getIdActa() + ".jpg");
            Files.write(rutaFoto, fotoBytes);

            evidenciaRepository.save(Evidencia.builder()
                    .idActa(acta.getIdActa())
                    .tipo(Evidencia.TipoEvidencia.FIRMA)
                    .rutaArchivo("uploads/firmas/firma_" + acta.getIdActa() + ".png")
                    .build());

            actaHistorialService.registrarEvento(
                    acta.getIdActa(),
                    TipoEventoActa.EVIDENCIA_CARGADA,
                    null,
                    acta.getEstado(),
                    null,
                    "SISTEMA",
                    firmaToken.getIdToken(),
                    "Tipo: FIRMA - uploads/firmas/firma_" + acta.getIdActa() + ".png");

            evidenciaRepository.save(Evidencia.builder()
                    .idActa(acta.getIdActa())
                    .tipo(Evidencia.TipoEvidencia.FOTO)
                    .rutaArchivo("uploads/fotos/foto_" + acta.getIdActa() + ".jpg")
                    .build());

            actaHistorialService.registrarEvento(
                    acta.getIdActa(),
                    TipoEventoActa.EVIDENCIA_CARGADA,
                    null,
                    acta.getEstado(),
                    null,
                    "SISTEMA",
                    firmaToken.getIdToken(),
                    "Tipo: FOTO - uploads/fotos/foto_" + acta.getIdActa() + ".jpg");

            firmaToken.setUtilizado(true);
            firmaToken.setFechaUtilizacion(LocalDateTime.now());
            firmaTokenRepository.save(firmaToken);

            EstadoActa estadoAnterior = acta.getEstado();
            acta.setEstado(EstadoActa.FIRMADA);
            acta.setFechaFirma(LocalDateTime.now());
            actaRepository.save(acta);

            actaHistorialService.registrarEvento(
                    acta.getIdActa(),
                    TipoEventoActa.ACTA_FIRMADA,
                    estadoAnterior,
                    EstadoActa.FIRMADA,
                    null,
                    identidadFirmante(acta),
                    firmaToken.getIdToken(),
                    "Firma digital registrada");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivos: " + e.getMessage());
        }
    }

    private String identidadFirmante(Acta acta) {
        String nombre = acta.getNombreUsuario();
        String cedula = acta.getCedulaUsuario();
        StringBuilder identidad = new StringBuilder();
        if (nombre != null && !nombre.isBlank()) {
            identidad.append(nombre);
        }
        if (cedula != null && !cedula.isBlank()) {
            identidad.append(" (CC ").append(cedula).append(")");
        }
        return identidad.length() > 0 ? identidad.toString() : "USUARIO_FIRMANTE";
    }
}
