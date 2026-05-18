package com.qaautomation.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for GitHub PR fetch response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitHubPRDTO {
    private String id;
    private String title;
    private String body;
    private String baseBranch;
    private String headBranch;
    private String state; // open, closed, merged
    private String mergeStatus; // merged, mergeable, conflict
    private Integer prNumber;
    private String repository;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // File changes
    private List<FileChangeDTO> filesChanged;
    private Integer totalAdditions;
    private Integer totalDeletions;
    
    // Diff summary
    private String diffSummary;
    private Integer changedFilesCount;
    
    // Scope categorization
    private List<String> scopeCategories; // Frontend, Backend, Database, Auth, Payments, etc.
    
    // Additional metadata
    private List<String> labels;
    private List<String> reviewers;
    private String sourceUrl;
    private LocalDateTime fetchedAt;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileChangeDTO {
        private String filename;
        private String status; // added, modified, deleted
        private Integer additions;
        private Integer deletions;
        private String patch; // diff content (truncated if large)
        private String fileType; // extension
        private Boolean isSensitive; // secrets, env files, etc.
        private String scopeCategory; // auto-determined
    }
}
