package com.qaautomation.controllers;

import com.qaautomation.agents.UserGuideProcessor;
import com.qaautomation.models.TestCase;
import com.qaautomation.models.TestCaseGenerationRequest;
import com.qaautomation.models.TestCaseGenerationResponse;
import com.qaautomation.models.TestCaseLibrary;
import com.qaautomation.models.SaveToLibraryRequest;
import com.qaautomation.models.BulkDeleteLibraryRequest;
import com.qaautomation.services.TestCaseGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for test case generation endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/agents/testcases")
@CrossOrigin(origins = "*")
public class TestCaseGenerationController {

    private final TestCaseGenerationService testCaseGenerationService;
    private final UserGuideProcessor userGuideProcessor;

    public TestCaseGenerationController(TestCaseGenerationService testCaseGenerationService, UserGuideProcessor userGuideProcessor) {
        this.testCaseGenerationService = testCaseGenerationService;
        this.userGuideProcessor = userGuideProcessor;
    }

    /**
     * POST /agents/testcases/generate - Generates test cases from requirement text.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateTestCases(@RequestBody TestCaseGenerationRequest request) {
        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Received test case generation request: {}", requestId);

            if (request.getText() == null || request.getText().isEmpty()) {
                if (request.getFileId() == null || request.getFileId().isEmpty()) {
                    return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Either 'text' or 'fileId' must be provided"));
                }
                // TODO: Fetch text from file service using fileId
            }

            List<TestCase> testCases = testCaseGenerationService.generateTestCases(request);

            TestCaseGenerationResponse response = TestCaseGenerationResponse.builder()
                .requestId(requestId)
                .status("SUCCESS")
                .testCases(testCases)
                .totalGenerated(testCases.size())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

            log.info("Successfully generated {} test cases for request {}", testCases.size(), requestId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating test cases for request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to generate test cases: " + e.getMessage()));
        }
    }

    /**
     * POST /agents/testcases - Also supports generation for backwards compatibility.
     */
    @PostMapping
    public ResponseEntity<?> generateTestCasesAlt(@RequestBody TestCaseGenerationRequest request) {
        return generateTestCases(request);
    }

    /**
     * GET /agents/testcases/{testCaseId} - Retrieves a test case.
     */
    @GetMapping("/{testCaseId}")
    public ResponseEntity<?> getTestCase(@PathVariable String testCaseId) {
        try {
            TestCase testCase = testCaseGenerationService.getTestCaseById(testCaseId);

            if (testCase == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(testCase);

        } catch (Exception e) {
            log.error("Error retrieving test case {}: {}", testCaseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve test case"));
        }
    }

    /**
     * GET /agents/testcases - Retrieves all test cases.
     */
    @GetMapping
    public ResponseEntity<?> getAllTestCases() {
        try {
            List<TestCase> testCases = testCaseGenerationService.getAllTestCases();
            return ResponseEntity.ok(testCases);
        } catch (Exception e) {
            log.error("Error retrieving test cases: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve test cases"));
        }
    }

    /**
     * GET /agents/testcases/guides/folders - Lists all available guide folders with files in each.
     */
    @GetMapping("/guides/folders")
    public ResponseEntity<?> listAvailableGuideFolders() {
        try {
            Map<String, List<String>> availableFolders = userGuideProcessor.listAvailableGuideFolders();
            log.info("Retrieved {} available guide folders", availableFolders.size());
            return ResponseEntity.ok(availableFolders);
        } catch (Exception e) {
            log.error("Error listing guide folders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to list guide folders"));
        }
    }

