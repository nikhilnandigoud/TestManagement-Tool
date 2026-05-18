package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore
    private Conversation conversation;
    
    @Column(nullable = false, length = 50)
    private String role; // 'user' or 'assistant'
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 50)
    private String messageType = "CHAT"; // CHAT, ACTION, SYSTEM
    
    @Column(length = 100)
    private String actionType; // regenerate, modify, add_more, merge, delete
    
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
