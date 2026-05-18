package com.qaautomation.services;

import com.qaautomation.models.*;
import com.qaautomation.repositories.*;
import com.qaautomation.dto.SimilarTestCaseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VectorSearchService {
    
    @Autowired
    private TestCaseEmbeddingRepository testCaseEmbeddingRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    private static final float DEDUPLICATION_THRESHOLD = 0.85f;
    private static final float SIMILARITY_THRESHOLD = 0.70f;
    
    /**
     * Find similar test cases using cosine similarity
     */
    public List<SimilarTestCaseDTO> findSimilarTestCases(
        float[] queryEmbedding,
        UUID conversationId,
        int limit,
        float threshold
    ) {
        try {
            log.info("Finding similar test cases for conversation: {}", conversationId);
            
            List<TestCaseEmbedding> embeddings = testCaseEmbeddingRepository.findByConversationId(conversationId);
            
            List<SimilarTestCaseDTO> results = embeddings.stream()
                .map(emb -> {
                    float similarity = embeddingService.calculateCosineSimilarity(
                        queryEmbedding,
                        emb.getCombinedEmbedding()
                    );
                    return SimilarTestCaseDTO.builder()
                        .testCaseId(emb.getTestCase().getId().toString())
                        .title(emb.getTestCase().getTitle())
                        .similarityScore(similarity)
                        .conversationId(conversationId.toString())
                        .build();
                })
                .filter(dto -> dto.getSimilarityScore() >= threshold)
                .sorted((a, b) -> Float.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(limit)
                .collect(Collectors.toList());
            
            log.info("Found {} similar test cases", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error finding similar test cases", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Detect duplicate test cases within a conversation
     */
    public List<DuplicatePair> detectDuplicates(UUID conversationId) {
        try {
            log.info("Detecting duplicates in conversation: {}", conversationId);
            
            List<TestCaseEmbedding> embeddings = testCaseEmbeddingRepository.findByConversationId(conversationId);
            List<DuplicatePair> duplicates = new ArrayList<>();
            
            for (int i = 0; i < embeddings.size(); i++) {
                for (int j = i + 1; j < embeddings.size(); j++) {
                    float similarity = embeddingService.calculateCosineSimilarity(
                        embeddings.get(i).getCombinedEmbedding(),
                        embeddings.get(j).getCombinedEmbedding()
                    );
                    
                    if (similarity >= DEDUPLICATION_THRESHOLD) {
                        duplicates.add(DuplicatePair.builder()
                            .testCase1Id(embeddings.get(i).getTestCase().getId())
                            .testCase2Id(embeddings.get(j).getTestCase().getId())
                            .testCase1Title(embeddings.get(i).getTestCase().getTitle())
                            .testCase2Title(embeddings.get(j).getTestCase().getTitle())
                            .similarity(similarity)
                            .build());
                    }
                }
            }
            
            log.info("Found {} duplicate pairs", duplicates.size());
            return duplicates;
        } catch (Exception e) {
            log.error("Error detecting duplicates", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Retrieve context for recommendations from similar cases
     */
    public List<String> retrieveContextForRecommendations(
        float[] queryEmbedding,
        UUID conversationId,
        int limit
    ) {
        try {
            log.info("Retrieving context for recommendations");
            
            List<SimilarTestCaseDTO> similar = findSimilarTestCases(
                queryEmbedding,
                conversationId,
                limit,
                SIMILARITY_THRESHOLD
            );
            
            return similar.stream()
                .map(SimilarTestCaseDTO::getTitle)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving recommendation context", e);
            return new ArrayList<>();
        }
    }
}

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
class DuplicatePair {
    private String testCase1Id;
    private String testCase2Id;
    private String testCase1Title;
    private String testCase2Title;
    private Float similarity;
}
