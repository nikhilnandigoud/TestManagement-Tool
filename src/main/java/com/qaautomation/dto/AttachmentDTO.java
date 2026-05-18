package com.qaautomation.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentDTO {
    private String id;
    private String originalFileName;
    private String fileType;
    private String category;
    private Long fileSize;
    private String extractedText;
    private LocalDateTime uploadedAt;
}
