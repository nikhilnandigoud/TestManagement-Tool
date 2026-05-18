package com.qaautomation.models;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
/**
 * Response DTO for API responses containing generated automation code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeGenerationResponse {
    private String requestId;
    private String status;
    private CodeArtifactDto artifact;
    private String timestamp;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodeArtifactDto {
        private String code;
        private List<CodeFile> files;
        private List<String> requiredDependencies;
        private Double confidence;
        private Boolean requiresHumanReview;
        private String notes;
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CodeFile {
            private String path;
            private String content;
        }
    }
}