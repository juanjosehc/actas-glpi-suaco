package com.empresa.actas.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.acta.entity.TipoActa;
import com.empresa.actas.acta.entity.TipoEventoActa;
import com.empresa.actas.acta.repository.ActaRepository;
import com.empresa.actas.acta.service.ActaHistorialService;
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.entity.FirmaToken;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.empresa.actas.firma.repository.FirmaTokenRepository;
import com.empresa.actas.usuario.service.UsuarioService;
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
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;

    @Value("${app.generated-dir}")
    private String generatedDir;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    /**
     * Regenera el documento firmado y su PDF en segundo plano, tras la firma.
     * Corre en su propia transaccion (llamado por el executor del portal).
     */
    @Transactional
    public void generarDocumentoFirmado(String token) {
        FirmaToken firmaToken = firmaTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token no valido"));

        Acta acta = actaRepository.findById(firmaToken.getIdActa())
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));

        // El ID dentro de la REGEN solo marca "en curso" para la aprobacion.
        REGEN_EN_CURSO.add(acta.getIdActa());
        try {
            generarDocumentoFirmadoInterno(acta, firmaToken.getIdToken());
        } finally {
            REGEN_EN_CURSO.remove(acta.getIdActa());
        }
    }

    /**
     * Garantiza que un acta ya firmada tenga su PDF firmado ANTES de aprobarla
     * (QA-06: la aprobacion ya no puede caer en la ventana en que el documento
     * firmado aun se esta generando en segundo plano).
     *
     * - Si el PDF firmado ya existe (ruta con sufijo _acta{id}.pdf para el acta
     *   y _checklist{id}.pdf para el checklist en ENTREGA), no hace nada.
     * - Si la regeneracion async esta en curso, espera hasta que aparezcan los
     *   archivos y apunta el acta a ellos (no genera duplicado).
     * - Si no hay nada en curso, regenera aqui (bloqueante).
     */
    public boolean regenerarDocumentoFirmadoParaAprobacion(Acta acta) {
        Long idActa = acta.getIdActa();
        String sufijoActa = "_acta" + idActa + ".pdf";
        String sufijoChecklist = "_checklist" + idActa + ".pdf";

        boolean actaOk = acta.getRutaPdf() != null && acta.getRutaPdf().endsWith(sufijoActa)
                && pdfExiste(acta.getRutaPdf());
        boolean checklistOk = !requiereChecklist(acta)
                || (acta.getRutaPdfChecklist() != null && acta.getRutaPdfChecklist().endsWith(sufijoChecklist)
                    && pdfExiste(acta.getRutaPdfChecklist()));
        if (actaOk && checklistOk) {
            return true;
        }

        if (acta.getDatosOriginales() == null || acta.getDatosOriginales().isBlank()) {
            log.warn("Acta {} sin datosOriginales: no se puede regenerar documento firmado", idActa);
            return false;
        }

        Path pdfDir = Paths.get(uploadsDir, "pdf");
        if (REGEN_EN_CURSO.contains(idActa)) {
            for (int i = 0; i < 60 && REGEN_EN_CURSO.contains(idActa); i++) {
                if (!actaOk) {
                    Path actaFirmada = buscarPdfFirmado(pdfDir, idActa, sufijoActa);
                    if (actaFirmada != null) {
                        acta.setRutaPdf(rutaVirtualPdf(actaFirmada));
                        actaOk = true;
                    }
                }
                if (requiereChecklist(acta) && !checklistOk) {
                    Path checklistFirmado = buscarPdfFirmado(pdfDir, idActa, sufijoChecklist);
                    if (checklistFirmado != null) {
                        acta.setRutaPdfChecklist(rutaVirtualPdf(checklistFirmado));
                        checklistOk = true;
                    }
                }
                if (actaOk && checklistOk) {
                    actaRepository.save(acta);
                    return true;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        FirmaToken token = firmaTokenRepository
                .findFirstByIdActaOrderByFechaCreacionDesc(idActa)
                .orElse(null);
        return generarDocumentoFirmadoInterno(acta, token != null ? token.getIdToken() : null);
    }

    /** Lista de actas cuya regeneracion async sigue en curso. */
    private static final java.util.Set<Long> REGEN_EN_CURSO =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private boolean generarDocumentoFirmadoInterno(Acta acta, Long idTokenFirma) {
        Long idActa = acta.getIdActa();

        String sufijo = "_acta" + idActa + ".pdf";
        if (acta.getRutaPdf() != null && acta.getRutaPdf().endsWith(sufijo)
                && pdfExiste(acta.getRutaPdf())) {
            log.info("Acta {} ya tiene PDF firmado ({}), se omite regeneracion", idActa, acta.getRutaPdf());
            return true;
        }

        if (acta.getDatosOriginales() == null || acta.getDatosOriginales().isBlank()) {
            log.warn("Acta {} no tiene datosOriginales — se omite generacion de documento firmado", idActa);
            return false;
        }

        try {
            Path outputDir = Paths.get(generatedDir);
            Files.createDirectories(outputDir);

            Map<String, Object> datos;
            try {
                datos = objectMapper.readValue(acta.getDatosOriginales(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("Error deserializando datosOriginales del acta {}: {}", idActa, e.getMessage());
                return false;
            }

            if (acta.getFechaFirma() != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                datos.put("fecha_firma", acta.getFechaFirma().format(fmt));
            }

            Path docxPath;
            switch (acta.getTipoActa()) {
                case ENTREGA -> docxPath = documentoWordService.generarActa(datos);
                case DEVOLUCION -> docxPath = documentoWordService.generarDevolucion(datos);
                case FORMATEO -> docxPath = documentoWordService.generarFormateoSeguro(datos);
                default -> {
                    log.warn("Tipo de acta no soportado: {}", acta.getTipoActa());
                    return false;
                }
            }

            Path firmaPath = Paths.get(uploadsDir, "firmas", "firma_" + idActa + ".png");
            Path fotoPath = Paths.get(uploadsDir, "fotos", "foto_" + idActa + ".jpg");

            // Cada imagen se inserta por separado: si una falta, la otra se pone
            // y la ausente se deja en blanco (nunca queda {{...}} literal).
            byte[] firmaBytes = Files.exists(firmaPath) ? Files.readAllBytes(firmaPath) : null;
            byte[] fotoBytes = Files.exists(fotoPath) ? Files.readAllBytes(fotoPath) : null;
            if (firmaBytes == null) {
                log.warn("Firma no encontrada para acta {} — placeholder de firma en blanco", idActa);
            }
            if (fotoBytes == null) {
                log.warn("Foto no encontrada para acta {} — placeholder de foto en blanco", idActa);
            }
            DocxImageReplacer.reemplazarFirmaYFoto(docxPath.toString(), firmaBytes, fotoBytes);

            // Firma permanente del tecnico: va en TODOS los DOCX (iniciales y
            // regenerados). Se obtiene por idTecnico del acta; si no existe,
            // el placeholder queda en blanco.
            byte[] firmaTecnico = usuarioService.obtenerFirmaBytesDe(acta.getIdTecnico());
            DocxImageReplacer.reemplazarFirmaTecnico(docxPath.toString(), firmaTecnico);

            Path pdfDir = Paths.get(uploadsDir, "pdf");
            String pdfFileNombreBase = libreOfficePdfService.convertirDocxAPdf(docxPath, pdfDir);

            // Nombre unico por acta: el PDF hereda el nombre del DOCX (derivado del contenido,
            // ejemplo "Devolucion_123_a"), asi que dos actas con los mismos datos compartian
            // ruta y la regeneracion de una sobreescribia el PDF firmado de la otra.
            String pdfFileName = pdfFileNombreBase.replaceFirst("(?i)\\.pdf$", sufijo);
            Files.move(pdfDir.resolve(pdfFileNombreBase), pdfDir.resolve(pdfFileName),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String rutaPdf = "uploads/pdf/" + pdfFileName;

            Evidencia evidenciaPdf = Evidencia.builder()
                    .idActa(idActa)
                    .tipo(Evidencia.TipoEvidencia.PDF_FINAL)
                    .rutaArchivo(rutaPdf)
                    .build();
            evidenciaRepository.save(evidenciaPdf);

            actaHistorialService.registrarEvento(
                    idActa,
                    TipoEventoActa.EVIDENCIA_CARGADA,
                    null,
                    acta.getEstado(),
                    null,
                    "SISTEMA",
                    idTokenFirma,
                    "Tipo: PDF_FINAL - " + rutaPdf);

            acta.setRutaPdf(rutaPdf);
            actaRepository.save(acta);

            actaHistorialService.registrarEvento(
                    idActa,
                    TipoEventoActa.PDF_REGENERADO,
                    null,
                    acta.getEstado(),
                    null,
                    "SISTEMA",
                    idTokenFirma,
                    "PDF del documento firmado regenerado: " + rutaPdf);

            if (requiereChecklist(acta)) {
                try {
                    // ENTREGA: el expediente incluye el checklist. Se regenera, se
                    // le insertan las mismas firmas (usuario, foto y tecnico) y se
                    // convierte a PDF firmado con su propia evidencia y ruta.
                    Path checklistDocx = documentoWordService.generarChecklist(datos);
                    DocxImageReplacer.reemplazarFirmaYFoto(checklistDocx.toString(), firmaBytes, fotoBytes);
                    DocxImageReplacer.reemplazarFirmaTecnico(checklistDocx.toString(), firmaTecnico);

                    String sufijoChecklist = "_checklist" + idActa + ".pdf";
                    String chkBase = libreOfficePdfService.convertirDocxAPdf(checklistDocx, pdfDir);
                    String chkName = chkBase.replaceFirst("(?i)\\.pdf$", sufijoChecklist);
                    Files.move(pdfDir.resolve(chkBase), pdfDir.resolve(chkName),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    String rutaChecklistPdf = "uploads/pdf/" + chkName;

                    evidenciaRepository.save(Evidencia.builder()
                            .idActa(idActa)
                            .tipo(Evidencia.TipoEvidencia.CHECKLIST_FINAL)
                            .rutaArchivo(rutaChecklistPdf)
                            .build());

                    actaHistorialService.registrarEvento(
                            idActa,
                            TipoEventoActa.EVIDENCIA_CARGADA,
                            null,
                            acta.getEstado(),
                            null,
                            "SISTEMA",
                            idTokenFirma,
                            "Tipo: CHECKLIST_FINAL - " + rutaChecklistPdf);

                    acta.setRutaPdfChecklist(rutaChecklistPdf);
                    actaRepository.save(acta);

                    actaHistorialService.registrarEvento(
                            idActa,
                            TipoEventoActa.PDF_REGENERADO,
                            null,
                            acta.getEstado(),
                            null,
                            "SISTEMA",
                            idTokenFirma,
                            "PDF del checklist de entrega firmado regenerado: " + rutaChecklistPdf);

                    log.info("Checklist firmado generado para acta {}: {}", idActa, rutaChecklistPdf);
                } catch (IOException e) {
                    // El acta firmada ya quedo lista; si el checklist falla se loguea
                    // y se revalida en la aprobacion (regenerarDocumentoFirmadoParaAprobacion).
                    log.error("Error generando checklist firmado para acta {}: {}", idActa, e.getMessage());
                }
            }

            log.info("Documento firmado generado para acta {}: {}", idActa, rutaPdf);
            return true;

        } catch (IOException e) {
            log.error("Error generando documento firmado para acta {}: {}", idActa, e.getMessage());
            return false;
        }
    }

    /** Busca en pdfDir un archivo cuyo nombre termine en el sufijo dado (resultado async). */
    private Path buscarPdfFirmado(Path pdfDir, Long idActa, String sufijo) {
        if (!Files.isDirectory(pdfDir)) return null;
        try (var files = Files.list(pdfDir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(sufijo))
                    .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /** El checklist es parte del expediente solo en ENTREGA. */
    private boolean requiereChecklist(Acta acta) {
        return acta.getTipoActa() == TipoActa.ENTREGA;
    }

    private boolean pdfExiste(String rutaVirtual) {
        return resolverPdf(rutaVirtual) != null;
    }

    private Path resolverPdf(String rutaVirtual) {
        if (rutaVirtual == null || !rutaVirtual.startsWith("uploads/")) return null;
        Path archivo = Paths.get(uploadsDir).resolve(rutaVirtual.substring("uploads/".length()));
        return Files.exists(archivo) && Files.isRegularFile(archivo) ? archivo : null;
    }

    private String rutaVirtualPdf(Path archivo) {
        return "uploads/pdf/" + archivo.getFileName().toString();
    }
}
