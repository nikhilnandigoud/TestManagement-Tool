package com.qaautomation.services;

import com.qaautomation.agents.TextExtractionAgent;
import com.qaautomation.models.UploadedFile;
import com.qaautomation.repositories.UploadedFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service layer for file extraction operations.
 */
@Slf4j
@Service
public class FileExtractionService {

    private final UploadedFileRepository uploadedFileRepository;
    private final TextExtractionAgent textExtractionAgent;

    public FileExtractionService(
        UploadedFileRepository uploadedFileRepository,
        TextExtractionAgent textExtractionAgent) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.textExtractionAgent = textExtractionAgent;
    }

    /**
     * Extracts text from uploaded file.
     */
    public UploadedFile extractTextFromFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        long fileSize = file.getSize();

        UploadedFile uploadedFile = UploadedFile.builder()
            .originalFilename(filename)
            .fileType(contentType)
            .fileSizeBytes(fileSize)
            .status(UploadedFile.ExtractionStatus.PROCESSING)
            .uploadedAt(LocalDateTime.now())
            .build();

        try {
            String extractedText = extractTextByType(file, contentType);
            
            uploadedFile.setExtractedText(extractedText);
            uploadedFile.setLanguage(textExtractionAgent.detectLanguage(extractedText));
            uploadedFile.setConfidenceScore(
                textExtractionAgent.calculateConfidenceScore(
                    new String(file.getBytes()),
                    extractedText
                )
            );
            uploadedFile.setStatus(UploadedFile.ExtractionStatus.SUCCESS);
            uploadedFile.setProcessedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error extracting text from file {}: {}", filename, e.getMessage(), e);
            uploadedFile.setStatus(UploadedFile.ExtractionStatus.FAILED);
            uploadedFile.setErrorMessage(e.getMessage());
        }

        return uploadedFileRepository.save(uploadedFile);
    }

    /**
     * Routes extraction based on file type.
     */
    private String extractTextByType(MultipartFile file, String contentType) throws IOException {
        if (contentType == null) {
            contentType = "";
        }

        if (contentType.contains("pdf")) {
            return textExtractionAgent.extractFromPdf(file);
        } else if (contentType.contains("word") || contentType.contains("document")) {
            return textExtractionAgent.extractFromDocx(file);
        } else if (contentType.contains("text") || contentType.contains("plain")) {
            return textExtractionAgent.extractFromText(file);
        } else {
            // Try to detect by extension
            String filename = file.getOriginalFilename();
            if (filename != null) {
                if (filename.endsWith(".pdf")) {
                    return textExtractionAgent.extractFromPdf(file);
                } else if (filename.endsWith(".docx") || filename.endsWith(".doc")) {
                    return textExtractionAgent.extractFromDocx(file);
                }
            }
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    /**
     * Retrieves extracted file by ID.
     */
    public Optional<UploadedFile> getUploadedFile(String fileId) {
        return uploadedFileRepository.findById(fileId);
    }
}
