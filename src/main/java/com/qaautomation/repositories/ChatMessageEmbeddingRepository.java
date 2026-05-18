package com.qaautomation.repositories;

import com.qaautomation.models.ChatMessageEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface ChatMessageEmbeddingRepository extends JpaRepository<ChatMessageEmbedding, UUID> {
    Optional<ChatMessageEmbedding> findByChatMessageId(UUID chatMessageId);
    
    @Query(value = "SELECT * FROM chat_message_embeddings WHERE conversation_id = :conversationId", nativeQuery = true)
    List<ChatMessageEmbedding> findByConversationId(@Param("conversationId") UUID conversationId);
    
    /**
     * Local fallback query. Similarity scoring can be performed in Java.
     */
    @Query(value = "SELECT * FROM chat_message_embeddings cme " +
            "WHERE conversation_id = :conversationId " +
            "AND CAST(:queryEmbedding AS text) IS NOT NULL " +
            "AND :threshold <= 1 " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<ChatMessageEmbedding> findSimilarMessages(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("conversationId") UUID conversationId,
            @Param("threshold") Double threshold,
            @Param("limit") Integer limit
    );
}
