package com.qaautomation.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk deletion of library entries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkDeleteLibraryRequest {
    
    @JsonProperty("libraryIds")
    private List<String> libraryIds;  // IDs of library entries to delete
    
    @JsonProperty("confirmDelete")
    private Boolean confirmDelete;  // Confirmation flag for safety
    
    public boolean isValid() {
        return libraryIds != null && !libraryIds.isEmpty() 
            && confirmDelete != null && confirmDelete;
    }
}
