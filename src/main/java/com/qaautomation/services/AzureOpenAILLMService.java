package com.qaautomation.services;

import com.qaautomation.config.AzureOpenAIProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * Azure OpenAI LLM Service for chat completion
 * Phase 4: Uses Azure OpenAI API for gpt-4 model via REST
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "azure.openai", name = "endpoint")
public class AzureOpenAILLMService {
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    @Autowired
    private AzureOpenAIProperties azureProperties;
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final int DEFAULT_MAX_TOKENS = 4096;
    
    /**
     * Generate chat completion response with context
     */
    public String generateChatResponse(String userMessage, List<String> contextList) {
        return generateChatResponse(userMessage, contextList, DEFAULT_MAX_TOKENS);
    }
    
    /**
     * Generate chat completion response with context and custom max tokens
     */
    public String generateChatResponse(String userMessage, List<String> contextList, int maxTokens) {
        try {
            if (restTemplate == null || azureProperties == null || azureProperties.getEndpoint() == null) {
                log.warn("Azure OpenAI not configured");
                return generateMockResponse(userMessage);
            }
            
            return callAzureOpenAIChat(userMessage, contextList, maxTokens);
            
        } catch (Exception e) {
            log.error("Error generating chat response", e);
            return generateMockResponse(userMessage);
        }
    }
    
    /**
     * Call Azure OpenAI Chat Completions API
     */
    private String callAzureOpenAIChat(String userMessage, List<String> contextList, int maxTokens) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String endpoint = azureProperties.getEndpoint();
                if (!endpoint.endsWith("/")) endpoint += "/";
                
                String url = endpoint + "openai/deployments/" + 
                    azureProperties.getDeploymentGpt4() + 
                    "/chat/completions?api-version=2023-05-15";
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", azureProperties.getApiKey());
                
                // Build system prompt with context
                String systemPrompt = buildSystemPrompt(contextList);
                
                Map<String, Object> message1 = new HashMap<>();
                message1.put("role", "system");
                message1.put("content", systemPrompt);
                
                Map<String, Object> message2 = new HashMap<>();
                message2.put("role", "user");
                message2.put("content", userMessage);
                
                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(message1);
                messages.add(message2);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("messages", messages);
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", 0.7);
                
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                
                if (response.getBody() != null) {
                    if (objectMapper == null) {
                        objectMapper = new ObjectMapper();
                    }
                    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                    JsonNode choices = jsonResponse.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode content = choices.get(0).get("message").get("content");
                        if (content != null) {
                            String result = content.asText();
                            log.debug("Successfully generated chat response");
                            return result;
                        }
                    }
                }
                
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("Chat completion attempt {} failed, retrying in {}ms", attempt, RETRY_DELAY_MS);
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.error("Failed to generate chat response after {} attempts", MAX_RETRIES, e);
                }
            }
        }
        
        return generateMockResponse(userMessage);
    }
    
    /**
     * Build system prompt with priority context
     */
    private String buildSystemPrompt(List<String> contextList) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert QA automation test case generation assistant.\\n\\n");
        prompt.append("Use the following context to generate comprehensive test cases:\\n\\n");
        
        if (contextList != null && !contextList.isEmpty()) {
            for (int i = 0; i < contextList.size(); i++) {
                prompt.append("PRIORITY ").append(i + 1).append(":\\n");
                prompt.append(contextList.get(i)).append("\\n\\n");
            }
        }
        
        prompt.append("Guidelines:\\n");
        prompt.append("1. Generate clear, actionable test cases\\n");
        prompt.append("2. Include preconditions, steps, and expected results\\n");
        prompt.append("3. Consider edge cases and error scenarios\\n");
        prompt.append("4. Use the provided context to inform your recommendations\\n");
        prompt.append("5. Be specific and avoid ambiguous language\\n");
        
        return prompt.toString();
    }
    
    /**
     * Mock response for fallback or testing
     */
    private String generateMockResponse(String userMessage) {
        return "Based on your request: \"" + userMessage + "\", here are test case recommendations:\\n\\n" +
               "Test Case 1: Verify primary functionality\\n" +
               "- Preconditions: System ready\\n" +
               "- Steps: Execute main flow\\n" +
               "- Expected: Successful completion\\n\\n" +
               "Test Case 2: Verify error handling\\n" +
               "- Preconditions: Error conditions present\\n" +
               "- Steps: Execute in error state\\n" +
               "- Expected: Proper error handling\\n\\n" +
               "(Note: This is a mock response. Configure Azure OpenAI for real responses.)";
    }
    
    /**
     * Health check: Verify Azure OpenAI connection
     */
    public boolean healthCheck() {
        if (restTemplate == null || azureProperties == null || azureProperties.getEndpoint() == null) {
            log.warn("Azure OpenAI not configured");
            return false;
        }
        
        try {
            // Try a simple chat completion to verify connection
            String response = generateChatResponse("Respond with 'healthy'.", List.of());
            boolean isHealthy = response.length() > 0 && !response.contains("mock response");
            if (isHealthy) {
                log.info("Azure OpenAI LLM health check passed");
            }
            return isHealthy;
        } catch (Exception e) {
            log.error("Azure OpenAI LLM health check failed", e);
            return false;
        }
    }
}
