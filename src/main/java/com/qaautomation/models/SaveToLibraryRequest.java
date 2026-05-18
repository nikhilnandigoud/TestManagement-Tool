package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for saving generated test cases to the library.
 * Contains metadata required for saving test cases with full context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveToLibraryRequest {
    
    @JsonProperty("testCaseIds")
    private List<String> testCaseIds;  // IDs of test cases to save
    
    @JsonProperty("libraryTitle")
    private String libraryTitle;  // Custom title for library storage
    
    @JsonProperty("libraryDescription")
    private String libraryDescription;  // Description for quick reference
    
    @JsonProperty("category")
    private TestCaseLibrary.TestCaseCategory category;  // Positive, Negative, Edge Case, etc.
    
    @JsonProperty("priority")
    private TestCase.Priority priority;  // HIGH, MEDIUM, LOW
    
    @JsonProperty("tags")
    private List<String> tags;  // Tags for searching and filtering
    
    @JsonProperty("qualityScore")
    private String qualityScore;  // Quality score percentage (e.g., "82%")
    
    @JsonProperty("isPublic")
    private Boolean isPublic;  // Public or Private
    
    @JsonProperty("createdBy")
    private String createdBy;  // User saving the test case
    
    @JsonProperty("notes")
    private String notes;  // Additional notes
    
    @JsonProperty("shouldReplace")
    private Boolean shouldReplace;  // Whether to replace existing library entry
    
    @JsonProperty("existingLibraryId")
    private String existingLibraryId;  // ID of existing library entry to replace (if shouldReplace = true)

    /**
     * Validates the request has all required fields.
     */
    public boolean isValid() {
        return testCaseIds != null && !testCaseIds.isEmpty()
            && libraryTitle != null && !libraryTitle.isBlank()
            && priority != null
            && category != null;
    }

    /**
     * Gets validation error message if request is invalid.
     */
    public String getValidationError() {
        if (testCaseIds == null || testCaseIds.isEmpty()) {
            return "Test case IDs are required";
        }
        if (libraryTitle == null || libraryTitle.isBlank()) {
            return "Library title is required";
        }
        if (priority == null) {
            return "Priority is required";
        }
        if (category == null) {
            return "Category is required";
        }
        return null;
    }
}
