package com.qaautomation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Service for calling external LLM APIs (OpenAI or Azure OpenAI).
 * Handles API calls, error handling, and response parsing.
 */
@Slf4j
@Service
public class LLMService {

    private final String openaiApiKey;
    private final String openaiModel;
    private final boolean azureOpenaiEnabled;
    private final String azureOpenaiApiKey;
    private final String azureOpenaiEndpoint;
    private final String azureOpenaiDeployment;
    private final String azureOpenaiApiVersion;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public LLMService(
        @Value("${external-apis.openai.api-key}") String openaiApiKey,
        @Value("${external-apis.openai.model}") String openaiModel,
        @Value("${external-apis.azure-openai.enabled:false}") boolean azureOpenaiEnabled,
        @Value("${external-apis.azure-openai.api-key:}") String azureOpenaiApiKey,
        @Value("${external-apis.azure-openai.endpoint:}") String azureOpenaiEndpoint,
        @Value("${external-apis.azure-openai.deployment:}") String azureOpenaiDeployment,
        @Value("${external-apis.azure-openai.api-version:2024-02-15-preview}") String azureOpenaiApiVersion,
        RestClient restClient,
        ObjectMapper objectMapper) {
        this.openaiApiKey = openaiApiKey;
        this.openaiModel = openaiModel;
        this.azureOpenaiEnabled = azureOpenaiEnabled;
        this.azureOpenaiApiKey = azureOpenaiApiKey;
        this.azureOpenaiEndpoint = azureOpenaiEndpoint;
        this.azureOpenaiDeployment = azureOpenaiDeployment;
        this.azureOpenaiApiVersion = azureOpenaiApiVersion;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        
        logInitializedProvider();
    }

    /**
     * Log which provider is being used.
     */
    private void logInitializedProvider() {
        if (isAzureOpenaiConfigured()) {
            log.info("LLM Service initialized with Azure OpenAI endpoint: {}", maskEndpoint(azureOpenaiEndpoint));
        } else if (isOpenaiConfigured()) {
            log.info("LLM Service initialized with OpenAI API");
        } else {
            log.warn("No LLM provider configured. Using mock responses.");
        }
    }

    /**
     * Calls the appropriate LLM API based on configuration.
     * @param prompt The prompt to send to the LLM
     * @param temperature Controls randomness (0.0 = deterministic, 1.0 = very random)
     * @param maxTokens Maximum tokens in response
     * @return The LLM's response text
     */
    public String callLLM(String prompt, float temperature, int maxTokens) throws Exception {
        if (isAzureOpenaiConfigured()) {
            log.debug("Using Azure OpenAI for LLM call");
            return callAzureOpenAI(prompt, temperature, maxTokens);
        } else if (isOpenaiConfigured()) {
            log.debug("Using OpenAI for LLM call");
            return callOpenAI(prompt, temperature, maxTokens);
        } else {
            log.warn("No LLM provider configured, returning mock response");
            return generateMockResponse(prompt);
        }
    }

    /**
     * Check if Azure OpenAI is properly configured.
     */
    private boolean isAzureOpenaiConfigured() {
        return azureOpenaiEnabled && 
               azureOpenaiApiKey != null && !azureOpenaiApiKey.isEmpty() &&
               azureOpenaiEndpoint != null && !azureOpenaiEndpoint.isEmpty() &&
               azureOpenaiDeployment != null && !azureOpenaiDeployment.isEmpty();
    }

    /**
     * Check if OpenAI is properly configured.
     */
    private boolean isOpenaiConfigured() {
        return openaiApiKey != null && !openaiApiKey.isEmpty() && !openaiApiKey.contains("test-key");
    }

    /**
     * Calls Azure OpenAI API.
     */
    private String callAzureOpenAI(String prompt, float temperature, int maxTokens) throws Exception {
        try {
            // Build Azure OpenAI URL
            String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                azureOpenaiEndpoint, azureOpenaiDeployment, azureOpenaiApiVersion);

            // Build request payload
            Map<String, Object> requestPayload = buildRequestPayload(prompt, temperature, maxTokens);

            log.debug("Calling Azure OpenAI at: {}", maskEndpoint(url));

            // Make HTTP request
            String response = restClient.post()
                .uri(url)
                .header("api-key", azureOpenaiApiKey)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(requestPayload))
                .retrieve()
                .body(String.class);

