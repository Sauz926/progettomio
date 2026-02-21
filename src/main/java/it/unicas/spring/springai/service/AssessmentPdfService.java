package it.unicas.spring.springai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.model.Macchinario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentPdfService {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 48f;

    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final float FONT_SIZE_TITLE = 18f;
    private static final float FONT_SIZE_SECTION = 13f;
    private static final float FONT_SIZE_BODY = 11f;

    private static final float LEADING_TITLE = 1.4f * FONT_SIZE_TITLE;
    private static final float LEADING_SECTION = 1.35f * FONT_SIZE_SECTION;
    private static final float LEADING_BODY = 1.35f * FONT_SIZE_BODY;

    private static final DateTimeFormatter DATE_TIME_FILENAME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.ROOT);
    private static final DateTimeFormatter DATE_TIME_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ITALY);

    private final ObjectMapper objectMapper;

    public byte[] generateAssessmentPdf(Macchinario macchinario, AssessmentResult assessment) {
        try (PDDocument document = new PDDocument()) {
            PdfFlow flow = new PdfFlow(document);

            String title = "Assessment di Conformità";
            String subtitle = safe(macchinario != null ? macchinario.getNome() : null, "Macchinario");

            flow.writeCentered(title, FONT_BOLD, FONT_SIZE_TITLE, LEADING_TITLE);
            flow.writeCentered(subtitle, FONT_BOLD, FONT_SIZE_SECTION, LEADING_SECTION);
            flow.blankLine(LEADING_BODY);

            flow.writeSection("Dettagli assessment");
            flow.writeWrappedLine("ID assessment: " + safe(assessment != null ? assessment.getId() : null, "-"));
            flow.writeWrappedLine("Data: " + formatDateTime(assessment != null ? assessment.getDataAssessment() : null));
            flow.writeWrappedLine("Versione: " + safe(assessment != null ? assessment.getVersione() : null, "-"));
            flow.writeWrappedLine("Livello rischio: " + safe(assessment != null ? assessment.getLivelloRischio() : null, "-"));
            flow.writeWrappedLine("Punteggio conformità: " + safe(assessment != null ? assessment.getPunteggioConformita() : null, "-"));
            flow.blankLine(LEADING_BODY);

            if (macchinario != null) {
                flow.writeSection("Dati macchinario");
                flow.writeWrappedLine("Nome: " + safe(macchinario.getNome(), "-"));
                flow.writeWrappedLine("Produttore: " + safe(macchinario.getProduttore(), "Non specificato"));
                flow.writeWrappedLine("Modello: " + safe(macchinario.getModello(), "Non specificato"));
                flow.writeWrappedLine("Numero di serie: " + safe(macchinario.getNumeroSerie(), "Non specificato"));
                flow.writeWrappedLine("Anno produzione: " + safe(macchinario.getAnnoProduzione(), "Non specificato"));
                flow.writeWrappedLine("Categoria: " + safe(macchinario.getCategoria(), "Non specificata"));
                flow.blankLine(LEADING_BODY);

                flow.writeSection("Descrizione");
                flow.writeParagraph(safe(macchinario.getDescrizione(), "Non disponibile"));
                flow.blankLine(LEADING_BODY);

                flow.writeSection("Specifiche tecniche");
                flow.writeParagraph(safe(macchinario.getSpecificheTecniche(), "Non disponibili"));
                flow.blankLine(LEADING_BODY);
            }

            flow.writeSection("Riepilogo conformità");
            flow.writeParagraph(safe(assessment != null ? assessment.getRiepilogoConformita() : null, "Non disponibile"));
            flow.blankLine(LEADING_BODY);

            flow.writeSection("Non conformità rilevate");
            flow.writeParagraph(formatExplainableList(safe(assessment != null ? assessment.getNonConformitaRilevate() : null, ""), "Nessuna non conformità rilevata"));
            flow.blankLine(LEADING_BODY);

            flow.writeSection("Raccomandazioni");
            flow.writeParagraph(formatExplainableList(safe(assessment != null ? assessment.getRaccomandazioni() : null, ""), "Nessuna raccomandazione"));
            flow.blankLine(LEADING_BODY);

            flow.writeSection("Documenti utilizzati");
            flow.writeParagraph(safe(assessment != null ? assessment.getDocumentiUtilizzati() : null, "Nessun documento di riferimento"));
            flow.blankLine(LEADING_BODY);

            flow.writeSection("Testo completo (output)");
            flow.writeParagraph(safe(assessment != null ? assessment.getRisultato() : null, "Non disponibile"));

            flow.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error generating assessment PDF", e);
            throw new RuntimeException("Errore durante la generazione del PDF: " + e.getMessage(), e);
        }
    }

    public String buildDownloadFilename(Macchinario macchinario, AssessmentResult assessment) {
        String machinePart = sanitizeFilenamePart(macchinario != null ? macchinario.getNome() : null, "macchinario");
        String idPart = assessment != null && assessment.getId() != null ? assessment.getId().toString() : "unknown";
        String datePart = assessment != null ? formatFilenameDateTime(assessment.getDataAssessment()) : "unknown";
        return "assessment_" + idPart + "_" + machinePart + "_" + datePart + ".pdf";
    }

    private String formatFilenameDateTime(LocalDateTime value) {
        if (value == null) return "unknown";
        return DATE_TIME_FILENAME.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) return "-";
        return DATE_TIME_DISPLAY.format(value);
    }

    private String safe(Object value, String fallback) {
        if (value == null) return fallback;
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private String formatExplainableList(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return fallback;

        JsonNode node;
        try {
            node = objectMapper.readTree(value);
        } catch (Exception e) {
            return value;
        }

        if (!node.isArray()) {
            return value;
        }

        StringBuilder sb = new StringBuilder();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) continue;

            String text;
            if (item.isTextual()) {
                text = item.asText("");
            } else {
                text = item.path("text").asText("");
                if (text.isBlank()) {
                    text = item.path("testo").asText("");
                }
            }

            text = text == null ? "" : text.trim();
            if (text.isEmpty()) continue;

            if (!sb.isEmpty()) sb.append('\n');
            sb.append("• ").append(text);
        }

        return sb.isEmpty() ? fallback : sb.toString();
    }

    private String sanitizeFilenamePart(String input, String fallback) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return fallback;

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        normalized = normalized.replaceAll("[^a-zA-Z0-9._-]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");

        if (normalized.isBlank()) {
            normalized = fallback;
        }

        if (normalized.length() > 80) {
            normalized = normalized.substring(0, 80);
        }

        return normalized;
    }

    private static final class PdfFlow implements Closeable {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;
        private final float width;

        private PdfFlow(PDDocument document) throws IOException {
            this.document = document;
            this.width = PAGE_SIZE.getWidth() - (2 * MARGIN);
            newPage();
        }

        private void newPage() throws IOException {
            closeQuietly(contentStream);
            page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = PAGE_SIZE.getHeight() - MARGIN;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < MARGIN) {
                newPage();
            }
        }

        private void writeCentered(String text, PDFont font, float fontSize, float leading) throws IOException {
            String line = sanitizeForFont(text, font, fontSize);
            float textWidth = stringWidth(font, fontSize, line);
            float x = Math.max(MARGIN, (PAGE_SIZE.getWidth() - textWidth) / 2f);
            ensureSpace(leading);
            writeLineAt(x, line, font, fontSize);
            y -= leading;
        }

        private void writeSection(String title) throws IOException {
            ensureSpace(LEADING_SECTION);
            String sanitized = sanitizeForFont(title, FONT_BOLD, FONT_SIZE_SECTION);
            writeLineAt(MARGIN, sanitized, FONT_BOLD, FONT_SIZE_SECTION);
            y -= LEADING_SECTION;
        }

        private void writeWrappedLine(String text) throws IOException {
            writeParagraph(text);
        }

        private void writeParagraph(String text) throws IOException {
            String safeText = text == null ? "" : text;
            String normalized = safeText.replace("\r\n", "\n").replace('\r', '\n');

            String[] paragraphs = normalized.split("\n", -1);
            for (int i = 0; i < paragraphs.length; i++) {
                String paragraph = paragraphs[i];
                if (paragraph.isBlank()) {
                    blankLine(LEADING_BODY);
                    continue;
                }

                List<String> lines = wrap(paragraph, FONT_REGULAR, FONT_SIZE_BODY, width);
                for (String line : lines) {
                    ensureSpace(LEADING_BODY);
                    writeLineAt(MARGIN, line, FONT_REGULAR, FONT_SIZE_BODY);
                    y -= LEADING_BODY;
                }
            }
        }

        private void blankLine(float leading) throws IOException {
            ensureSpace(leading);
            y -= leading;
        }

        private void writeLineAt(float x, String line, PDFont font, float fontSize) throws IOException {
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(line);
            contentStream.endText();
        }

        private List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            String sanitized = sanitizeForFont(text, font, fontSize);
            String trimmed = sanitized.trim();
            if (trimmed.isEmpty()) {
                return List.of("");
            }

            String[] words = trimmed.split("\\s+");
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            for (String word : words) {
                if (current.length() == 0) {
                    if (stringWidth(font, fontSize, word) <= maxWidth) {
                        current.append(word);
                        continue;
                    }

                    List<String> broken = breakLongToken(word, font, fontSize, maxWidth);
                    lines.addAll(broken);
                    continue;
                }

                String candidate = current + " " + word;
                if (stringWidth(font, fontSize, candidate) <= maxWidth) {
                    current.append(' ').append(word);
                    continue;
                }

                lines.add(current.toString());
                current.setLength(0);

                if (stringWidth(font, fontSize, word) <= maxWidth) {
                    current.append(word);
                } else {
                    List<String> broken = breakLongToken(word, font, fontSize, maxWidth);
                    lines.addAll(broken);
                }
            }

            if (current.length() > 0) {
                lines.add(current.toString());
            }

            return lines;
        }

        private List<String> breakLongToken(String token, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            int offset = 0;
            while (offset < token.length()) {
                int codePoint = token.codePointAt(offset);
                String ch = new String(Character.toChars(codePoint));
                offset += Character.charCount(codePoint);

                String candidate = current + ch;
                if (stringWidth(font, fontSize, candidate) <= maxWidth) {
                    current.append(ch);
                    continue;
                }

                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }

                if (stringWidth(font, fontSize, ch) <= maxWidth) {
                    current.append(ch);
                } else {
                    parts.add("?");
                }
            }

            if (current.length() > 0) {
                parts.add(current.toString());
            }

            return parts;
        }

        private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
            return (font.getStringWidth(text) / 1000f) * fontSize;
        }

        private String sanitizeForFont(String text, PDFont font, float fontSize) {
            if (text == null) return "";

            StringBuilder sb = new StringBuilder();
            int offset = 0;
            while (offset < text.length()) {
                int codePoint = text.codePointAt(offset);
                String ch = new String(Character.toChars(codePoint));
                offset += Character.charCount(codePoint);

                try {
                    // will throw for unsupported glyphs in the given font/encoding
                    font.getStringWidth(ch);
                    sb.append(ch);
                } catch (Exception e) {
                    sb.append('?');
                }
            }

            String sanitized = sb.toString();
            try {
                font.getStringWidth(sanitized);
                return sanitized;
            } catch (Exception e) {
                return sanitized.replaceAll("[^\\x20-\\x7E]", "?");
            }
        }

        @Override
        public void close() throws IOException {
            closeQuietly(contentStream);
        }

        private static void closeQuietly(Closeable closeable) throws IOException {
            if (closeable != null) {
                closeable.close();
            }
        }
    }
}
