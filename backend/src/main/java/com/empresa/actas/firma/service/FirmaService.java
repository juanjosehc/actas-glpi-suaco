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
import com.empresa.actas.firma.support.FirmaUrlBuilder;
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
    private final AccesoService accesoService;
    private final AuditoriaService auditoriaService;
    private final OtpService otpService;
    private final TokenFirmaValidador validadorToken;
    private final FirmaUrlBuilder firmaUrlBuilder;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    /** Vigencia del enlace de firma en horas (default 72h = 3 dias). */
    @Value("${app.firma-token-expira-horas:72}")
    private long firmaTokenExpiraHoras;

    // ======================= SEC-008: validacion de evidencias =======================
    // El portal recibe firma (PNG) y foto (JPG) en Base64 dentro del JSON. Antes se
    // decodificaba y escribia directo: sin limite de tamaño, sin firma de formato,
    // base64 invalido terminaba en 500. Se valida tamaño (bytes decodificados) y
    // magic bytes del formato declarado antes de persistir. El error se lanza como
    // IllegalArgumentException: el GlobalExceptionHandler la traduce a 400.

    /** Tamaño máximo decodificado de la firma (PNG). */
    static final long TAMANO_MAX_FIRMA = 2L * 1024 * 1024;   // 2 MiB
    /** Tamaño máximo decodificado de la foto (JPG). */
    static final long TAMANO_MAX_FOTO = 5L * 1024 * 1024;    // 5 MiB

    /** Firma PNG: 89 50 4E 47 0D 0A 1A 0A */
    static final byte[] MAGIC_PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /** Foto JPEG: FF D8 FF */
    static final byte[] MAGIC_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /**
     * SEC-008: decodifica y valida una evidencia Base64 (formato + tamaño).
     *
     * @param base64  contenido Base64 (acepta el prefijo data-URL si viene).
     * @param maxSize límite de bytes decodificados.
     * @param magic   firma de bytes que debe iniciar el archivo.
     * @param campo   nombre legible del campo para el mensaje de error.
     * @return bytes validados listos para persistir.
     */
    static byte[] decodificarEvidenciaValida(String base64, long maxSize, byte[] magic, String campo) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatoria");
        }
        // Tolerancia: si el cliente manda un data-URL (data:image/png;base64,...),
        // se quita el prefijo antes de decodificar.
        int comaDataUrl = base64.indexOf(',');
        String datos = comaDataUrl >= 0 ? base64.substring(comaDataUrl + 1) : base64;

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(datos);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    campo + " no es Base64 valida");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException(campo + " vacia");
        }
        if (bytes.length > maxSize) {
            throw new IllegalArgumentException(
                    campo + " excede el tamanio maximo permitido (" + maxSize / (1024 * 1024) + " MiB)");
        }
        if (!empiezaCon(bytes, magic)) {
            throw new IllegalArgumentException(
                    campo + " no corresponde al formato esperado");
        }
        return bytes;
    }

    /** true si {@code datos} empieza con el prefijo {@code prefijo}. */
    static boolean empiezaCon(byte[] datos, byte[] prefijo) {
        if (datos == null || datos.length < prefijo.length) {
            return false;
        }
        for (int i = 0; i < prefijo.length; i++) {
            if (datos[i] != prefijo[i]) {
                return false;
            }
        }
        return true;
    }

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

        String urlFirma = firmaUrlBuilder.construir(token);

        // Correo unico: enlace + codigo OTP + vigencia (solicitud de firma + segunda capa de seguridad).
        boolean correoEnviado = otpService.generarYEnviarParaToken(firmaToken);

        // Si el correo no salio, NO marcar la acta como enviada: el flujo falla con error
        // visible (rollback de este @Transactional: token, estado y historial se revierten)
        // y el frontend no puede cerrar el modal como si hubiera sido un exito.
        if (!correoEnviado) {
            throw new IllegalArgumentException(
                    "No se pudo enviar el correo de firma a " + correoUtilizado
                            + ". Verifique el destinatario y el servicio SMTP, e intente de nuevo.");
        }

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
     * Sirve el PDF del acta al firmante (portal publico, sin JWT).
     * Valida el token de firma: debe existir, no estar usado y el acta ENVIADA.
     * Devuelve null si el archivo no existe.
     */
    public Resource obtenerPdfPorToken(String token) {
        FirmaToken firmaToken = validadorToken.validar(token);

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

    /**
     * Sirve el PDF del checklist al firmante (portal publico, sin JWT).
     * Mismas reglas que {@link #obtenerPdfPorToken} pero para el checklist
     * del expediente de entrega.
     */
    public Resource obtenerChecklistPdfPorToken(String token) {
        FirmaToken firmaToken = validadorToken.validar(token);

        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));

        if (acta.getEstado() != EstadoActa.ENVIADA) {
            throw new IllegalArgumentException(
                    "Esta acta no esta disponible para firma. Estado: " + acta.getEstado());
        }

        if (acta.getRutaPdfChecklist() == null || !acta.getRutaPdfChecklist().startsWith("uploads/")) {
            return null;
        }
        Path archivo = Paths.get(uploadsDir)
                .resolve(acta.getRutaPdfChecklist().substring("uploads/".length()));
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo)) {
            return null;
        }

        auditoriaService.registrar(TipoEventoAuditoria.DOCUMENTO_VISTO, null, "PORTAL_FIRMA",
                "ACTA", String.valueOf(acta.getIdActa()), "/firma/" + token + "/checklist/pdf",
                "El firmante visualizo el PDF del Checklist de Entrega (documento asociado del expediente)");

        return new FileSystemResource(archivo.toFile());
    }

    @Transactional
    public FirmaPublicaResponse obtenerActaPorToken(String token) {
        FirmaToken firmaToken = validadorToken.validar(token);

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
                .rutaPdfChecklist(acta.getRutaPdfChecklist())
                .nombreUsuario(acta.getNombreUsuario())
                .cedulaUsuario(acta.getCedulaUsuario())
                .correoUsuario(acta.getCorreoUsuario())
                .serialEquipo(acta.getSerialEquipo())
                .placaEquipo(acta.getPlacaEquipo())
                .ticketGlpi(acta.getTicketGlpi())
                .fechaRechazo(acta.getFechaRechazo())
                .observacionRechazo(acta.getObservacionRechazo())
                .build();
    }

    @Transactional
    public void rechazarActa(String token, String motivo) {
        FirmaToken firmaToken = validadorToken.validar(token);

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
        FirmaToken firmaToken = validadorToken.validar(token);

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

            // SEC-008: se validan AMBAS evidencias ANTES de escribir nada al filesystem:
            // si la foto falla no queda una firma huérfana a medio persistir.
            byte[] firmaBytes = decodificarEvidenciaValida(
                    request.firmaBase64(), TAMANO_MAX_FIRMA, MAGIC_PNG, "La firma");
            byte[] fotoBytes = decodificarEvidenciaValida(
                    request.fotoBase64(), TAMANO_MAX_FOTO, MAGIC_JPEG, "La foto");
            Path rutaFirma = directorioFirmas.resolve("firma_" + acta.getIdActa() + ".png");
            Files.write(rutaFirma, firmaBytes);
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
