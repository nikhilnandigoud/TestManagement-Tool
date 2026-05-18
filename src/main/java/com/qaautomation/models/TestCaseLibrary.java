package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data model representing a test case saved to the personal library.
 * Extends TestCase with library-specific metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "test_case_library")
public class TestCaseLibrary {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Reference to the original test case
    private String testCaseId;

    // Library metadata
    private String libraryTitle;  // Custom title for library storage
    
    @Column(columnDefinition = "TEXT")
    private String libraryDescription;  // Description for quick reference
    
    @Enumerated(EnumType.STRING)
    private TestCaseCategory category;  // Category: Positive, Negative, Edge Case
    
    @Enumerated(EnumType.STRING)
    private TestCase.Priority priority;  // Priority: HIGH, MEDIUM, LOW
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "library_tags", joinColumns = @JoinColumn(name = "library_id"))
    private List<String> tags;  // Tags for searching and filtering
    
    @Column(columnDefinition = "TEXT")
    private String qualityScore;  // Quality score as percentage string (e.g., "82%")
    
    private Boolean isPublic;  // Public/Private indicator
    
    private String createdBy;  // User who saved
    
    private LocalDateTime savedToLibraryAt;
    
    private LocalDateTime updatedAt;
    
    private Integer usageCount;  // How many times this test case was used/reused
    
    @Column(columnDefinition = "TEXT")
    private String notes;  // Additional notes about the test case

    @PrePersist
    public void prePersist() {
        this.savedToLibraryAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.usageCount = 0;
        if (this.isPublic == null) {
            this.isPublic = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TestCaseCategory {
        POSITIVE("Positive Test Case"),
        NEGATIVE("Negative Test Case"),
        EDGE_CASE("Edge Case Test Case"),
        BOUNDARY("Boundary Test Case"),
        SECURITY("Security Test Case"),
        PERFORMANCE("Performance Test Case");

        private final String displayName;

        TestCaseCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
