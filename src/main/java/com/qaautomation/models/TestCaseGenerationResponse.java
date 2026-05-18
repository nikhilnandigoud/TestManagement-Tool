package com.qaautomation.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for API responses containing generated test cases.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseGenerationResponse {
    private String requestId;
    private String status;
    private List<TestCase> testCases;
    private Integer totalGenerated;
    private String timestamp;
    private String message;  // Optional message (e.g., which guide was used)
}