            log.debug("Azure OpenAI response received");
            return parseAzureOpenAIResponse(response);

        } catch (Exception e) {
            log.error("Error calling Azure OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Azure OpenAI: " + e.getMessage(), e);
        }
    }

    /**
     * Internal method to call OpenAI API.
     */
    private String callOpenAI(String prompt, float temperature, int maxTokens) throws Exception {
        try {
            // Build request payload
            Map<String, Object> requestPayload = buildRequestPayload(prompt, temperature, maxTokens);

            log.debug("Calling OpenAI API");

            // Make HTTP request
            String response = restClient.post()
                .uri(OPENAI_API_URL)
                .header("Authorization", "Bearer " + openaiApiKey)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(requestPayload))
                .retrieve()
                .body(String.class);

            log.debug("OpenAI response received");
            return parseOpenAIResponse(response);

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call LLM: " + e.getMessage(), e);
        }
    }

    /**
     * Builds LLM request payload (compatible with both OpenAI and Azure OpenAI).
     */
    private Map<String, Object> buildRequestPayload(String prompt, float temperature, int maxTokens) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Use Azure deployment name if available, otherwise use OpenAI model
        String model = isAzureOpenaiConfigured() ? azureOpenaiDeployment : openaiModel;
        payload.put("model", model);
        payload.put("temperature", Math.max(0.0f, Math.min(1.0f, temperature)));
        payload.put("max_tokens", Math.min(maxTokens, 4000));
        payload.put("top_p", 0.95);

        // Messages format
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        payload.put("messages", messages);

        return payload;
    }

    /**
     * Parses Azure OpenAI response and extracts content.
     */
    private String parseAzureOpenAIResponse(String response) throws Exception {
        try {
            var rootNode = objectMapper.readTree(response);

            // Check for errors
            if (rootNode.has("error")) {
                String errorMessage = rootNode.get("error").get("message").asText();
                log.error("Azure OpenAI API error: {}", errorMessage);
                throw new RuntimeException("Azure OpenAI API error: " + errorMessage);
            }

            // Extract content from first choice
            var choices = rootNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                var message = choices.get(0).get("message");
                if (message != null) {
                    return message.get("content").asText();
                }
            }

            throw new RuntimeException("Unexpected response format from Azure OpenAI");

        } catch (Exception e) {
            log.error("Error parsing Azure OpenAI response: {}", response, e);
            throw e;
        }
    }

    /**
     * Parses OpenAI response and extracts content.
     */
    private String parseOpenAIResponse(String response) throws Exception {
        try {
            var rootNode = objectMapper.readTree(response);

            // Check for errors
            if (rootNode.has("error")) {
                String errorMessage = rootNode.get("error").get("message").asText();
                log.error("OpenAI API error: {}", errorMessage);
                throw new RuntimeException("OpenAI API error: " + errorMessage);
            }

            // Extract content from first choice
            var choices = rootNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                var message = choices.get(0).get("message");
                if (message != null) {
                    return message.get("content").asText();
                }
            }

            throw new RuntimeException("Unexpected response format from OpenAI");

        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}", response, e);
            throw e;
        }
    }

    /**
     * Mask sensitive endpoint information for logging.
     */
    private String maskEndpoint(String endpoint) {
        if (endpoint == null || endpoint.length() < 20) {
            return "***";
        }
        return endpoint.substring(0, 20) + "...";
    }

    /**
     * Generates a mock response for testing/demo purposes.
     */
    private String generateMockResponse(String prompt) {
        // Check what type of response is needed based on prompt content
        if (prompt.contains("test case") || prompt.contains("Test Case")) {
            return """
                [
                  {
                    "title": "Test Login with Valid Credentials",
                    "description": "User should be able to login with correct username and password",
                    "preconditions": "User is on the login page",
                    "steps": [
                      {"stepNumber": 1, "action": "Enter username in username field", "testData": "testuser@example.com", "expectedResult": "Username is entered"},
                      {"stepNumber": 2, "action": "Enter password in password field", "testData": "ValidPassword123", "expectedResult": "Password is masked"},
                      {"stepNumber": 3, "action": "Click Login button", "testData": "N/A", "expectedResult": "User is logged in and redirected to dashboard"}
                    ],
                    "expectedResults": ["User is successfully logged in", "Dashboard is displayed"],
                    "priority": "HIGH",
                    "tags": ["authentication", "positive"],
                    "estimatedComplexity": 0.4
                  }
                ]
                """;
        } else if (prompt.contains("code") || prompt.contains("automation")) {
            return """
                {
                  "code": "package tests;\\n\\nimport org.testng.annotations.Test;\\n\\npublic class LoginTest {\\n    @Test\\n    public void testLoginWithValidCredentials() {\\n        System.out.println(\\"Testing login functionality\\");\\n        // Step 1: Open browser\\n        // Step 2: Navigate to login page\\n        // Step 3: Enter valid credentials\\n        // Step 4: Click login button\\n        // Step 5: Verify user is logged in\\n    }\\n}",
                  "files": [
                    {
                      "path": "src/test/java/tests/LoginTest.java",
                      "content": "package tests;\\n\\nimport org.testng.annotations.Test;\\n\\npublic class LoginTest {\\n    @Test\\n    public void testLoginWithValidCredentials() {\\n        System.out.println(\\"Testing login functionality\\");\\n        // Step 1: Open browser\\n        // Step 2: Navigate to login page\\n        // Step 3: Enter valid credentials\\n        // Step 4: Click login button\\n        // Step 5: Verify user is logged in\\n    }\\n}"
                    }
                  ],
                  "dependencies": ["org.testng:testng:7.7.0"],
                  "notes": "Mock generated code",
                  "confidence": 0.85
                }
                """;
        }

            // Default mock response
            return "Mock LLM response for prompt: " + prompt.substring(0, Math.min(100, prompt.length()));
        }
    }