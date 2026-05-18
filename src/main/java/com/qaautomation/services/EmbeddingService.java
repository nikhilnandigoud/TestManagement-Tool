package com.qaautomation.services;

import com.qaautomation.models.*;
import com.qaautomation.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmbeddingService {
    
    @Autowired
    private TestCaseEmbeddingRepository testCaseEmbeddingRepository;
    
    @Autowired
    private ChatMessageEmbeddingRepository chatMessageEmbeddingRepository;
    
    @Autowired(required = false)
    private AzureOpenAIEmbeddingService azureEmbeddingService;
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/embeddings}")
    private String openaiUrl;
    
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int EMBEDDING_DIMENSION = 1536;
    
    /**
     * Generate embeddings for a test case
     */
    public TestCaseEmbedding generateTestCaseEmbedding(TestCase testCase, Conversation conversation) {
        try {
            log.info("Generating embeddings for test case: {}", testCase.getId());
            
            // Generate embeddings for different components
            float[] titleEmbedding = generateEmbedding(testCase.getTitle());
            float[] stepsEmbedding = generateEmbedding(stepsToString(testCase.getSteps()));
            float[] resultsEmbedding = generateEmbedding(expectedResultsToString(testCase.getExpectedResults()));
            
            // Combined embedding: concatenate and average
            float[] combinedEmbedding = combineEmbeddings(titleEmbedding, stepsEmbedding, resultsEmbedding);
            
            TestCaseEmbedding embedding = TestCaseEmbedding.builder()
                .id(UUID.randomUUID())
                .testCase(testCase)
                .conversation(conversation)
                .embeddingModel(EMBEDDING_MODEL)
                .embeddingDimensions(EMBEDDING_DIMENSION)
                .titleEmbedding(titleEmbedding)
                .stepsEmbedding(stepsEmbedding)
                .resultsEmbedding(resultsEmbedding)
                .combinedEmbedding(combinedEmbedding)
                .build();
            
            return testCaseEmbeddingRepository.save(embedding);
        } catch (Exception e) {
            log.error("Error generating test case embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    /**
     * Generate embeddings for a chat message
     */
    public ChatMessageEmbedding generateChatMessageEmbedding(ChatMessage message, Conversation conversation) {
        try {
            log.info("Generating embedding for chat message: {}", message.getId());
            
            float[] embedding = generateEmbedding(message.getContent());
            
            ChatMessageEmbedding msgEmbedding = ChatMessageEmbedding.builder()
                .id(UUID.randomUUID())
                .chatMessage(message)
                .conversation(conversation)
                .embeddingModel(EMBEDDING_MODEL)
                .embeddingDimensions(EMBEDDING_DIMENSION)
                .contentEmbedding(embedding)
                .build();
            
            return chatMessageEmbeddingRepository.save(msgEmbedding);
        } catch (Exception e) {
            log.error("Error generating chat message embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
    
    /**
     * Generate embedding using OpenAI API or Azure OpenAI (Phase 4)
     */
    private float[] generateEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                // Return zero vector if text is empty
                return new float[EMBEDDING_DIMENSION];
            }
            
            // Prefer Azure OpenAI if configured
            if (azureEmbeddingService != null) {
                log.debug("Using Azure OpenAI for embedding generation (text length: {})", text.length());
                return azureEmbeddingService.generateEmbedding(text);
            }
            
            // Fallback to mock embedding for development/testing
            log.debug("Azure OpenAI not configured, using mock embedding for text length: {}", text.length());
            return generateMockEmbedding(text);
            
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            // Return zero vector instead of throwing to allow graceful degradation
            return new float[EMBEDDING_DIMENSION];
        }
    }
    
    /**
     * Mock embedding generation (replace with real OpenAI API call)
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
     * Combine multiple embeddings
     */
    private float[] combineEmbeddings(float[]... embeddings) {
        float[] combined = new float[EMBEDDING_DIMENSION];
        for (float[] emb : embeddings) {
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                combined[i] += emb[i];
            }
        }
        // Average
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            combined[i] /= embeddings.length;
        }
        // Normalize
        float norm = 0;
        for (float v : combined) norm += v * v;
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            combined[i] /= (norm + 1e-8f);
        }
        return combined;
    }
    
    /**
     * Convert steps list to string
     */
    private String stepsToString(java.util.List<com.qaautomation.models.Step> stepsList) {
        try {
            if (stepsList == null || stepsList.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (com.qaautomation.models.Step step : stepsList) {
                sb.append("Step ").append(step.getStepNumber()).append(": ")
                  .append(step.getAction()).append(". ");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Convert expected results list to string
     */
    private String expectedResultsToString(java.util.List<String> resultsList) {
        try {
            if (resultsList == null || resultsList.isEmpty()) return "";
            return String.join(". ", resultsList);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    public float calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null || 
            embedding1.length != embedding2.length) {
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
        }
        return dotProduct;
    }
    
    /**
     * Convert float array to JSON string for storage
     */
    private String floatArrayToJson(float[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Convert JSON string to float array for retrieval
     */
    private float[] jsonToFloatArray(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new float[EMBEDDING_DIMENSION];
        }
        try {
            String trimmed = json.replaceAll("[\\[\\]\\s]", "");
            String[] parts = trimmed.split(",");
            float[] array = new float[Math.min(parts.length, EMBEDDING_DIMENSION)];
            for (int i = 0; i < array.length; i++) {
                array[i] = Float.parseFloat(parts[i]);
            }
            if (array.length < EMBEDDING_DIMENSION) {
                // Pad with zeros if needed
                float[] padded = new float[EMBEDDING_DIMENSION];
                System.arraycopy(array, 0, padded, 0, array.length);
                return padded;
            }
            return array;
        } catch (Exception e) {
            log.warn("Error parsing embedding JSON", e);
            return new float[EMBEDDING_DIMENSION];
        }
    }
}
