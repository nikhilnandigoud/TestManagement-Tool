package com.qaautomation.dto;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationStartDTO {
    private String scenario;
    private List<String> guides;
    private List<String> uploadedFiles;
}
