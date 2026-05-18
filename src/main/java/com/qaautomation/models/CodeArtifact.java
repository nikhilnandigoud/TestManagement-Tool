package com.qaautomation.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data model for generated automation code artifacts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "code_artifacts")
public class CodeArtifact {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String scenarioId;
    
    @Enumerated(EnumType.STRING)
    private Framework framework;
    
    @Enumerated(EnumType.STRING)
    private Language language;
    
    @Column(columnDefinition = "LONGTEXT")
    private String code;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "code_artifact_files", joinColumns = @JoinColumn(name = "artifact_id"))
    private List<CodeFile> files;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "code_artifact_dependencies", joinColumns = @JoinColumn(name = "artifact_id"))
    private List<String> dependencies;
    
    private Double confidence;
    
    private Boolean requiresHumanReview;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    private LocalDateTime generatedAt;

    @PrePersist
    public void prePersist() {
        this.generatedAt = LocalDateTime.now();
    }

    public enum Framework {
        SELENIUM, PLAYWRIGHT, CYPRESS, WEBDRIVER_IO
    }

    public enum Language {
        JAVA, JAVASCRIPT, TYPESCRIPT, PYTHON
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeFile {
        private String filePath;
        @Column(columnDefinition = "LONGTEXT")
        private String content;
    }
}
