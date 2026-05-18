package com.qaautomation.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Integration credentials storage model
 */
@Entity
@Table(name = "integration_credentials")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationCredentials {
    @Id
    private String id;

    @Column(name = "jira_workspace_url")
    private String jiraWorkspaceUrl;

    @Column(name = "jira_auth_method")
    private String jiraAuthMethod; // OAuth or PAT

    @Column(name = "jira_token", length = 4096)
    private String jiraToken; // Encrypted

    @Column(name = "jira_token_expiry")
    private String jiraTokenExpiry;

    @Column(name = "github_token", length = 4096)
    private String githubToken; // Encrypted (PAT with repo:read scope)

    @Column(name = "github_token_expiry")
    private String githubTokenExpiry;

    @Column(name = "gitlab_token", length = 4096)
    private String gitlabToken; // Encrypted (optional, P2)

    @Column(name = "gitlab_token_expiry")
    private String gitlabTokenExpiry;

    @Column(name = "user_id")
    private String userId; // Which user configured this

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "is_team_level")
    private Boolean isTeamLevel; // Configured by QA Lead for entire team
}
