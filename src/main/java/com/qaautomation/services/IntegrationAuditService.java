package com.qaautomation.services;

import com.qaautomation.models.IntegrationAuditLog;
import com.qaautomation.repositories.IntegrationAuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for audit logging of integration operations
 * Tracks all Jira/GitHub fetches for compliance and debugging
 */
@Slf4j
@Service
public class IntegrationAuditService {

    private static final int RETENTION_DAYS = 30;

    @Autowired
    private IntegrationAuditLogRepository auditLogRepository;

    /**
     * Log an integration operation
     */
    @Transactional
    public void logFetch(String userId, String action, String ticketOrPrId, String sourceSystem,
                        boolean success, String errorMessage, long responseTimeMs, String ipAddress) {
        IntegrationAuditLog auditLog = new IntegrationAuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setTicketOrPrId(ticketOrPrId);
        auditLog.setSourceSystem(sourceSystem);
        auditLog.setSuccess(success);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setResponseTimeMs(responseTimeMs);
        auditLog.setIpAddress(ipAddress);

        auditLogRepository.save(auditLog);

        // Clean up old logs
        cleanupOldLogs();
    }

    /**
     * Get audit logs for a user
     */
    public List<IntegrationAuditLog> getUserLogs(String userId, int limit) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId).stream()
            .limit(limit)
            .toList();
    }

    /**
     * Get all audit logs (for admin/compliance)
     */
    public List<IntegrationAuditLog> getAllLogs(int limit) {
        return auditLogRepository.findAll(
            PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();
    }

    /**
     * Get audit logs by source system
     */
    public List<IntegrationAuditLog> getLogsBySourceSystem(String sourceSystem) {
        return auditLogRepository.findBySourceSystemOrderByTimestampDesc(sourceSystem);
    }

    /**
     * Get failure rate for a time period
     */
    public double getFailureRate(String sourceSystem, long sinceMillis) {
        List<IntegrationAuditLog> recentLogs = auditLogRepository.findBySourceSystemAndTimestampAfter(
            sourceSystem,
            LocalDateTime.now().minusHours(Math.max(1, sinceMillis / 3600000))
        );

        if (recentLogs.isEmpty()) {
            return 0;
        }

        long failures = recentLogs.stream()
            .filter(log -> !log.getSuccess())
            .count();

        return (double) failures / recentLogs.size();
    }

    /**
     * Remove logs older than retention period
     */
    @Transactional
    private void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        auditLogRepository.deleteByTimestampBefore(cutoff);
    }

    /**
     * Export audit logs as CSV
     */
    public String exportLogsAsCSV(List<IntegrationAuditLog> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,User ID,Action,Ticket/PR,Source,Success,Error,Response Time (ms),IP\n");

        for (IntegrationAuditLog log : logs) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\",%d,\"%s\"\n",
                log.getTimestamp(),
                log.getUserId(),
                log.getAction(),
                log.getTicketOrPrId(),
                log.getSourceSystem(),
                log.getSuccess(),
                log.getErrorMessage() != null ? log.getErrorMessage().replace("\"", "\"\"") : "",
                log.getResponseTimeMs(),
                log.getIpAddress()));
        }

        return csv.toString();
    }
}
