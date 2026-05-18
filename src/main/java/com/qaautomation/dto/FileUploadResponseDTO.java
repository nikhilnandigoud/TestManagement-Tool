package com.qaautomation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponseDTO {
    private String id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String category;
    private String message;
    private boolean success;
}
