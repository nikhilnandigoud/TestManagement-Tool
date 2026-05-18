package com.qaautomation.repositories;

import com.qaautomation.models.TestCaseLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TestCaseLibrary entities.
 * Manages persistence of test cases saved to the personal library.
 */
@Repository
public interface TestCaseLibraryRepository extends JpaRepository<TestCaseLibrary, String> {
    
    /**
     * Finds all library entries for a specific test case
     */
    List<TestCaseLibrary> findByTestCaseId(String testCaseId);
    
    /**
     * Finds library entries by category
     */
    List<TestCaseLibrary> findByCategory(TestCaseLibrary.TestCaseCategory category);
    
    /**
     * Finds library entries by priority
     */
    List<TestCaseLibrary> findByPriority(String priority);
    
    /**
     * Finds library entries by creator
     */
    List<TestCaseLibrary> findByCreatedBy(String createdBy);
    
    /**
     * Finds library entries by tag (since tags is a collection)
     */
    @Query("SELECT tcl FROM TestCaseLibrary tcl WHERE :tag MEMBER OF tcl.tags")
    List<TestCaseLibrary> findByTag(@Param("tag") String tag);
    
    /**
     * Finds library entries by title (case-insensitive)
     */
    List<TestCaseLibrary> findByLibraryTitleIgnoreCase(String libraryTitle);
    
    /**
     * Finds library entries that are public
     */
    List<TestCaseLibrary> findByIsPublic(Boolean isPublic);
    
    /**
     * Finds library entries by creator and category
     */
    List<TestCaseLibrary> findByCreatedByAndCategory(String createdBy, TestCaseLibrary.TestCaseCategory category);
    
    /**
     * Finds the most recently used library entries
     */
    @Query(value = "SELECT tcl FROM TestCaseLibrary tcl ORDER BY tcl.usageCount DESC LIMIT :limit")
    List<TestCaseLibrary> findMostUsedLibraryEntries(@Param("limit") int limit);
    
    /**
     * Search library entries by title or description (case-insensitive)
     */
    @Query("SELECT tcl FROM TestCaseLibrary tcl WHERE " +
           "LOWER(tcl.libraryTitle) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(tcl.libraryDescription) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<TestCaseLibrary> searchByTitleOrDescription(@Param("searchText") String searchText);
    
    /**
     * Deletes multiple library entries by IDs
     */
    void deleteByIdIn(List<String> ids);
    
    /**
     * Finds all library entries by multiple IDs
     */
    List<TestCaseLibrary> findByIdIn(List<String> ids);
}
