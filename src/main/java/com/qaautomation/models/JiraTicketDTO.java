package com.qaautomation.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Jira ticket fetch response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JiraTicketDTO {
    private String id;
    private String key;
    private String title;
    private String description;
    private String acceptanceCriteria;
    private String status;
    private String assignee;
    private String reporter;
    private String priority;
    private String sprint;
    private Integer storyPoints;
    private List<String> labels;
    private List<String> linkedIssues;
    private List<String> attachments;
    private LocalDateTime fetchedAt;
    private String sourceUrl;
    
    // Additional metadata
    private String issueType;
    private String projectKey;
    private LocalDateTime created;
    private LocalDateTime updated;
}
