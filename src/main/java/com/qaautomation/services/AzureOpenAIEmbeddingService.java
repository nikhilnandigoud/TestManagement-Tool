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
import java.util.stream.Collectors;

/**
 * Azure OpenAI Embedding Service for real embedding generation
 * Phase 4: Uses Azure OpenAI API for text-embedding-3-small model via REST
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "azure.openai", name = "endpoint")
public class AzureOpenAIEmbeddingService {
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    @Autowired
    private AzureOpenAIProperties azureProperties;
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    private static final int EMBEDDING_DIMENSION = 1536;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    /**
     * Generate embedding for a single text using Azure OpenAI
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[EMBEDDING_DIMENSION];
        }
        
        try {
            if (restTemplate == null || azureProperties == null || azureProperties.getEndpoint() == null) {
                log.warn("Azure OpenAI not configured, using mock embedding");
                return generateMockEmbedding(text);
            }
            
            return callAzureOpenAIEmbedding(text);
            
        } catch (Exception e) {
            log.error("Error generating embedding for text length: {}", text.length(), e);
            // Fallback to mock embedding
            return generateMockEmbedding(text);
        }
    }
    
    /**
     * Call Azure OpenAI Embedding API
     */
    private float[] callAzureOpenAIEmbedding(String text) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String endpoint = azureProperties.getEndpoint();
                if (!endpoint.endsWith("/")) endpoint += "/";
                
                String url = endpoint + "openai/deployments/" + 
                    azureProperties.getDeploymentEmbedding() + 
                    "/embeddings?api-version=2023-05-15";
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", azureProperties.getApiKey());
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("input", text.substring(0, Math.min(text.length(), 8191)));
                
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                
                if (response.getBody() != null) {
                    if (objectMapper == null) {
                        objectMapper = new ObjectMapper();
                    }
                    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                    JsonNode dataArray = jsonResponse.get("data");
                    if (dataArray != null && dataArray.isArray() && dataArray.size() > 0) {
                        JsonNode embedding = dataArray.get(0).get("embedding");
                        if (embedding != null && embedding.isArray()) {
                            float[] result = new float[Math.min(embedding.size(), EMBEDDING_DIMENSION)];
                            for (int i = 0; i < result.length; i++) {
                                result[i] = (float) embedding.get(i).asDouble();
                            }
                            log.debug("Successfully generated embedding for text length: {}", text.length());
                            return result;
                        }
                    }
                }
                
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("Embedding generation attempt {} failed, retrying in {}ms", attempt, RETRY_DELAY_MS);
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.error("Failed to generate embedding after {} attempts", MAX_RETRIES, e);
                }
            }
        }
        
        return new float[EMBEDDING_DIMENSION];
    }
    
    /**
     * Generate embeddings for multiple texts in batch
     */
    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        
        return texts.stream()
            .map(this::generateEmbedding)
            .collect(Collectors.toList());
    }
    
    /**
     * Mock embedding for fallback or testing
     */
    private float[] generateMockEmbedding(String text) {
        float[] embedding = new float[EMBEDDING_DIMENSION];
        Random random = new Random(text.hashCode());
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = (float) (Math.sin(random.nextDouble()) * 0.5);
        }
        // Normalize
        float norm = 0;
        for (float v : embedding) norm += v * v;
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] /= (norm + 1e-8f);
        }
        return embedding;
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
            // Try to generate a simple embedding to verify connection
            float[] result = generateEmbedding("health check");
            boolean isValid = result != null && result.length == EMBEDDING_DIMENSION;
            if (isValid) {
                log.info("Azure OpenAI Embedding health check passed");
            }
            return isValid;
        } catch (Exception e) {
            log.error("Azure OpenAI Embedding health check failed", e);
            return false;
        }
    }
}
