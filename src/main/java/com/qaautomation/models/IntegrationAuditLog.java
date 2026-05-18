package com.qaautomation.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Audit log for integration fetches
 */
@Entity
@Table(name = "integration_audit_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationAuditLog {
    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    private String action; // JIRA_FETCH, GITHUB_FETCH, etc.

    @Column(name = "ticket_or_pr_id")
    private String ticketOrPrId;

    @Column(name = "source_system")
    private String sourceSystem; // JIRA, GITHUB, GITLAB

    private Boolean success;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    private LocalDateTime timestamp;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "ip_address")
    private String ipAddress;
}
