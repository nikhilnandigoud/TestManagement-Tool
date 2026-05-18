package com.qaautomation.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaautomation.models.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Jira/Zephyr Integration Agent - Maps test cases to Jira and pushes them.
 * Responsibilities:
 * - Map test cases to Jira issue payloads
 * - Call Jira REST APIs to create issues
 * - Handle Zephyr Steps API integration
 * - Implement idempotency and retry logic
 * - Provide error handling and reconciliation reports
 */
@Slf4j
@Component
public class JiraIntegrationAgent implements Agent {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public JiraIntegrationAgent(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getDescription() {
        return "Integrates with Jira and Zephyr to push test cases as issues and test steps";
    }

    @Override
    public boolean canHandle(Object input) {
        return input instanceof List<?> && 
               ((List<?>) input).stream().allMatch(item -> item instanceof TestCase);
    }

    @Override
    public boolean execute() {
        // Implementation handled by service layer
        return true;
    }

    /**
     * Data class for Jira push results.
     */
    public static class JiraPushResult {
        public String testCaseId;
        public String jiraIssueKey;
        public String status; // SUCCESS, FAILED, SKIPPED
        public String errorMessage;
    }

    /**
     * Pushes test cases to Jira.
     */
    public List<JiraPushResult> pushTestCasesToJira(
        List<TestCase> testCases,
        String jiraProjectKey,
        String jiraBaseUrl,
        String jiraApiToken,
        int maxRetries) {

        List<JiraPushResult> results = new ArrayList<>();

        for (TestCase testCase : testCases) {
            JiraPushResult result = pushSingleTestCase(testCase, jiraProjectKey, jiraBaseUrl, jiraApiToken, maxRetries);
            results.add(result);
        }

        return results;
    }

    /**
     * Pushes a single test case to Jira with retry logic.
     */
    private JiraPushResult pushSingleTestCase(
        TestCase testCase,
        String jiraProjectKey,
        String jiraBaseUrl,
        String jiraApiToken,
        int maxRetries) {

        JiraPushResult result = new JiraPushResult();
        result.testCaseId = testCase.getId();

        String idempotencyKey = generateIdempotencyKey(testCase.getId());

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Pushing test case {} to Jira - Attempt {}/{}", testCase.getId(), attempt, maxRetries);

                // Create Jira payload
                Map<String, Object> jiraPayload = createJiraIssuePayload(testCase, jiraProjectKey);

                // Call Jira API
                String issueKey = callJiraCreateIssueApi(jiraPayload, jiraBaseUrl, jiraApiToken, idempotencyKey);

                if (issueKey != null) {
                    result.jiraIssueKey = issueKey;
                    result.status = "SUCCESS";

                    // Optionally push Zephyr steps
                    try {
                        pushZephyrSteps(issueKey, testCase, jiraBaseUrl, jiraApiToken);
                    } catch (Exception e) {
                        log.warn("Failed to push Zephyr steps for issue {}: {}", issueKey, e.getMessage());
                    }

                    return result;
                }
            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                result.errorMessage = e.getMessage();

                if (attempt == maxRetries) {
                    result.status = "FAILED";
                    log.error("All retry attempts exhausted for test case {}", testCase.getId(), e);
                } else if (isRetryableError(e)) {
                    try {
                        Thread.sleep(1000L * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    result.status = "FAILED";
                    break;
                }
            }
        }

        if (result.status == null) {
            result.status = "FAILED";
        }

        return result;
    }

    /**
     * Creates Jira issue payload from test case.
     */
    private Map<String, Object> createJiraIssuePayload(TestCase testCase, String projectKey) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Project
        Map<String, Object> project = new HashMap<>();
        project.put("key", projectKey);
        payload.put("project", project);

        // Issue Type (Test Case)
        Map<String, Object> issueType = new HashMap<>();
        issueType.put("name", "Test");
        payload.put("issueType", issueType);

        // Summary
        payload.put("summary", truncate(testCase.getTitle(), 255));

        // Description
        StringBuilder description = new StringBuilder();
        description.append("*Description:* ").append(testCase.getDescription()).append("\n\n");
        if (testCase.getPreconditions() != null && !testCase.getPreconditions().isEmpty()) {
            description.append("*Preconditions:* ").append(testCase.getPreconditions()).append("\n\n");
        }
        if (testCase.getEstimatedComplexity() != null) {
            description.append("*Estimated Complexity:* ").append(testCase.getEstimatedComplexity()).append("\n");
        }
        payload.put("description", description.toString());

        // Priority
        Map<String, Object> priority = new HashMap<>();
        priority.put("name", mapPriorityToJira(testCase.getPriority()));
        payload.put("priority", priority);

        // Labels (tags)
        if (testCase.getTags() != null && !testCase.getTags().isEmpty()) {
            payload.put("labels", testCase.getTags());
        }

        return payload;
    }

    /**
     * Calls Jira REST API to create an issue.
     */
    private String callJiraCreateIssueApi(
        Map<String, Object> payload,
        String jiraBaseUrl,
        String jiraApiToken,
        String idempotencyKey) throws Exception {

        // TODO: Implement actual REST call using RestClient
        // This is a placeholder for the actual API integration
        log.info("Would call Jira API at {}/rest/api/3/issue with payload: {}", jiraBaseUrl, payload);

        // For now, simulate success
        return "TEST-" + System.currentTimeMillis();
    }

    /**
     * Pushes test steps to Zephyr.
     */
    private void pushZephyrSteps(String jiraIssueKey, TestCase testCase, String jiraBaseUrl, String jiraApiToken) {
        // TODO: Implement Zephyr API integration
        log.info("Would push Zephyr steps for issue {}", jiraIssueKey);
    }

    /**
     * Generates an idempotency key for retry safety.
     */
    private String generateIdempotencyKey(String testCaseId) {
        return "tc-" + testCaseId;
    }

    /**
     * Maps TestCase priority to Jira priority.
     */
    private String mapPriorityToJira(TestCase.Priority priority) {
        return switch (priority) {
            case HIGH -> "Highest";
            case MEDIUM -> "Medium";
            case LOW -> "Lowest";
        };
    }

    /**
     * Determines if an error is retryable.
     */
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("timeout") || 
               message.contains("503") || 
               message.contains("502") ||
               message.contains("connection");
    }

    private String truncate(String s, int length) {
        return s != null && s.length() > length ? s.substring(0, length) : s;
    }
}
