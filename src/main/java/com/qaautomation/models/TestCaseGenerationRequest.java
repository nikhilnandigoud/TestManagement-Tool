package com.qaautomation.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for test case generation from requirements text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseGenerationRequest {
    // New/primary fields (kept for compatibility with newer clients)
    private String requirementsText;
    private String applicationName;
    private String moduleName;
    private String testCaseType; // "functional", "regression", "smoke", etc.
    private int numberOfTestCases; // Suggested number

    // Backwards-compatible fields (older controllers/services expect these getters)
    private String text;
    private String fileId;
    private String requirementId;
    private RequestContext context;

    // User Guide reference for enriched test case generation
    private String userGuideFileName; // e.g., "Decision_Training_for_Release_Managers.pdf" (single guide, for backwards compatibility)
    private List<String> userGuideFileNames; // Multiple guides for comprehensive test case generation
    private String userGuideFolderName; // Single folder (legacy)
    private List<String> userGuideFolderNames; // Multiple folders for comprehensive test case generation

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequestContext {
        private String appName;
        private String module;
        private String priorityHint;
    }
}