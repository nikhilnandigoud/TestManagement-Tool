package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_context")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationContext {
    @Id
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    @JsonIgnore
    private Conversation conversation;
    
    @Column(columnDefinition = "TEXT")
    private String activeScenario;
    
    @Column(columnDefinition = "TEXT")
    private String activeGuides; // JSON as string
    
    @Column(columnDefinition = "TEXT")
    private String uploadedFiles; // JSON as string
    
    @Column
    private Integer vectorContextHits = 0;
    
    @Column
    private LocalDateTime lastQueryTime;
    
    @Column(columnDefinition = "TEXT")
    private String embeddingStats; // JSON as string
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
