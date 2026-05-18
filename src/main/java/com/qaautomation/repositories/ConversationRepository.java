package com.qaautomation.repositories;

import com.qaautomation.models.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Conversation> findById(UUID id);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = 'ACTIVE' ORDER BY c.updatedAt DESC")
    List<Conversation> findAllActiveConversations();
}
