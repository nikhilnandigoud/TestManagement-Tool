package com.qaautomation.services;

import com.qaautomation.agents.TestCaseGenerationAgent;
import com.qaautomation.models.*;
import com.qaautomation.repositories.ConversationRepository;
import com.qaautomation.repositories.TestCaseRepository;
import com.qaautomation.repositories.TestCaseLibraryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

/**
 * Service layer for test case generation operations.
 */
@Slf4j
@Service
public class TestCaseGenerationService {

    private final TestCaseRepository testCaseRepository;
    private final TestCaseGenerationAgent testCaseGenerationAgent;
    private final ConversationRepository conversationRepository;
    private final TestCaseLibraryRepository testCaseLibraryRepository;

    public TestCaseGenerationService(
        TestCaseRepository testCaseRepository,
        TestCaseGenerationAgent testCaseGenerationAgent,
        ConversationRepository conversationRepository,
        TestCaseLibraryRepository testCaseLibraryRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testCaseGenerationAgent = testCaseGenerationAgent;
        this.conversationRepository = conversationRepository;
        this.testCaseLibraryRepository = testCaseLibraryRepository;
    }

    /**
     * Generates test cases from requirement text.
     */
    public List<TestCase> generateTestCases(TestCaseGenerationRequest request) throws Exception {
        log.info("Generating test cases from requirement: {}", request.getRequirementId());

        List<TestCase> testCases = testCaseGenerationAgent.generateTestCases(request, 3);

        // Set appName and module from request before saving
        String appName = request.getContext() != null ? request.getContext().getAppName() : "Unknown";
        String module = request.getContext() != null ? request.getContext().getModule() : "General";
        Conversation conversation = createGenerationConversation(request);
        
        testCases.forEach(tc -> {
            tc.setAppName(appName);
            tc.setModule(module);
            tc.setConversation(conversation);
        });

        // Save generated test cases
        testCases = testCaseRepository.saveAll(testCases);

        log.info("Generated {} test cases for app: {}, module: {}", testCases.size(), appName, module);
        return testCases;
    }

    /**
     * Generates test cases from requirement text with user guide context.
     */
    public List<TestCase> generateTestCasesWithGuide(TestCaseGenerationRequest request) throws Exception {
        String guideFileName = request.getUserGuideFileName();
        log.info("Generating test cases from requirement with guide: {} (Guide: {})", 
            request.getRequirementId(), guideFileName);

        // Use the enhanced method with guide
        List<TestCase> testCases = testCaseGenerationAgent.generateTestCasesWithGuide(request, 3);

        // Set appName and module from request before saving
        String appName = request.getContext() != null ? request.getContext().getAppName() : "Unknown";
        String module = request.getContext() != null ? request.getContext().getModule() : "General";
        Conversation conversation = createGenerationConversation(request);
        
        testCases.forEach(tc -> {
            tc.setAppName(appName);
            tc.setModule(module);
            tc.setConversation(conversation);
        });

        // Save generated test cases
        testCases = testCaseRepository.saveAll(testCases);

        log.info("Generated {} test cases for app: {}, module: {}, guide: {}", 
            testCases.size(), appName, module, guideFileName);
        return testCases;
    }

    /**
     * Retrieves test cases by requirement ID.
     */
    public List<TestCase> getTestCasesByRequirement(String requirementId) {
        return testCaseRepository.findByRequirementId(requirementId);
    }

    /**
     * Retrieves test cases by application name.
     */
    public List<TestCase> getTestCasesByApplication(String appName) {
        return testCaseRepository.findByAppName(appName);
    }

    /**
     * Retrieves test cases by module.
     */
    public List<TestCase> getTestCasesByModule(String module) {
        return testCaseRepository.findByModule(module);
    }

    /**
     * Retrieves test cases by both application and module.
     */
    public List<TestCase> getTestCasesByApplicationAndModule(String appName, String module) {
        return testCaseRepository.findByAppNameAndModule(appName, module);
    }

    /**
     * Retrieves test cases by tag/label.
     */
    public List<TestCase> getTestCasesByTag(String tag) {
        return testCaseRepository.findByTag(tag);
    }

    /**
     * Retrieves all test cases.
     */
    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    /**
     * Retrieves test case by ID.
     */
    public TestCase getTestCaseById(String testCaseId) {
        return testCaseRepository.findById(testCaseId).orElse(null);
    }

