package com.empresa.actas.acta.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final EvidenciaRepository evidenciaRepository;

    @Value("${app.uploads-dir:uploads}")
    private String uploadsDir;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String generarPdfFinal(Acta acta) {
        try {
            Path directorioPdf = Paths.get(uploadsDir, "pdf");
            Files.createDirectories(directorioPdf);

            Path rutaPdf = directorioPdf.resolve("acta_" + acta.getIdActa() + ".pdf");

            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, Files.newOutputStream(rutaPdf));
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("ACTA DE " + acta.getTipoActa().name(), tituloFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Estado: " + acta.getEstado().name(), subtituloFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("DATOS DEL ACTA", subtituloFont));
            document.add(new Paragraph("ID Acta: " + acta.getIdActa(), normalFont));
            document.add(new Paragraph("Ticket GLPI: " + acta.getTicketGlpi(), normalFont));
            document.add(new Paragraph("Fecha creacion: " + (acta.getFechaCreacion() != null
                    ? acta.getFechaCreacion().format(FORMATTER) : "N/A"), normalFont));
            document.add(new Paragraph("Fecha envio: " + (acta.getFechaEnvio() != null
                    ? acta.getFechaEnvio().format(FORMATTER) : "N/A"), normalFont));
            document.add(new Paragraph("Fecha firma: " + (acta.getFechaFirma() != null
                    ? acta.getFechaFirma().format(FORMATTER) : "N/A"), normalFont));
            document.add(new Paragraph("Fecha aprobacion: " + (acta.getFechaAprobacion() != null
                    ? acta.getFechaAprobacion().format(FORMATTER) : "N/A"), normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("DATOS DEL USUARIO", subtituloFont));
            document.add(new Paragraph("Nombre: " + acta.getNombreUsuario(), normalFont));
            document.add(new Paragraph("Cedula: " + acta.getCedulaUsuario(), normalFont));
            document.add(new Paragraph("Correo: " + acta.getCorreoUsuario(), normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("DATOS DEL EQUIPO", subtituloFont));
            document.add(new Paragraph("Serial: " + acta.getSerialEquipo(), normalFont));
            document.add(new Paragraph("Placa: " + acta.getPlacaEquipo(), normalFont));
            document.add(new Paragraph("Descripcion: " + acta.getDescripcionEquipo(), normalFont));
            document.add(new Paragraph(" "));

            List<Evidencia> evidencias = evidenciaRepository.findByIdActa(acta.getIdActa());

            for (Evidencia evidencia : evidencias) {
                document.add(new Paragraph("EVIDENCIA: " + evidencia.getTipo(), subtituloFont));

                String textoAlternativo = "Evidencia no disponible";
                if (evidencia.getTipo() == Evidencia.TipoEvidencia.FIRMA) {
                    textoAlternativo = "Firma no disponible";
                } else if (evidencia.getTipo() == Evidencia.TipoEvidencia.FOTO) {
                    textoAlternativo = "Foto no disponible";
                } else if (evidencia.getTipo() == Evidencia.TipoEvidencia.PDF_FINAL) {
                    textoAlternativo = "PDF no disponible";
                }

                Path rutaImagen = Paths.get(evidencia.getRutaArchivo());

                if (!Files.exists(rutaImagen)) {
                    log.warn("Archivo no encontrado para evidencia {}: {}",
                            evidencia.getIdEvidencia(), evidencia.getRutaArchivo());
                    document.add(new Paragraph(textoAlternativo, normalFont));
                    document.add(new Paragraph(" "));
                    continue;
                }

                try {
                    BufferedImage bufferedImage = ImageIO.read(rutaImagen.toFile());
                    if (bufferedImage == null) {
                        log.warn("Imagen invalida para evidencia {}: {}",
                                evidencia.getIdEvidencia(), evidencia.getRutaArchivo());
                        document.add(new Paragraph(textoAlternativo, normalFont));
                        document.add(new Paragraph(" "));
                        continue;
                    }

                    Image imagen = Image.getInstance(rutaImagen.toAbsolutePath().toString());
                    imagen.scaleToFit(450, 300);
                    imagen.setAlignment(Element.ALIGN_CENTER);
                    document.add(imagen);
                    document.add(new Paragraph(" "));

                } catch (IOException e) {
                    log.warn("Error leyendo imagen evidencia {}: {} - {}",
                            evidencia.getIdEvidencia(), evidencia.getRutaArchivo(), e.getMessage());
                    document.add(new Paragraph(textoAlternativo, normalFont));
                    document.add(new Paragraph(" "));
                }
            }

            document.close();
            return rutaPdf.toAbsolutePath().toString();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage());
        }
    }
}
