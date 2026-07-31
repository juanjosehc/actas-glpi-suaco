package com.empresa.actas.firma.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.ActaHistorial;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.repository.ActaHistorialRepository;
import com.empresa.actas.acta.repository.ActaRepository;
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
    private final ActaHistorialRepository actaHistorialRepository;
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

        actaHistorialRepository.save(ActaHistorial.builder()
                .idActa(idActa)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(EstadoActa.ENVIADA)
                .usuarioAccion(userSecurity.getUsername())
                .build());

        return new EnviarActaResponse(token, "/firma/" + token);
    }

    @Transactional(readOnly = true)
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

        actaHistorialRepository.save(ActaHistorial.builder()
                .idActa(acta.getIdActa())
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(EstadoActa.RECHAZADA)
                .usuarioAccion("SISTEMA")
                .observacion(motivo)
                .build());
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

            evidenciaRepository.save(Evidencia.builder()
                    .idActa(acta.getIdActa())
                    .tipo(Evidencia.TipoEvidencia.FOTO)
                    .rutaArchivo("uploads/fotos/foto_" + acta.getIdActa() + ".jpg")
                    .build());

            firmaToken.setUtilizado(true);
            firmaToken.setFechaUtilizacion(LocalDateTime.now());
            firmaTokenRepository.save(firmaToken);

            EstadoActa estadoAnterior = acta.getEstado();
            acta.setEstado(EstadoActa.FIRMADA);
            acta.setFechaFirma(LocalDateTime.now());
            actaRepository.save(acta);

            actaHistorialRepository.save(ActaHistorial.builder()
                    .idActa(acta.getIdActa())
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(EstadoActa.FIRMADA)
                    .usuarioAccion("SISTEMA")
                    .observacion("Firma digital registrada")
                    .build());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivos: " + e.getMessage());
        }
    }
}