    /**
     * GET /agents/testcases/guides/available - Lists all available user guides (legacy support).
     */
    @GetMapping("/guides/available")
    public ResponseEntity<?> listAvailableGuides() {
        try {
            Map<String, String> availableGuides = userGuideProcessor.listAvailableGuides();
            log.info("Retrieved {} available user guides", availableGuides.size());
            return ResponseEntity.ok(availableGuides);
        } catch (Exception e) {
            log.error("Error listing user guides: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to list user guides"));
        }
    }

    /**
     * POST /agents/testcases/generate/with-guide - Generates test cases with user guide context.
     */
    @PostMapping("/generate/with-guide")
    public ResponseEntity<?> generateTestCasesWithGuide(@RequestBody TestCaseGenerationRequest request) {
        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Received test case generation request with guide: {} (Guide: {})", 
                requestId, request.getUserGuideFileName());

            if (request.getText() == null || request.getText().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("'text' (requirement) must be provided"));
            }

            // Use the service which will route to generateTestCasesWithGuide if guide is provided
            List<TestCase> testCases = testCaseGenerationService.generateTestCasesWithGuide(request);

            TestCaseGenerationResponse response = TestCaseGenerationResponse.builder()
                .requestId(requestId)
                .status("SUCCESS")
                .testCases(testCases)
                .totalGenerated(testCases.size())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .message("Test cases generated with user guide: " + (request.getUserGuideFileName() != null ? request.getUserGuideFileName() : "None"))
                .build();

            log.info("Successfully generated {} test cases with guide for request {}", testCases.size(), requestId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating test cases with guide for request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to generate test cases: " + e.getMessage()));
        }
    }

    /**
     * POST /agents/testcases/library/save - Saves test cases to personal library
     */
    @PostMapping("/library/save")
    public ResponseEntity<?> saveTestCasesToLibrary(@RequestBody SaveToLibraryRequest request) {
        try {
            log.info("Received save to library request for {} test cases", request.getTestCaseIds().size());
            
            List<TestCaseLibrary> savedEntries = testCaseGenerationService.saveTestCasesToLibrary(request);
            
            return ResponseEntity.ok(new SaveToLibraryResponse(
                "SUCCESS",
                "Successfully saved " + savedEntries.size() + " test case(s) to library",
                savedEntries
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid save to library request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error saving test cases to library: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to save test cases: " + e.getMessage()));
        }
    }

    /**
     * GET /agents/testcases/library - Retrieves all library entries
     */
    @GetMapping("/library")
    public ResponseEntity<?> getAllLibraryEntries() {
        try {
            List<TestCaseLibrary> entries = testCaseGenerationService.getAllLibraryEntries();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            log.error("Error retrieving library entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve library entries"));
        }
    }

    /**
     * GET /agents/testcases/library/{libraryId} - Retrieves a specific library entry
     */
    @GetMapping("/library/{libraryId}")
    public ResponseEntity<?> getLibraryEntry(@PathVariable String libraryId) {
        try {
            TestCaseLibrary entry = testCaseGenerationService.getLibraryEntryById(libraryId);
            if (entry == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            log.error("Error retrieving library entry {}: {}", libraryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve library entry"));
        }
    }

    /**
     * GET /agents/testcases/library/search - Searches library entries
     */
    @GetMapping("/library/search")
    public ResponseEntity<?> searchLibrary(@RequestParam String query) {
        try {
            List<TestCaseLibrary> results = testCaseGenerationService.searchLibrary(query);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching library: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to search library"));
        }
    }

    /**
     * PUT /agents/testcases/library/{libraryId} - Updates a library entry
     */
    @PutMapping("/library/{libraryId}")
    public ResponseEntity<?> updateLibraryEntry(@PathVariable String libraryId, @RequestBody SaveToLibraryRequest request) {
        try {
            TestCaseLibrary updated = testCaseGenerationService.updateLibraryEntry(libraryId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Library entry not found: {}", libraryId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating library entry {}: {}", libraryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to update library entry"));
        }
    }

    /**
     * POST /agents/testcases/library/delete-multiple - Deletes specific library entries
     */
    @PostMapping("/library/delete-multiple")
    public ResponseEntity<?> deleteMultipleLibraryEntries(@RequestBody BulkDeleteLibraryRequest request) {
        try {
            if (request == null || !request.isValid()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid request: library IDs and confirmation required"));
            }
            
            log.info("Deleting {} library entries with confirmation", request.getLibraryIds().size());
            
            Map<String, Object> result = testCaseGenerationService.deleteSpecificLibraryEntries(request.getLibraryIds());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error deleting library entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to delete library entries: " + e.getMessage()));
        }
    }

    /**
     * DELETE /agents/testcases/library/{libraryId} - Deletes a library entry
     */
    @DeleteMapping("/library/{libraryId}")
    public ResponseEntity<?> deleteLibraryEntry(@PathVariable String libraryId) {
        try {
            testCaseGenerationService.deleteLibraryEntry(libraryId);
            return ResponseEntity.ok(new MessageResponse("Library entry deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting library entry {}: {}", libraryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to delete library entry"));
        }
    }

    /**
     * GET /agents/testcases/library/category/{category} - Retrieves library entries by category
     */
    @GetMapping("/library/category/{category}")
    public ResponseEntity<?> getLibraryByCategory(@PathVariable String category) {
        try {
            TestCaseLibrary.TestCaseCategory catEnum = TestCaseLibrary.TestCaseCategory.valueOf(category.toUpperCase());
            List<TestCaseLibrary> entries = testCaseGenerationService.getLibraryByCategory(catEnum);
            return ResponseEntity.ok(entries);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid category: " + category));
        } catch (Exception e) {
            log.error("Error retrieving library by category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve library entries"));
        }
    }

    /**
     * GET /agents/testcases/library/public - Retrieves all public library entries
     */
    @GetMapping("/library/public/all")
    public ResponseEntity<?> getPublicLibraryEntries() {
        try {
            List<TestCaseLibrary> entries = testCaseGenerationService.getPublicLibraryEntries();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            log.error("Error retrieving public library entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve public entries"));
        }
    }

    /**
     * POST /agents/testcases/library/{libraryId}/use - Increments usage count for a library entry
     */
    @PostMapping("/library/{libraryId}/use")
    public ResponseEntity<?> incrementLibraryUsage(@PathVariable String libraryId) {
        try {
            TestCaseLibrary updated = testCaseGenerationService.incrementLibraryUsageCount(libraryId);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error incrementing usage count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to increment usage count"));
        }
    }

    /**
     * Response class for save to library
     */
    public static class SaveToLibraryResponse {
        public String status;
        public String message;
        public List<TestCaseLibrary> savedEntries;

        public SaveToLibraryResponse(String status, String message, List<TestCaseLibrary> savedEntries) {
            this.status = status;
            this.message = message;
            this.savedEntries = savedEntries;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public List<TestCaseLibrary> getSavedEntries() {
            return savedEntries;
        }
    }

    /**
     * Response class for simple messages
     */
    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Error response DTO.
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
