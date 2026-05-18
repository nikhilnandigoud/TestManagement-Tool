package com.qaautomation.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDTO {
    private String conversationId;
    private String message;
    private String actionType; // regenerate, modify, add_more, merge, delete
    private List<String> guides; // Active guides selected by user
    private List<String> attachmentIds; // UUIDs of uploaded attachments
}