    public long deleteAllTestCases() {
        long count = testCaseRepository.count();
        testCaseRepository.deleteAll();
        log.info("Deleted {} test cases from the library", count);
        return count;
    }

    private Conversation createGenerationConversation(TestCaseGenerationRequest request) {
        String scenarioText = request.getText() != null && !request.getText().isBlank()
            ? request.getText()
            : request.getRequirementsText();

        String requirementId = request.getRequirementId() != null && !request.getRequirementId().isBlank()
            ? request.getRequirementId()
            : "standalone";

        Conversation conversation = Conversation.builder()
            .id(UUID.randomUUID())
            .title("Generated Test Cases - " + requirementId)
            .scenarioText(scenarioText)
            .status("GENERATED")
            .userId("default-user")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return conversationRepository.save(conversation);
    }

    /**
     * Saves test cases to the library with metadata
     */
    public List<TestCaseLibrary> saveTestCasesToLibrary(SaveToLibraryRequest request) throws Exception {
        log.info("Saving {} test cases to library", request.getTestCaseIds().size());
        
        // Validate request
        String validationError = request.getValidationError();
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        
        List<TestCaseLibrary> savedEntries = new ArrayList<>();
        
        for (String testCaseId : request.getTestCaseIds()) {
            // Get the test case
            TestCase testCase = testCaseRepository.findById(testCaseId).orElse(null);
            if (testCase == null) {
                log.warn("Test case not found: {}", testCaseId);
                continue;
            }
            
            // Handle replacement if needed
            if (request.getShouldReplace() != null && request.getShouldReplace() 
                && request.getExistingLibraryId() != null) {
                testCaseLibraryRepository.deleteById(request.getExistingLibraryId());
                log.info("Replaced existing library entry: {}", request.getExistingLibraryId());
            }
            
            // Create library entry
            TestCaseLibrary libraryEntry = TestCaseLibrary.builder()
                .testCaseId(testCaseId)
                .libraryTitle(request.getLibraryTitle())
                .libraryDescription(request.getLibraryDescription() != null ? 
                    request.getLibraryDescription() : testCase.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : 
                    TestCaseLibrary.TestCaseCategory.POSITIVE)
                .priority(request.getPriority() != null ? request.getPriority() : 
                    TestCase.Priority.MEDIUM)
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .qualityScore(request.getQualityScore() != null ? request.getQualityScore() : "Not Rated")
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .createdBy(request.getCreatedBy() != null ? request.getCreatedBy() : "Unknown")
                .notes(request.getNotes())
                .usageCount(0)
                .build();
            
            TestCaseLibrary saved = testCaseLibraryRepository.save(libraryEntry);
            savedEntries.add(saved);
            log.info("Saved test case {} to library with ID: {}", testCaseId, saved.getId());
        }
        
        log.info("Successfully saved {} test cases to library", savedEntries.size());
        return savedEntries;
    }

    /**
     * Retrieves test cases from library by category
     */
    public List<TestCaseLibrary> getLibraryByCategory(TestCaseLibrary.TestCaseCategory category) {
        return testCaseLibraryRepository.findByCategory(category);
    }

    /**
     * Retrieves test cases from library by creator
     */
    public List<TestCaseLibrary> getLibraryByCreator(String createdBy) {
        return testCaseLibraryRepository.findByCreatedBy(createdBy);
    }

    /**
     * Retrieves test cases from library by tag
     */
    public List<TestCaseLibrary> getLibraryByTag(String tag) {
        return testCaseLibraryRepository.findByTag(tag);
    }

    /**
     * Searches library by title or description
     */
    public List<TestCaseLibrary> searchLibrary(String searchText) {
        return testCaseLibraryRepository.searchByTitleOrDescription(searchText);
    }

    /**
     * Gets all public library entries
     */
    public List<TestCaseLibrary> getPublicLibraryEntries() {
        return testCaseLibraryRepository.findByIsPublic(true);
    }

    /**
     * Gets the most used library entries
     */
    public List<TestCaseLibrary> getMostUsedLibraryEntries(int limit) {
        return testCaseLibraryRepository.findMostUsedLibraryEntries(limit);
    }

