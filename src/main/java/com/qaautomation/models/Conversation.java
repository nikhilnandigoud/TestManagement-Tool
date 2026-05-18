package com.qaautomation.models;

import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    @Id
    private UUID id;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String scenarioText;
    
    @Column(length = 255)
    private String userId;
    
    @Column(length = 50)
    private String status = "ACTIVE";
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestCase> testCases = new ArrayList<>();
    
    @OneToOne(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ConversationContext context;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
