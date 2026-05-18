package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "chat_message_id", nullable = false)
    @JsonIgnore
    private ChatMessage chatMessage;
    
    @Column(nullable = false, length = 255)
    private String originalFileName;
    
    @Column(nullable = false, length = 500)
    private String filePath; // Where the file is stored
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false, length = 50)
    private String fileType; // pdf, docx, pgf, doc, jpg, png, etc.
    
    @Column(nullable = false, length = 50)
    private String category; // IMAGE, DOCUMENT, SPREADSHEET, etc.
    
    @Column(columnDefinition = "TEXT")
    private String extractedText; // OCR or text extraction from file
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