    /**
     * Updates a library entry
     */
    public TestCaseLibrary updateLibraryEntry(String libraryId, SaveToLibraryRequest request) throws Exception {
        TestCaseLibrary existing = testCaseLibraryRepository.findById(libraryId)
            .orElseThrow(() -> new IllegalArgumentException("Library entry not found: " + libraryId));
        
        log.info("Updating library entry: {}", libraryId);
        
        if (request.getLibraryTitle() != null && !request.getLibraryTitle().isBlank()) {
            existing.setLibraryTitle(request.getLibraryTitle());
        }
        if (request.getLibraryDescription() != null && !request.getLibraryDescription().isBlank()) {
            existing.setLibraryDescription(request.getLibraryDescription());
        }
        if (request.getCategory() != null) {
            existing.setCategory(request.getCategory());
        }
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            existing.setTags(request.getTags());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            existing.setNotes(request.getNotes());
        }
        if (request.getIsPublic() != null) {
            existing.setIsPublic(request.getIsPublic());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        TestCaseLibrary saved = testCaseLibraryRepository.save(existing);
        log.info("Updated library entry: {}", libraryId);
        return saved;
    }

    /**
     * Increments usage count for a library entry
     */
    public TestCaseLibrary incrementLibraryUsageCount(String libraryId) {
        TestCaseLibrary entry = testCaseLibraryRepository.findById(libraryId)
            .orElse(null);
        if (entry != null) {
            entry.setUsageCount((entry.getUsageCount() != null ? entry.getUsageCount() : 0) + 1);
            entry.setUpdatedAt(LocalDateTime.now());
            testCaseLibraryRepository.save(entry);
            log.debug("Incremented usage count for library entry: {}", libraryId);
        }
        return entry;
    }

    /**
     * Deletes a library entry
     */
    public void deleteLibraryEntry(String libraryId) {
        testCaseLibraryRepository.deleteById(libraryId);
        log.info("Deleted library entry: {}", libraryId);
    }

    /**
     * Gets all library entries
     */
    public List<TestCaseLibrary> getAllLibraryEntries() {
        return testCaseLibraryRepository.findAll();
    }

    /**
     * Gets a library entry by ID
     */
    public TestCaseLibrary getLibraryEntryById(String libraryId) {
        return testCaseLibraryRepository.findById(libraryId).orElse(null);
    }

    /**
     * Retrieves the full test case for a library entry
     */
    public TestCase getTestCaseFromLibraryEntry(String libraryId) {
        TestCaseLibrary libraryEntry = testCaseLibraryRepository.findById(libraryId).orElse(null);
        if (libraryEntry != null) {
            return testCaseRepository.findById(libraryEntry.getTestCaseId()).orElse(null);
        }
        return null;
    }

    /**
     * Deletes all library entries
     */
    public long deleteAllLibraryEntries() {
        long count = testCaseLibraryRepository.count();
        testCaseLibraryRepository.deleteAll();
        log.info("Deleted {} library entries", count);
        return count;
    }

    /**
     * Deletes specific library entries by their IDs
     */
    public long deleteLibraryEntriesByIds(List<String> libraryIds) {
        if (libraryIds == null || libraryIds.isEmpty()) {
            return 0;
        }
        long count = libraryIds.size();
        testCaseLibraryRepository.deleteByIdIn(libraryIds);
        log.info("Deleted {} library entries by IDs", count);
        return count;
    }

    /**
     * Deletes specific test cases from the library
     */
    public Map<String, Object> deleteSpecificLibraryEntries(List<String> libraryIds) {
        Map<String, Object> result = new HashMap<>();
        
        if (libraryIds == null || libraryIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "No library IDs provided");
            result.put("deletedCount", 0);
            return result;
        }
        
        try {
            // Get the entries before deletion for response
            List<TestCaseLibrary> toDelete = testCaseLibraryRepository.findByIdIn(libraryIds);
            
            // Delete them
            long deletedCount = deleteLibraryEntriesByIds(libraryIds);
            
            result.put("success", true);
            result.put("message", "Successfully deleted " + deletedCount + " library entries");
            result.put("deletedCount", deletedCount);
            result.put("deletedEntries", toDelete);
        } catch (Exception e) {
            log.error("Error deleting library entries: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Failed to delete library entries: " + e.getMessage());
            result.put("deletedCount", 0);
        }
        
        return result;
    }
}
