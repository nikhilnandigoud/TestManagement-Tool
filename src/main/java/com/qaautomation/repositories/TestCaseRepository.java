
package com.qaautomation.repositories;

import com.qaautomation.models.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, String> {
    List<TestCase> findByRequirementId(String requirementId);
    List<TestCase> findByPriority(TestCase.Priority priority);
    List<TestCase> findByAppName(String appName);
    List<TestCase> findByModule(String module);
    List<TestCase> findByAppNameAndModule(String appName, String module);
    
    // Find test cases by conversation
    List<TestCase> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    
    // Custom query for finding test cases by tag (since tags is a collection)
    @Query("SELECT tc FROM TestCase tc WHERE :tag MEMBER OF tc.tags")
    List<TestCase> findByTag(@Param("tag") String tag);
}

