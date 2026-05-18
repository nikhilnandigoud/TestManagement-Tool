package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_message_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEmbedding {
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false)
    @JsonIgnore
    private ChatMessage chatMessage;
    
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
    private float[] contentEmbedding;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
