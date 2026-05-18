package com.qaautomation.dto;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponseDTO {
    private String id;
    private String title;
    private List<ChatResponseDTO> messages;
    private List<TestCaseDTO> testCases;
    private String status;
    private String createdAt;
}
