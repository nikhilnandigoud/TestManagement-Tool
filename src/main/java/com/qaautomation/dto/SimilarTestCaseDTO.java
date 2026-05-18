package com.qaautomation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimilarTestCaseDTO {
    private String testCaseId;
    private String title;
    private Float similarityScore;
    private String conversationId;
}
