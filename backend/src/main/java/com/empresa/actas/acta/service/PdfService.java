package com.empresa.actas.acta.service;

import com.empresa.actas.acta.entity.Acta;
import com.empresa.actas.firma.entity.Evidencia;
import com.empresa.actas.firma.repository.EvidenciaRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
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

    private static final Color PRIMARY_COLOR = new Color(30, 58, 138);
    private static final Color BG_LIGHT = new Color(241, 245, 249);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    public String generarPdfFinal(Acta acta) {
        try {
            Path directorioPdf = Paths.get(uploadsDir, "pdf");
            Files.createDirectories(directorioPdf);

            Path rutaPdf = directorioPdf.resolve("acta_" + acta.getIdActa() + ".pdf");

            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, Files.newOutputStream(rutaPdf));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_COLOR);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_MUTED);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_MUTED);

            // Header
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3, 2});

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerCell.addElement(new Paragraph("COMPANIA DE FINANCIAMIENTO COMERCIAL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, PRIMARY_COLOR)));
            headerCell.addElement(new Paragraph("COLTEFINANCIERA S.A.", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, PRIMARY_COLOR)));
            headerCell.addElement(new Paragraph("NIT 890.000.000-0", smallFont));
            headerTable.addCell(headerCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(new Paragraph("ACTA N° " + acta.getIdActa(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK)));
            rightCell.addElement(new Paragraph("Fecha: " + (acta.getFechaCreacion() != null ? acta.getFechaCreacion().format(FORMATTER) : "N/A"), smallFont));
            headerTable.addCell(rightCell);

            document.add(headerTable);

            // Separator
            PdfPTable sep = new PdfPTable(1);
            sep.setWidthPercentage(100);
            PdfPCell sepCell = new PdfPCell();
            sepCell.setBorder(Rectangle.BOTTOM);
            sepCell.setBorderColorBottom(PRIMARY_COLOR);
            sepCell.setBorderWidthBottom(2f);
            sepCell.setFixedHeight(8f);
            sep.addCell(sepCell);
            document.add(sep);

            // Title
            String tipoText = "ENTREGA".equals(acta.getTipoActa().name()) ? "MEMORANDO DE ENTREGA DE DISPOSITIVOS" : "MEMORANDO DE DEVOLUCION DE DISPOSITIVOS";
            Paragraph titlePara = new Paragraph(tipoText, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingBefore(12);
            titlePara.setSpacingAfter(4);
            document.add(titlePara);

            Paragraph subtitlePara = new Paragraph("Por medio del presente documento se deja constancia de la " +
                    ("ENTREGA".equals(acta.getTipoActa().name()) ? "entrega" : "devolucion") +
                    " de equipos de computo y/o dispositivos electronicos.", smallFont);
            subtitlePara.setAlignment(Element.ALIGN_CENTER);
            subtitlePara.setSpacingAfter(16);
            document.add(subtitlePara);

            java.util.function.BiFunction<String, String, PdfPTable> createFieldRow = (label, value) -> {
                PdfPTable t = new PdfPTable(2);
                t.setWidthPercentage(100);
                t.setWidths(new float[]{2.5f, 5.5f});
                PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
                lc.setBorder(Rectangle.NO_BORDER);
                lc.setPaddingTop(3);
                lc.setPaddingBottom(3);
                lc.setPaddingLeft(0);
                lc.setPaddingRight(8);
                PdfPCell vc = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
                vc.setBorder(Rectangle.BOTTOM);
                vc.setBorderColorBottom(BORDER_COLOR);
                vc.setBorderWidthBottom(0.5f);
                vc.setPaddingTop(3);
                vc.setPaddingBottom(3);
                vc.setPaddingLeft(0);
                t.addCell(lc);
                t.addCell(vc);
                return t;
            };

            // Section: Datos del Acta
            Paragraph sec1 = new Paragraph("DATOS DEL ACTA", sectionFont);
            sec1.setSpacingBefore(12);
            sec1.setSpacingAfter(4);
            document.add(sec1);

            document.add(createFieldRow.apply("ID ACTA", String.valueOf(acta.getIdActa())));
            document.add(createFieldRow.apply("TICKET GLPI", acta.getTicketGlpi() != null ? String.valueOf(acta.getTicketGlpi()) : "-"));
            document.add(createFieldRow.apply("ESTADO", acta.getEstado().name()));
            document.add(createFieldRow.apply("FECHA CREACION", acta.getFechaCreacion() != null ? acta.getFechaCreacion().format(FORMATTER) : "N/A"));
            if (acta.getFechaEnvio() != null) document.add(createFieldRow.apply("FECHA ENVIO", acta.getFechaEnvio().format(FORMATTER)));
            if (acta.getFechaFirma() != null) document.add(createFieldRow.apply("FECHA FIRMA", acta.getFechaFirma().format(FORMATTER)));
            if (acta.getFechaAprobacion() != null) document.add(createFieldRow.apply("FECHA APROBACION", acta.getFechaAprobacion().format(FORMATTER)));
            if (acta.getObservacionRechazo() != null) document.add(createFieldRow.apply("OBSERVACION RECHAZO", acta.getObservacionRechazo()));

            // Section: Datos del Usuario
            Paragraph sec2 = new Paragraph("DATOS DEL USUARIO", sectionFont);
            sec2.setSpacingBefore(16);
            sec2.setSpacingAfter(4);
            document.add(sec2);

            document.add(createFieldRow.apply("NOMBRE", acta.getNombreUsuario()));
            document.add(createFieldRow.apply("CEDULA", acta.getCedulaUsuario()));
            document.add(createFieldRow.apply("CORREO", acta.getCorreoUsuario()));
            if (acta.getCargo() != null) document.add(createFieldRow.apply("CARGO", acta.getCargo()));
            if (acta.getLugarTrabajo() != null) document.add(createFieldRow.apply("DEPARTAMENTO / SEDE", acta.getLugarTrabajo()));
            if (acta.getEmpresa() != null) document.add(createFieldRow.apply("EMPRESA", acta.getEmpresa()));

            // Section: Datos del Equipo
            Paragraph sec3 = new Paragraph("DATOS DEL EQUIPO", sectionFont);
            sec3.setSpacingBefore(16);
            sec3.setSpacingAfter(4);
            document.add(sec3);

            document.add(createFieldRow.apply("DESCRIPCION", acta.getDescripcionEquipo()));
            document.add(createFieldRow.apply("SERIAL", acta.getSerialEquipo()));
            document.add(createFieldRow.apply("PLACA INTERNA", acta.getPlacaEquipo()));
            if (acta.getMarcaModelo() != null) document.add(createFieldRow.apply("MARCA / MODELO", acta.getMarcaModelo()));
            if (acta.getProcesador() != null) document.add(createFieldRow.apply("PROCESADOR", acta.getProcesador()));
            if (acta.getMemoriaRam() != null) document.add(createFieldRow.apply("MEMORIA RAM", acta.getMemoriaRam()));
            if (acta.getDiscoDuro() != null) document.add(createFieldRow.apply("DISCO DURO", acta.getDiscoDuro()));
            if (acta.getSistemaOperativo() != null) document.add(createFieldRow.apply("SISTEMA OPERATIVO", acta.getSistemaOperativo()));
            if (acta.getMonitor() != null) document.add(createFieldRow.apply("MONITOR", acta.getMonitor()));
            if (acta.getAccesorios() != null) document.add(createFieldRow.apply("ACCESORIOS", acta.getAccesorios()));
            if (acta.getEstadoEquipo() != null) document.add(createFieldRow.apply("ESTADO EQUIPO", acta.getEstadoEquipo()));
            if (acta.getObservaciones() != null) {
                document.add(createFieldRow.apply("OBSERVACIONES", acta.getObservaciones()));
            }

            // Evidences
            List<Evidencia> evidencias = evidenciaRepository.findByIdActa(acta.getIdActa());

            if (!evidencias.isEmpty()) {
                Paragraph sec4 = new Paragraph("EVIDENCIAS", sectionFont);
                sec4.setSpacingBefore(16);
                sec4.setSpacingAfter(8);
                document.add(sec4);

                for (Evidencia evidencia : evidencias) {
                    String tipoLabel = evidencia.getTipo() == Evidencia.TipoEvidencia.FIRMA ? "Firma" :
                            evidencia.getTipo() == Evidencia.TipoEvidencia.FOTO ? "Foto" : "PDF";

                    Paragraph evTitle = new Paragraph(tipoLabel + ":", subtitleFont);
                    evTitle.setSpacingBefore(8);
                    evTitle.setSpacingAfter(4);
                    document.add(evTitle);

                    Path rutaArchivo = Paths.get(evidencia.getRutaArchivo());
                    String textoAlternativo = tipoLabel + " no disponible";

                    if (!Files.exists(rutaArchivo)) {
                        log.warn("Archivo no encontrado para evidencia {}: {}",
                                evidencia.getIdEvidencia(), evidencia.getRutaArchivo());
                        document.add(new Paragraph(textoAlternativo, normalFont));
                        continue;
                    }

                    if (evidencia.getTipo() == Evidencia.TipoEvidencia.PDF_FINAL) {
                        document.add(new Paragraph("PDF disponible en: " + evidencia.getRutaArchivo(), smallFont));
                        continue;
                    }

                    try {
                        BufferedImage bufferedImage = ImageIO.read(rutaArchivo.toFile());
                        if (bufferedImage == null) {
                            log.warn("Imagen invalida para evidencia {}: {}",
                                    evidencia.getIdEvidencia(), evidencia.getRutaArchivo());
                            document.add(new Paragraph(textoAlternativo, normalFont));
                            continue;
                        }

                        Image imagen = Image.getInstance(rutaArchivo.toAbsolutePath().toString());
                        imagen.scaleToFit(400, 250);
                        imagen.setAlignment(Element.ALIGN_CENTER);
                        document.add(imagen);

                    } catch (IOException e) {
                        log.warn("Error leyendo imagen evidencia {}: {} - {}",
                                evidencia.getIdEvidencia(), evidencia.getRutaArchivo(), e.getMessage());
                        document.add(new Paragraph(textoAlternativo, normalFont));
                    }
                }
            }

            document.close();
            return rutaPdf.toString().replace("\\", "/");

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage());
        }
    }
}
