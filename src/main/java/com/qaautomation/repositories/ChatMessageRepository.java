package com.qaautomation.repositories;

import com.qaautomation.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversation.id = :conversationId AND cm.role = :role")
    List<ChatMessage> findByConversationAndRole(
        @Param("conversationId") UUID conversationId,
        @Param("role") String role
    );
}
