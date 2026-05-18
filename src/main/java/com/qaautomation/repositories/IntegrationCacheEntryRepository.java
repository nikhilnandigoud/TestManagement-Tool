package com.qaautomation.repositories;

import com.qaautomation.models.IntegrationCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface IntegrationCacheEntryRepository extends JpaRepository<IntegrationCacheEntry, String> {
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
