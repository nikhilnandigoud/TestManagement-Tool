package com.qaautomation.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "integration_cache_entries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationCacheEntry {
    @Id
    private String cacheKey;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
