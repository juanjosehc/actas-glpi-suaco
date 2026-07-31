package com.empresa.actas.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class DocxImageReplacer {

    private static final double EMU_PER_CM = 360000.0;
    private static final double EMU_PER_TWIP = 635.0;
    private static final double PHOTO_CELL_FILL = 0.85;

    private static final double SIG_WIDTH_CM = 4.0;
    private static final double SIG_MAX_HEIGHT_CM = 2.0;

    private static final double PHOTO_WIDTH_CM = 2.5;
    private static final double PHOTO_HEIGHT_CM = 3.0;

    private static int cmToEmu(double cm) {
        return (int) Math.round(cm * EMU_PER_CM);
    }

    public static void reemplazarFirmaYFoto(
            String docxPath,
            byte[] firmaBytes,
            byte[] fotoBytes
    ) throws IOException {
        try (FileInputStream fis = new FileInputStream(docxPath);
             XWPFDocument doc = new XWPFDocument(fis)) {

            reemplazarEnParrafos(doc.getParagraphs(), doc, "{{firma_usuario}}",
                    firmaBytes, XWPFDocument.PICTURE_TYPE_PNG, dimensionesFirma(firmaBytes), 0);
            reemplazarEnParrafos(doc.getParagraphs(), doc, "{{foto_usuario}}",
                    fotoBytes, XWPFDocument.PICTURE_TYPE_JPEG, dimensionesFoto(fotoBytes), 0);

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        reemplazarEnParrafos(cell.getParagraphs(), doc, "{{firma_usuario}}",
                                firmaBytes, XWPFDocument.PICTURE_TYPE_PNG, dimensionesFirma(firmaBytes), 0);
                        boolean tieneFoto = false;
                        for (XWPFParagraph pp : cell.getParagraphs()) {
                            String texto = pp.getText();
                            if (texto != null && texto.contains("{{foto_usuario}}")) {
                                tieneFoto = true;
                                break;
                            }
                        }
                        if (tieneFoto) {
                            var tcPr = cell.getCTTc().getTcPr();
                            if (tcPr != null && tcPr.isSetVAlign()) tcPr.unsetVAlign();
                            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.TOP);
                        }
                        int[] fotoDims = dimensionesFotoEnCelda(fotoBytes, row, cell);
                        reemplazarEnParrafos(cell.getParagraphs(), doc, "{{foto_usuario}}",
                                fotoBytes, XWPFDocument.PICTURE_TYPE_JPEG, fotoDims, fotoDims[2]);
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(docxPath)) {
                doc.write(fos);
            }
        }
    }

    private static int[] dimensionesFirma(byte[] firmaBytes) throws IOException {
        int anchoEmu = cmToEmu(SIG_WIDTH_CM);
        int altoEmu = cmToEmu(SIG_MAX_HEIGHT_CM);
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(firmaBytes));
            if (img != null && img.getWidth() > 0) {
                double aspect = (double) img.getHeight() / img.getWidth();
                int altoProporcional = (int) Math.round(anchoEmu * aspect);
                altoEmu = Math.min(altoEmu, altoProporcional);
            }
        } catch (IOException e) {
            altoEmu = cmToEmu(1.2);
        }
        return new int[]{anchoEmu, altoEmu};
    }

    private static int[] dimensionesFoto(byte[] fotoBytes) {
        int maxAnchoEmu = cmToEmu(PHOTO_WIDTH_CM);
        int maxAltoEmu = cmToEmu(PHOTO_HEIGHT_CM);
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(fotoBytes));
            if (img != null && img.getWidth() > 0) {
                double aspect = (double) img.getHeight() / img.getWidth();
                double ancho = maxAnchoEmu;
                double alto = ancho * aspect;
                if (alto > maxAltoEmu) {
                    alto = maxAltoEmu;
                    ancho = alto / aspect;
                }
                return new int[]{(int) Math.round(ancho), (int) Math.round(alto)};
            }
        } catch (IOException e) {
            return new int[]{maxAnchoEmu, maxAltoEmu};
        }
        return new int[]{maxAnchoEmu, maxAltoEmu};
    }

    private static int[] dimensionesFotoEnCelda(byte[] fotoBytes, XWPFTableRow row, XWPFTableCell cell) throws IOException {
        int[] fallback = dimensionesFoto(fotoBytes);

        int anchoTwips = cell.getWidth();
        int altoTwips = row.getHeight();
        if (anchoTwips > 0 && altoTwips > 0) {
            anchoTwips -= margenCeldaEnTwips(cell, true);
            anchoTwips -= margenCeldaEnTwips(cell, false);
        }
        if (anchoTwips <= 0 || altoTwips <= 0) {
            return new int[]{fallback[0], fallback[1], espaciadoVerticalTwips(altoTwips, fallback[1])};
        }

        double maxAnchoEmu = anchoTwips * EMU_PER_TWIP * PHOTO_CELL_FILL;
        double maxAltoEmu = altoTwips * EMU_PER_TWIP * PHOTO_CELL_FILL;

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(fotoBytes));
        if (img == null || img.getWidth() <= 0) {
            return new int[]{fallback[0], fallback[1], espaciadoVerticalTwips(altoTwips, fallback[1])};
        }
        double aspect = (double) img.getHeight() / img.getWidth();

        double ancho = maxAnchoEmu;
        double alto = ancho * aspect;
        if (alto > maxAltoEmu) {
            alto = maxAltoEmu;
            ancho = alto / aspect;
        }
        int anchoEmu = (int) Math.round(ancho);
        int altoEmu = (int) Math.round(alto);
        return new int[]{anchoEmu, altoEmu, espaciadoVerticalTwips(altoTwips, altoEmu)};
    }

    private static int espaciadoVerticalTwips(int altoFilaTwips, int altoImagenEmu) {
        if (altoFilaTwips <= 0 || altoImagenEmu <= 0) return 0;
        double altoFilaPt = altoFilaTwips / 20.0;
        double altoImagenPt = altoImagenEmu / 12700.0;
        double bordesPt = 2.0;
        double espaciadoPt = Math.max(0, (altoFilaPt - bordesPt - altoImagenPt) / 2.0);
        return (int) Math.round(espaciadoPt * 20.0);
    }

    private static int margenCeldaEnTwips(XWPFTableCell cell, boolean izquierdo) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null || tcPr.getTcMar() == null) return 0;
        var margen = izquierdo ? tcPr.getTcMar().getLeft() : tcPr.getTcMar().getRight();
        if (margen == null || margen.getW() == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(margen.getW()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void reemplazarEnParrafos(
            List<XWPFParagraph> parrafos,
            XWPFDocument doc,
            String placeholder,
            byte[] imagenBytes,
            int pictureType,
            int[] dimensionesEmu,
            int espaciadoVerticalTwips
    ) throws IOException {
        for (XWPFParagraph p : parrafos) {
            String fullText = p.getText();
            if (fullText == null || !fullText.contains(placeholder)) continue;

            List<XWPFRun> runs = p.getRuns();
            if (runs == null || runs.isEmpty()) continue;

            int n = runs.size();
            int[] boundary = new int[n + 1];
            StringBuilder concat = new StringBuilder();
            for (int r = 0; r < n; r++) {
                boundary[r] = concat.length();
                String t = runs.get(r).text();
                concat.append(t != null ? t : "");
            }
            boundary[n] = concat.length();
            String full = concat.toString();

            int phLen = placeholder.length();
            int idx = 0;
            while ((idx = full.indexOf(placeholder, idx)) != -1) {
                int phStart = idx;
                int phEnd = idx + phLen;
                boolean imageInserted = false;

                for (int r = 0; r < n; r++) {
                    int runStart = boundary[r];
                    int runEnd = boundary[r + 1];

                    if (phStart >= runEnd || phEnd <= runStart) continue;

                    XWPFRun run = runs.get(r);
                    String runText = run.getText(0);
                    if (runText == null) runText = "";

                    int overlapStart = Math.max(phStart, runStart);
                    int overlapEnd = Math.min(phEnd, runEnd);
                    int localStart = overlapStart - runStart;
                    int localEnd = overlapEnd - runStart;

                    String cleaned = runText.substring(0, localStart)
                            + runText.substring(localEnd);
                    run.setText(cleaned, 0);

                    if (!imageInserted) {
                        insertarImagenEnRun(run, doc, imagenBytes, pictureType, dimensionesEmu);
                        imageInserted = true;
                    }
                }

                idx = phEnd;
            }

            if (espaciadoVerticalTwips > 0) {
                p.setSpacingBefore(espaciadoVerticalTwips);
                p.setSpacingAfter(espaciadoVerticalTwips);
            }
        }
    }

    private static void insertarImagenEnRun(
            XWPFRun run,
            XWPFDocument doc,
            byte[] imagenBytes,
            int pictureType,
            int[] dimensionesEmu
    ) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imagenBytes)) {
            run.addPicture(bais, pictureType, "imagen", dimensionesEmu[0], dimensionesEmu[1]);
        } catch (Exception e) {
            throw new IOException("Error insertando imagen en el documento", e);
        }
    }
}
