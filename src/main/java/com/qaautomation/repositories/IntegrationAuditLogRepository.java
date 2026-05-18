package com.qaautomation.repositories;

import com.qaautomation.models.IntegrationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationAuditLogRepository extends JpaRepository<IntegrationAuditLog, String> {
    List<IntegrationAuditLog> findByUserIdOrderByTimestampDesc(String userId);
    List<IntegrationAuditLog> findBySourceSystemOrderByTimestampDesc(String sourceSystem);
    List<IntegrationAuditLog> findBySourceSystemAndTimestampAfter(String sourceSystem, LocalDateTime timestamp);
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
