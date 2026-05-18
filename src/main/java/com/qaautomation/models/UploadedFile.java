package com.qaautomation.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Data model for storing uploaded files and their extracted text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "uploaded_files")
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String originalFilename;
    
    private String fileType;
    
    private long fileSizeBytes;
    
    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;
    
    private String language;
    
    private Double confidenceScore;
    
    private Integer pageCount;
    
    @Enumerated(EnumType.STRING)
    private ExtractionStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private LocalDateTime uploadedAt;
    
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        this.uploadedAt = LocalDateTime.now();
    }

    public enum ExtractionStatus {
        PENDING, PROCESSING, SUCCESS, FAILED, OCR_REQUIRED
    }
}
