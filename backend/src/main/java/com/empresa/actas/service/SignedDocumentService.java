package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.empresa.actas.firma.repository.FirmaTokenRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SignedDocumentService {

    private static final Logger log = LoggerFactory.getLogger(SignedDocumentService.class);

    private final ActaRepository actaRepository;
    private final FirmaTokenRepository firmaTokenRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final ActaHistorialService actaHistorialService;
    private final DocumentoWordService documentoWordService;
    private final LibreOfficePdfService libreOfficePdfService;
    private final ObjectMapper objectMapper;

    @Value("${app.generated-dir}")
    private String generatedDir;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    @Transactional
    public void generarDocumentoFirmado(String token) {
        FirmaToken firmaToken = firmaTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token no valido"));

        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));

        if (acta.getDatosOriginales() == null || acta.getDatosOriginales().isBlank()) {
            log.warn("Acta {} no tiene datosOriginales — se omite generacion de documento firmado", acta.getIdActa());
            return;
        }

        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Map<String, Object> datos;
            try {
                datos = objectMapper.readValue(acta.getDatosOriginales(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("Error deserializando datosOriginales del acta {}: {}", acta.getIdActa(), e.getMessage());
                return;
            }

            if (acta.getFechaFirma() != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                datos.put("fecha_firma", acta.getFechaFirma().format(fmt));
            }

            Path docxPath;
            switch (acta.getTipoActa()) {
                case ENTREGA -> docxPath = documentoWordService.generarActa(datos);
                case DEVOLUCION -> docxPath = documentoWordService.generarDevolucion(datos);
                default -> {
                    log.warn("Tipo de acta no soportado: {}", acta.getTipoActa());
                    return;
                }
            }

            Path firmaPath = Paths.get(uploadsDir, "firmas", "firma_" + acta.getIdActa() + ".png");
            Path fotoPath = Paths.get(uploadsDir, "fotos", "foto_" + acta.getIdActa() + ".jpg");

            if (!Files.exists(firmaPath) || !Files.exists(fotoPath)) {
                log.warn("Firma o foto no encontradas para acta {} — se omite insercion de imagenes", acta.getIdActa());
            } else {
                byte[] firmaBytes = Files.readAllBytes(firmaPath);
                byte[] fotoBytes = Files.readAllBytes(fotoPath);
                DocxImageReplacer.reemplazarFirmaYFoto(docxPath.toString(), firmaBytes, fotoBytes);
            }

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileNombreBase = libreOfficePdfService.convertirDocxAPdf(docxPath, pdfDir);

            // Nombre unico por acta: el PDF hereda el nombre del DOCX (derivado del contenido,
            // ejemplo "Devolucion_123_a"), asi que dos actas con los mismos datos compartian
            // ruta y la regeneracion de una sobreescribia el PDF firmado de la otra.
            String pdfFileName = pdfFileNombreBase.replaceFirst("(?i)\\.pdf$", "_acta" + acta.getIdActa() + ".pdf");
            Files.move(pdfDir.resolve(pdfFileNombreBase), pdfDir.resolve(pdfFileName),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String rutaPdf = "uploads/pdf/" + pdfFileName;

            Evidencia evidenciaPdf = Evidencia.builder()
                    .idActa(acta.getIdActa())
                    .tipo(Evidencia.TipoEvidencia.PDF_FINAL)
                    .rutaArchivo(rutaPdf)
                    .build();
            evidenciaRepository.save(evidenciaPdf);

            actaHistorialService.registrarEvento(
                    acta.getIdActa(),
                    TipoEventoActa.EVIDENCIA_CARGADA,
                    null,
                    acta.getEstado(),
                    null,
                    "SISTEMA",
                    firmaToken.getIdToken(),
                    "Tipo: PDF_FINAL - " + rutaPdf);

            acta.setRutaPdf(rutaPdf);
            actaRepository.save(acta);

            actaHistorialService.registrarEvento(
                    acta.getIdActa(),
                    TipoEventoActa.PDF_REGENERADO,
                    null,
                    acta.getEstado(),
                    null,
                    "SISTEMA",
                    firmaToken.getIdToken(),
                    "PDF del documento firmado regenerado: " + rutaPdf);

            log.info("Documento firmado generado para acta {}: {}", acta.getIdActa(), rutaPdf);

        } catch (IOException e) {
            log.error("Error generando documento firmado para acta {}: {}", acta.getIdActa(), e.getMessage());
        }
    }
}
