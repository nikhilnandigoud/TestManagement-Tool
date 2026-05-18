package com.qaautomation.controllers;

import com.qaautomation.models.FileExtractionResponse;
import com.qaautomation.models.UploadedFile;
import com.qaautomation.services.FileExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for file extraction endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/files")
@CrossOrigin(origins = "*")
public class FileExtractionController {

    private final FileExtractionService fileExtractionService;

    public FileExtractionController(FileExtractionService fileExtractionService) {
        this.fileExtractionService = fileExtractionService;
    }

    /**
     * POST /files/extract - Uploads a file and extracts its text.
     */
    @PostMapping("/extract")
    public ResponseEntity<?> extractFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Receiving file upload: {}", file.getOriginalFilename());

            UploadedFile uploadedFile = fileExtractionService.extractTextFromFile(file);

            FileExtractionResponse response = FileExtractionResponse.builder()
                .fileId(uploadedFile.getId())
                .text(uploadedFile.getExtractedText())
                .metadata(FileExtractionResponse.FileMetadata.builder()
                    .filename(uploadedFile.getOriginalFilename())
                    .pages(uploadedFile.getPageCount())
                    .language(uploadedFile.getLanguage())
                    .confidence(uploadedFile.getConfidenceScore())
                    .extractionStatus(uploadedFile.getStatus().toString())
                    .build())
                .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error extracting file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to extract file: " + e.getMessage()));
        }
    }

    /**
     * GET /files/{fileId} - Retrieves extracted file data.
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<?> getFile(@PathVariable String fileId) {
        try {
            var uploadedFile = fileExtractionService.getUploadedFile(fileId);

            if (uploadedFile.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            UploadedFile file = uploadedFile.get();
            FileExtractionResponse response = FileExtractionResponse.builder()
                .fileId(file.getId())
                .text(file.getExtractedText())
                .metadata(FileExtractionResponse.FileMetadata.builder()
                    .filename(file.getOriginalFilename())
                    .pages(file.getPageCount())
                    .language(file.getLanguage())
                    .confidence(file.getConfidenceScore())
                    .extractionStatus(file.getStatus().toString())
                    .build())
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving file {}: {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve file"));
        }
    }

    /**
     * Error response DTO.
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
