package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data model representing a test case generated from requirements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "test_cases")
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String preconditions;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_case_steps", joinColumns = @JoinColumn(name = "test_case_id"))
    private List<Step> steps;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_case_expected_results", joinColumns = @JoinColumn(name = "test_case_id"))
    private List<String> expectedResults;
    
    @Enumerated(EnumType.STRING)
    private Priority priority;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_case_tags", joinColumns = @JoinColumn(name = "test_case_id"))
    private List<String> tags;
    
    private String requirementId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conversation_id")
    @JsonIgnore
    private Conversation conversation;
    
    private String appName;  // Application name for filtering
    
    private String module;   // Module/Feature name for filtering
    
    private Double estimatedComplexity;
    
    @Column(columnDefinition = "TEXT")
    private String testDataHints;
    
    private String status;  // Status of the test case (Draft, Active, etc.)
    
    private Integer version;  // Version number for tracking changes
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}
