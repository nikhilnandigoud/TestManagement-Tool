package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_case_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseEmbedding {
    @Id
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    @JsonIgnore
    private TestCase testCase;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore
    private Conversation conversation;
    
    @Column(length = 100)
    private String embeddingModel = "text-embedding-3-small";
    
    @Column
    private Integer embeddingDimensions = 1536;
    
    /**
     * Local fallback storage for embeddings.
     * Uses PostgreSQL REAL[] so local development does not require pgvector.
     */
    @Column(columnDefinition = "REAL[]")
    private float[] titleEmbedding;
    
    @Column(columnDefinition = "REAL[]")
    private float[] stepsEmbedding;
    
    @Column(columnDefinition = "REAL[]")
    private float[] resultsEmbedding;
    
    @Column(columnDefinition = "REAL[]")
    private float[] combinedEmbedding;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
