package com.qaautomation.agents;

import com.qaautomation.models.UploadedFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Text Extraction Agent - Converts uploaded files (PDF/DOCX/TXT) to cleaned text.
 * Responsibilities:
 * - Accept file uploads
 * - Extract and clean text
 * - Remove headers/footers, page numbers, boilerplate
 * - Return cleaned text with metadata
 */
@Slf4j
@Component
public class TextExtractionAgent implements Agent {

    @Override
    public String getDescription() {
        return "Extracts and cleans text from PDF, DOCX, and TXT files";
    }

    @Override
    public boolean canHandle(Object input) {
        if (!(input instanceof MultipartFile file)) {
            return false;
        }
        String contentType = file.getContentType();
        return contentType != null && (
            contentType.contains("pdf") ||
            contentType.contains("word") ||
            contentType.contains("text") ||
            contentType.contains("document")
        );
    }

    @Override
    public boolean execute() {
        // Implementation handled by service layer
        return true;
    }

    /**
     * Extracts text from a PDF file.
     */
    public String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return cleanText(text);
        }
    }

    /**
     * Extracts text from a DOCX file.
     */
    public String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            return cleanText(
                document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"))
            );
        }
    }

    /**
     * Extracts text from a plain text file.
     */
    public String extractFromText(MultipartFile file) throws IOException {
        String text = new String(file.getBytes());
        return cleanText(text);
    }

    /**
     * Cleans extracted text by removing headers, footers, page numbers, and normalizing whitespace.
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        // Remove page numbers (e.g., "— Page 5 —" or just numbers at line ends)
        text = text.replaceAll("(?m)^.*?\\bPage\\s*\\d+\\b.*?$", "");
        text = text.replaceAll("(?m)^\\s*\\d+\\s*$", "");

        // Remove multiple consecutive newlines
        text = text.replaceAll("\n{3,}", "\n\n");

        // Normalize whitespace
        text = text.replaceAll("\\s+", " ");
        text = text.replaceAll(" +\n", "\n");
        text = text.replaceAll("\n +", "\n");

        // Trim each line
        text = text.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining("\n"));

        return text.trim();
    }

    /**
     * Detects language of the text (simplified - returns "en" for now).
     */
    public String detectLanguage(String text) {
        // TODO: Implement proper language detection using a library
        return "en";
    }

    /**
     * Calculates extraction confidence score (0-1).
     */
    public double calculateConfidenceScore(String originalText, String cleanedText) {
        if (originalText == null || originalText.isEmpty()) {
            return 0.0;
        }
        
        // Simple heuristic: if most text was preserved, confidence is high
        double ratio = (double) cleanedText.length() / originalText.length();
        return Math.min(1.0, Math.max(0.0, ratio));
    }
}
