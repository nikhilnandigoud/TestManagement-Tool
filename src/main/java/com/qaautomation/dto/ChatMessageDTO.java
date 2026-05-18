package com.qaautomation.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private String id;
    private String role; // user or assistant
    private String content;
    private String timestamp;
    private Boolean isActionable;
    private RAGStatsDTO ragStats;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class RAGStatsDTO {
    private Integer vectorHits;
    private Integer guidesUsed;
}
