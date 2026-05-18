package com.qaautomation.agents;

import com.qaautomation.models.UserGuideContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserGuideProcessor - Loads and extracts text from PDF and DOCX user guide documents.
 * Provides caching and utility methods for guide content retrieval.
 */
@Slf4j
@Component
public class UserGuideProcessor {

    private static final String GUIDES_FOLDER = "src/main/resources/user-guides";
    private static final int MAX_CONTENT_LENGTH = 50000; // Limit extracted content for LLM prompt

    // Cache for loaded guides to avoid re-processing
    private final Map<String, UserGuideContext> guideCache = new HashMap<>();

    /**
     * Load a user guide by filename. Checks cache first, then loads from disk.
     *
     * @param fileName The name of the file (e.g., "Decision_Training_for_Release_Managers.pdf")
     * @return UserGuideContext with extracted content, or null if file not found
     */
    public UserGuideContext loadUserGuide(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            log.warn("Empty fileName provided to loadUserGuide");
            return null;
        }

        // Check cache first
        if (guideCache.containsKey(fileName)) {
            log.debug("Returning cached guide: {}", fileName);
            return guideCache.get(fileName);
        }

        try {
            File guideFile = new File(GUIDES_FOLDER, fileName);

            if (!guideFile.exists()) {
                log.warn("Guide file not found: {}", guideFile.getAbsolutePath());
                return null;
            }

            UserGuideContext context = null;

            if (fileName.endsWith(".pdf")) {
                context = extractFromPdf(guideFile);
            } else if (fileName.endsWith(".docx")) {
                context = extractFromDocx(guideFile);
            } else {
                log.warn("Unsupported file format: {}", fileName);
                return null;
            }

            if (context != null && context.isValid()) {
                guideCache.put(fileName, context);
                log.info("Successfully loaded and cached guide: {} ({} chars)", fileName, context.getContentLength());
            }

            return context;

        } catch (Exception e) {
            log.error("Error loading user guide: {}", fileName, e);
            return null;
        }
    }

    /**
     * Extract text from a PDF file.
     */
    private UserGuideContext extractFromPdf(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("No text extracted from PDF: {}", pdfFile.getName());
                return createInvalidContext(pdfFile, "PDF extraction resulted in empty content");
            }

            // Truncate if too long
            String contentToUse = extractedText.length() > MAX_CONTENT_LENGTH 
                ? extractedText.substring(0, MAX_CONTENT_LENGTH) 
                : extractedText;

            return UserGuideContext.builder()
                .guideFileName(pdfFile.getName())
                .guideContent(contentToUse)
                .filePath(pdfFile.getAbsolutePath())
                .loadedAt(LocalDateTime.now())
                .contentLength(contentToUse.length())
                .isValid(true)
                .module(extractModuleNameFromFileName(pdfFile.getName()))
                .description("PDF User Guide")
                .build();

        } catch (Exception e) {
            log.error("Error extracting text from PDF: {}", pdfFile.getName(), e);
            return createInvalidContext(pdfFile, e.getMessage());
        }
    }

    /**
     * Extract text from a DOCX file.
     */
    private UserGuideContext extractFromDocx(File docxFile) {
        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder extractedText = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    extractedText.append(text).append("\n");
                }
            }

            String contentStr = extractedText.toString().trim();

            if (contentStr.isEmpty()) {
                log.warn("No text extracted from DOCX: {}", docxFile.getName());
                return createInvalidContext(docxFile, "DOCX extraction resulted in empty content");
            }

            // Truncate if too long
            String contentToUse = contentStr.length() > MAX_CONTENT_LENGTH 
                ? contentStr.substring(0, MAX_CONTENT_LENGTH) 
                : contentStr;

            return UserGuideContext.builder()
                .guideFileName(docxFile.getName())
                .guideContent(contentToUse)
                .filePath(docxFile.getAbsolutePath())
                .loadedAt(LocalDateTime.now())
                .contentLength(contentToUse.length())
                .isValid(true)
                .module(extractModuleNameFromFileName(docxFile.getName()))
                .description("DOCX User Guide")
                .build();

        } catch (Exception e) {
            log.error("Error extracting text from DOCX: {}", docxFile.getName(), e);
            return createInvalidContext(docxFile, e.getMessage());
        }
    }

    /**
     * Extract module name from filename (e.g., "Decision Manager" from "Decision Manager (DM) User Guide 7.11.docx")
     */
    private String extractModuleNameFromFileName(String fileName) {
        // Try to extract meaningful part before "User Guide" or version numbers
        if (fileName.toLowerCase().contains("decision")) {
            return "Decision Manager";
        } else if (fileName.toLowerCase().contains("training")) {
            return "Training";
        }
        return fileName.replaceAll("\\.(pdf|docx)$", "");
    }

    /**
     * List all available folders containing user guides.
     * Returns a map of folder name to list of files in each folder.
     */
    public Map<String, List<String>> listAvailableGuideFolders() {
        try {
            Path guidePath = Paths.get(GUIDES_FOLDER);
            if (!Files.exists(guidePath)) {
                log.warn("Guides folder does not exist: {}", guidePath.toAbsolutePath());
                return new HashMap<>();
            }

            Map<String, List<String>> folders = new HashMap<>();

            // List all directories in the guides folder
            Files.list(guidePath)
                .filter(Files::isDirectory)
                .forEach(folderPath -> {
                    try {
                        String folderName = folderPath.getFileName().toString();
                        // Get all PDF and DOCX files in this folder
                        List<String> files = Files.list(folderPath)
                            .filter(f -> f.toString().endsWith(".pdf") || f.toString().endsWith(".docx"))
                            .map(f -> f.getFileName().toString())
                            .sorted()
                            .collect(Collectors.toList());
                        
                        if (!files.isEmpty()) {
                            folders.put(folderName, files);
                            log.debug("Folder: {} contains {} files", folderName, files.size());
                        }
                    } catch (Exception e) {
                        log.warn("Error listing files in folder: {}", folderPath.getFileName(), e);
                    }
                });

            return folders;

        } catch (Exception e) {
            log.error("Error listing available guide folders", e);
            return new HashMap<>();
        }
    }

    /**
     * Load all user guides from a specific folder and combine their content.
     * 
     * @param folderName The name of the folder (e.g., "bestPractices", "DMDEDT")
     * @return Combined UserGuideContext with content from all files in the folder
     */
    public UserGuideContext loadUserGuidesFromFolder(String folderName) {
        if (folderName == null || folderName.isEmpty()) {
            log.warn("Empty folderName provided to loadUserGuidesFromFolder");
            return null;
        }

        // Check cache first
        String cacheKey = "folder_" + folderName;
        if (guideCache.containsKey(cacheKey)) {
            log.debug("Returning cached folder guides: {}", folderName);
            return guideCache.get(cacheKey);
        }

        try {
            Path folderPath = Paths.get(GUIDES_FOLDER, folderName);

            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                log.warn("Guide folder not found: {}", folderPath.toAbsolutePath());
                return null;
            }

            StringBuilder combinedContent = new StringBuilder();
            List<String> loadedFiles = new java.util.ArrayList<>();
            int totalContentLength = 0;

            // Load all PDF and DOCX files from the folder
            List<File> guideFiles = Files.list(folderPath)
                .filter(f -> f.toString().endsWith(".pdf") || f.toString().endsWith(".docx"))
                .map(Path::toFile)
                .sorted()
                .collect(Collectors.toList());

            for (File guideFile : guideFiles) {
                try {
                    UserGuideContext context = null;
                    
                    if (guideFile.getName().endsWith(".pdf")) {
                        context = extractFromPdf(guideFile);
                    } else if (guideFile.getName().endsWith(".docx")) {
                        context = extractFromDocx(guideFile);
                    }

                    if (context != null && context.isValid()) {
                        if (combinedContent.length() > 0) {
                            combinedContent.append("\n\n--- FROM: ").append(guideFile.getName()).append(" ---\n\n");
                        } else {
                            combinedContent.append("--- FROM: ").append(guideFile.getName()).append(" ---\n\n");
                        }
                        combinedContent.append(context.getGuideContent());
                        loadedFiles.add(guideFile.getName());
                        totalContentLength += context.getContentLength();
                        log.debug("Loaded file from folder: {}", guideFile.getName());
                    }
                } catch (Exception e) {
                    log.warn("Error loading file {}: {}", guideFile.getName(), e.getMessage());
                }
            }

            if (combinedContent.length() == 0) {
                log.warn("No valid guides found in folder: {}", folderName);
                return createInvalidContext(new File(folderPath.toString()), 
                    "No valid guide files found in folder");
            }

            // Truncate if too long
            String contentToUse = combinedContent.length() > MAX_CONTENT_LENGTH 
                ? combinedContent.substring(0, MAX_CONTENT_LENGTH) 
                : combinedContent.toString();

            UserGuideContext folderContext = UserGuideContext.builder()
                .guideFileName(folderName + " [Folder with " + loadedFiles.size() + " files]")
                .guideContent(contentToUse)
                .filePath(folderPath.toAbsolutePath().toString())
                .loadedAt(LocalDateTime.now())
                .contentLength(contentToUse.length())
                .isValid(true)
                .module(folderName)
                .description("Folder containing " + loadedFiles.size() + " guide files: " + String.join(", ", loadedFiles))
                .build();

            guideCache.put(cacheKey, folderContext);
            log.info("Successfully loaded and cached {} files from folder: {} ({} chars)", 
                loadedFiles.size(), folderName, contentToUse.length());

            return folderContext;

        } catch (Exception e) {
            log.error("Error loading guides from folder: {}", folderName, e);
            return null;
        }
    }

    /**
     * List all available user guides in the guides folder (legacy support for backward compatibility).
     */
    public Map<String, String> listAvailableGuides() {
        try {
            Path guidePath = Paths.get(GUIDES_FOLDER);
            if (!Files.exists(guidePath)) {
                log.warn("Guides folder does not exist: {}", guidePath.toAbsolutePath());
                return new HashMap<>();
            }

            return Files.list(guidePath)
                .filter(path -> path.toString().endsWith(".pdf") || path.toString().endsWith(".docx"))
                .collect(Collectors.toMap(
                    path -> path.getFileName().toString(),
                    path -> extractModuleNameFromFileName(path.getFileName().toString())
                ));

        } catch (Exception e) {
            log.error("Error listing available guides", e);
            return new HashMap<>();
        }
    }

    /**
     * Clear all cached guides.
     */
    public void clearCache() {
        guideCache.clear();
        log.info("User guide cache cleared");
    }

    /**
     * Clear a specific guide from cache.
     */
    public void clearCacheEntry(String fileName) {
        guideCache.remove(fileName);
        log.info("Cleared cache entry for: {}", fileName);
    }

    /**
     * Get a summary/preview of the guide content (first 500 chars).
     */
    public String getGuidePreview(String fileName) {
        UserGuideContext context = loadUserGuide(fileName);
        if (context == null || !context.isValid()) {
            return null;
        }

        String content = context.getGuideContent();
        return content.length() > 500 ? content.substring(0, 500) + "..." : content;
    }

    /**
     * Helper method to create an invalid context.
     */
    private UserGuideContext createInvalidContext(File file, String errorMsg) {
        return UserGuideContext.builder()
            .guideFileName(file.getName())
            .filePath(file.getAbsolutePath())
            .loadedAt(LocalDateTime.now())
            .isValid(false)
            .errorMessage(errorMsg)
            .contentLength(0)
            .build();
    }
}
