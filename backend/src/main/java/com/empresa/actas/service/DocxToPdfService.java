package com.empresa.actas.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocxToPdfService {

    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

    public String convertirDocxAPdf(Path docxPath, Path outputDir) throws IOException {
        try {
            Files.createDirectories(outputDir);

            String pdfName = docxPath.getFileName().toString()
                    .replaceAll("(?i)\\.docx$", ".pdf");
            Path pdfPath = outputDir.resolve(pdfName);

            try (FileInputStream fis = new FileInputStream(docxPath.toFile());
                 XWPFDocument doc = new XWPFDocument(fis);
                 Document pdfDoc = new Document(PageSize.A4, 50, 50, 50, 50)) {

                PdfWriter.getInstance(pdfDoc, Files.newOutputStream(pdfPath));
                pdfDoc.open();

                for (IBodyElement element : doc.getBodyElements()) {
                    if (element instanceof XWPFParagraph para) {
                        processParagraph(para, pdfDoc);
                    } else if (element instanceof XWPFTable table) {
                        processTable(table, pdfDoc);
                    }
                }

                pdfDoc.close();
            }

            return pdfPath.toString().replace("\\", "/");

        } catch (DocumentException e) {
            throw new IOException("Error generando PDF desde DOCX: " + e.getMessage(), e);
        }
    }

    private void processParagraph(XWPFParagraph para, Document pdfDoc) throws DocumentException {
        String text = para.getText();
        if (text.trim().isEmpty()) return;

        boolean allBold = para.getRuns().stream()
                .allMatch(r -> {
                    String t = r.getText(0);
                    return t == null || t.trim().isEmpty() || r.isBold();
                });

        Font font;
        if (allBold && text.length() < 60) {
            font = para.getText().toUpperCase().contains("ACTA")
                    || para.getText().toUpperCase().contains("MEMORANDO")
                    ? FONT_TITLE : FONT_SUBTITLE;
        } else {
            font = hasBoldRuns(para) ? FONT_BOLD : FONT_NORMAL;
        }

        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(4);
        p.setMultipliedLeading(1.15f);

        switch (para.getAlignment()) {
            case CENTER -> p.setAlignment(Element.ALIGN_CENTER);
            case RIGHT -> p.setAlignment(Element.ALIGN_RIGHT);
            case BOTH, DISTRIBUTE -> p.setAlignment(Element.ALIGN_JUSTIFIED);
            default -> p.setAlignment(Element.ALIGN_LEFT);
        }

        pdfDoc.add(p);
    }

    private void processTable(XWPFTable table, Document pdfDoc) throws DocumentException {
        int cols = table.getRows().isEmpty() ? 1
                : table.getRow(0).getTableCells().size();

        PdfPTable pdfTable = new PdfPTable(cols);
        pdfTable.setWidthPercentage(100);

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = cell.getText().trim();
                if (cellText.isEmpty()) cellText = " ";

                PdfPCell pdfCell = new PdfPCell(new Phrase(cellText, FONT_NORMAL));
                pdfCell.setPadding(4);
                pdfCell.setBorder(Rectangle.BOX);
                pdfTable.addCell(pdfCell);
            }
        }

        pdfDoc.add(pdfTable);
    }

    private boolean hasBoldRuns(XWPFParagraph para) {
        for (XWPFRun run : para.getRuns()) {
            String t = run.getText(0);
            if (t != null && !t.trim().isEmpty() && run.isBold()) return true;
        }
        return false;
    }
}
