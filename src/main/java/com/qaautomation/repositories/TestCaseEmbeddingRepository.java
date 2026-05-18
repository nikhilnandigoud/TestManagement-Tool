package com.qaautomation.repositories;

import com.qaautomation.models.TestCaseEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface TestCaseEmbeddingRepository extends JpaRepository<TestCaseEmbedding, UUID> {
    Optional<TestCaseEmbedding> findByTestCaseId(String testCaseId);
    
    @Query(value = "SELECT * FROM test_case_embeddings WHERE conversation_id = :conversationId", nativeQuery = true)
    List<TestCaseEmbedding> findByConversationId(@Param("conversationId") UUID conversationId);
    
    /**
     * Local fallback query. Similarity scoring is performed in VectorSearchService.
     */
    @Query(value = "SELECT * FROM test_case_embeddings tce " +
            "WHERE conversation_id = :conversationId " +
            "AND CAST(:queryEmbedding AS text) IS NOT NULL " +
            "AND :threshold <= 1 " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<TestCaseEmbedding> findSimilarTestCases(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("conversationId") UUID conversationId,
            @Param("threshold") Double threshold,
            @Param("limit") Integer limit
    );
    
    /**
     * Local fallback query. Duplicate scoring is performed in VectorSearchService.
     */
    @Query(value = "SELECT tce1.* FROM test_case_embeddings tce1 " +
            "JOIN test_case_embeddings tce2 ON tce1.conversation_id = tce2.conversation_id " +
            "AND tce1.test_case_id < tce2.test_case_id " +
            "WHERE tce1.conversation_id = :conversationId " +
            "AND :threshold <= 1",
            nativeQuery = true)
    List<TestCaseEmbedding> findDuplicateTestCases(
            @Param("conversationId") UUID conversationId,
            @Param("threshold") Double threshold
    );
}
