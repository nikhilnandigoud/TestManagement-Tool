package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for API responses containing extracted text and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileExtractionResponse {
    private String fileId;
    private String text;
    private FileMetadata metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileMetadata {
        private String filename;
        private Integer pages;
        private String language;
        private Double confidence;
        private String extractionStatus;
    }
}
