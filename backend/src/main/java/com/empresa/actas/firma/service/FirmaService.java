package com.empresa.actas.firma.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.EstadoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import com.empresa.actas.auditoria.entity.TipoEventoAuditoria;
import com.empresa.actas.auditoria.service.AuditoriaService;
import com.empresa.actas.firma.dto.EnviarActaResponse;
import com.empresa.actas.firma.dto.FirmaPublicaResponse;
import com.empresa.actas.firma.dto.FirmaRequest;
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.empresa.actas.firma.repository.FirmaTokenRepository;
import com.empresa.actas.mail.service.MailService;
import com.empresa.actas.security.AccesoService;
import com.empresa.actas.security.UserSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
    private final MailService mailService;
    private final AccesoService accesoService;
    private final AuditoriaService auditoriaService;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    @Value("${app.firma-url-base:}")
    private String firmaUrlBase;

    /** Vigencia del enlace de firma en horas (default 72h = 3 dias). */
    @Value("${app.firma-token-expira-horas:72}")
    private long firmaTokenExpiraHoras;

    @Transactional
    public EnviarActaResponse enviarActa(Long idActa, String correo) {
        UserSecurity userSecurity =
                (UserSecurity) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();

        Acta acta = actaRepository.findById(idActa)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Acta no encontrada con id: " + idActa));

        // ROL TECNICO: solo puede enviar a firma sus propias actas.
        accesoService.verificarAccesoActa(acta);

        if (acta.getEstado() != EstadoActa.GENERADA) {
            throw new IllegalArgumentException(
                    "Solo se pueden enviar actas en estado GENERADA. Estado actual: " + acta.getEstado());
        }

        String correoDetectadoGlpi = acta.getCorreoUsuario();

        if (correo != null && !correo.isBlank()) {
            acta.setCorreoUsuario(correo.trim());
        }

        String correoUtilizado = acta.getCorreoUsuario();

        String token = UUID.randomUUID().toString();

        FirmaToken firmaToken = FirmaToken.builder()
                .idActa(idActa)
                .token(token)
                .utilizado(false)
                .fechaExpiracion(calcularExpiracion())
                .build();
        firmaTokenRepository.save(firmaToken);

        EstadoActa estadoAnterior = acta.getEstado();
        acta.setEstado(EstadoActa.ENVIADA);
        acta.setFechaEnvio(LocalDateTime.now());
        actaRepository.save(acta);

        String urlFirma = construirUrlFirma(token);

        boolean correoEnviado = mailService.enviarCorreoFirma(
                correoUtilizado,
                acta.getNombreUsuario(),
                acta.getTipoActa() != null ? acta.getTipoActa().name() : null,
                acta.getSerialEquipo(),
                urlFirma);

        String observacion = "Acta enviada para firma"
                + "; correo_detectado_glpi=" + (correoDetectadoGlpi == null ? "" : correoDetectadoGlpi)
                + "; correo_utilizado=" + (correoUtilizado == null ? "" : correoUtilizado)
                + "; correo_enviado=" + (correoEnviado ? "SI" : "NO");

        actaHistorialService.registrarEvento(
                idActa,
                TipoEventoActa.ACTA_ENVIADA,
                estadoAnterior,
                EstadoActa.ENVIADA,
                userSecurity.getUsuario().getIdUsuario(),
                userSecurity.getUsername(),
                firmaToken.getIdToken(),
                observacion);

        return new EnviarActaResponse(token, urlFirma);
    }

    /**
     * Construye la URL publica del portal de firma a partir de la
     * configuracion {@code app.firma-url-base}. Nunca se usa localhost.
     */
    private String construirUrlFirma(String token) {
        if (firmaUrlBase == null || firmaUrlBase.isBlank()) {
            return "/firma.html?token=" + token;
        }
        String base = firmaUrlBase.endsWith("/")
                ? firmaUrlBase.substring(0, firmaUrlBase.length() - 1)
                : firmaUrlBase;
        return base + "/firma.html?token=" + token;
    }

    /**
     * Sirve el PDF del acta al firmante (portal publico, sin JWT).
     * Valida el token de firma: debe existir, no estar usado y el acta ENVIADA.
     * Devuelve null si el archivo no existe.
     */
    public Resource obtenerPdfPorToken(String token) {
        FirmaToken firmaToken = validarTokenFirma(token);

        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));

        if (acta.getEstado() != EstadoActa.ENVIADA) {
            throw new IllegalArgumentException(
                    "Esta acta no esta disponible para firma. Estado: " + acta.getEstado());
        }

        if (acta.getRutaPdf() == null || !acta.getRutaPdf().startsWith("uploads/")) {
            return null;
        }
        Path archivo = Paths.get(uploadsDir)
                .resolve(acta.getRutaPdf().substring("uploads/".length()));
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo)) {
            return null;
        }

        // Portal publico: el firmante visualiza el documento (acceso con token, sin usuario del sistema).
        auditoriaService.registrar(TipoEventoAuditoria.DOCUMENTO_VISTO, null, "PORTAL_FIRMA",
                "ACTA", String.valueOf(acta.getIdActa()), "/firma/" + token + "/pdf",
                "El firmante visualizo el PDF del acta");

        return new FileSystemResource(archivo.toFile());
    }

    @Transactional
    public FirmaPublicaResponse obtenerActaPorToken(String token) {
        FirmaToken firmaToken = validarTokenFirma(token);

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
        FirmaToken firmaToken = validarTokenFirma(token);

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
        FirmaToken firmaToken = validarTokenFirma(token);

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

    private LocalDateTime calcularExpiracion() {
        return LocalDateTime.now().plusHours(firmaTokenExpiraHoras);
    }

    /**
     * Valida el token de firma: debe existir, no estar usado y no vencido.
     * Antes de bloquear registra el evento correspondiente en la CAPA 2:
     *   - no existe / alterado                  -> TOKEN_INVALIDO
     *   - ya utilizado                          -> TOKEN_INVALIDO
     *   - vencido (fecha_expiracion pasada)     -> TOKEN_EXPIRADO
     */
    private FirmaToken validarTokenFirma(String token) {
        FirmaToken firmaToken = firmaTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    auditoriaService.registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null,
                            "PORTAL_FIRMA", "FIRMA_TOKEN", token, "/firma/" + token,
                            "Token inexistente o alterado");
                    return new IllegalArgumentException("Token no valido");
                });

        if (firmaToken.getUtilizado()) {
            auditoriaService.registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", token, "/firma/" + token,
                    "Token ya utilizado");
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }

        LocalDateTime expiracion = firmaToken.getFechaExpiracion();
        if (expiracion != null && expiracion.isBefore(LocalDateTime.now())) {
            auditoriaService.registrar(TipoEventoAuditoria.TOKEN_EXPIRADO, null,
                    "PORTAL_FIRMA", "FIRMA_TOKEN", token, "/firma/" + token,
                    "Token vencido (expiro el " + expiracion + ")");
            throw new IllegalArgumentException("Este enlace ha expirado, solicite uno nuevo");
        }

        return firmaToken;
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
