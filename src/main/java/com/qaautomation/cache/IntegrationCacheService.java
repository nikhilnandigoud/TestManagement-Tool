package com.qaautomation.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaautomation.models.JiraTicketDTO;
import com.qaautomation.models.GitHubPRDTO;
import com.qaautomation.models.IntegrationCacheEntry;
import com.qaautomation.repositories.IntegrationCacheEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cache service for integration responses
 * Implements database-backed caching with TTL. This is deployable without a
 * separate Redis service and can be swapped for Redis behind this same API.
 */
@Slf4j
@Service
public class IntegrationCacheService {

    @Value("${cache.ttl-minutes:5}")
    private int cacheTtlMinutes;

    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private IntegrationCacheEntryRepository cacheRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Initialize cache with cleanup task
     */
    public IntegrationCacheService() {
        // Schedule periodic cleanup of expired entries
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredEntries,
            CLEANUP_INTERVAL_MS,
            CLEANUP_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cache a Jira ticket
     */
    public void cacheJiraTicket(String workspaceId, String ticketId, JiraTicketDTO ticket) {
        String key = buildJiraCacheKey(workspaceId, ticketId);
        saveCacheEntry(key, "JIRA", ticket);
        log.debug("Cached Jira ticket: {} (TTL: {} minutes)", key, cacheTtlMinutes);
    }

    /**
     * Retrieve cached Jira ticket
     */
    public Optional<JiraTicketDTO> getJiraTicket(String workspaceId, String ticketId) {
        String key = buildJiraCacheKey(workspaceId, ticketId);
        return readCacheEntry(key, JiraTicketDTO.class);
    }

    /**
     * Cache a GitHub PR
     */
    public void cacheGitHubPR(String owner, String repo, String prNumber, GitHubPRDTO pr) {
        String key = buildGitHubCacheKey(owner, repo, prNumber);
        saveCacheEntry(key, "GITHUB", pr);
        log.debug("Cached GitHub PR: {} (TTL: {} minutes)", key, cacheTtlMinutes);
    }

    /**
     * Retrieve cached GitHub PR
     */
    public Optional<GitHubPRDTO> getGitHubPR(String owner, String repo, String prNumber) {
        String key = buildGitHubCacheKey(owner, repo, prNumber);
        return readCacheEntry(key, GitHubPRDTO.class);
    }

    /**
     * Invalidate Jira ticket cache
     */
    public void invalidateJiraTicket(String workspaceId, String ticketId) {
        String key = buildJiraCacheKey(workspaceId, ticketId);
        cacheRepository.deleteById(key);
        log.debug("Invalidated Jira cache: {}", key);
    }

    /**
     * Invalidate GitHub PR cache
     */
    public void invalidateGitHubPR(String owner, String repo, String prNumber) {
        String key = buildGitHubCacheKey(owner, repo, prNumber);
        cacheRepository.deleteById(key);
        log.debug("Invalidated GitHub cache: {}", key);
    }

    /**
     * Clear all Jira cache
     */
    public void clearJiraCache() {
        cacheRepository.findAll().stream()
            .filter(entry -> "JIRA".equals(entry.getSourceSystem()))
            .forEach(entry -> cacheRepository.deleteById(entry.getCacheKey()));
        log.info("Cleared all Jira cache");
    }

    /**
     * Clear all GitHub cache
     */
    public void clearGitHubCache() {
        cacheRepository.findAll().stream()
            .filter(entry -> "GITHUB".equals(entry.getSourceSystem()))
            .forEach(entry -> cacheRepository.deleteById(entry.getCacheKey()));
        log.info("Cleared all GitHub cache");
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        long jiraEntries = cacheRepository.findAll().stream()
            .filter(entry -> "JIRA".equals(entry.getSourceSystem()))
            .count();
        long githubEntries = cacheRepository.findAll().stream()
            .filter(entry -> "GITHUB".equals(entry.getSourceSystem()))
            .count();
        return new CacheStats(
            (int) jiraEntries,
            (int) githubEntries,
            cacheTtlMinutes
        );
    }

    /**
     * Remove expired entries from both caches
     */
    private void cleanupExpiredEntries() {
        cacheRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("Cache cleanup completed");
    }

    private <T> void saveCacheEntry(String key, String sourceSystem, T payload) {
        try {
            IntegrationCacheEntry entry = new IntegrationCacheEntry();
            entry.setCacheKey(key);
            entry.setSourceSystem(sourceSystem);
            entry.setPayloadJson(objectMapper.writeValueAsString(payload));
            entry.setCreatedAt(LocalDateTime.now());
            entry.setExpiresAt(LocalDateTime.now().plusMinutes(cacheTtlMinutes));
            cacheRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to cache {} entry {}: {}", sourceSystem, key, e.getMessage());
        }
    }

    private <T> Optional<T> readCacheEntry(String key, Class<T> type) {
        try {
            Optional<IntegrationCacheEntry> cached = cacheRepository.findById(key);
            if (cached.isEmpty()) {
                return Optional.empty();
            }
            IntegrationCacheEntry entry = cached.get();
            if (entry.getExpiresAt().isBefore(LocalDateTime.now())) {
                cacheRepository.deleteById(key);
                return Optional.empty();
            }
            log.debug("Cache hit for integration entry: {}", key);
            return Optional.of(objectMapper.readValue(entry.getPayloadJson(), type));
        } catch (Exception e) {
            log.warn("Failed to read cache entry {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Build cache key for Jira
     */
    private String buildJiraCacheKey(String workspaceId, String ticketId) {
        return String.format("jira:%s:%s", workspaceId, ticketId.toLowerCase());
    }

    /**
     * Build cache key for GitHub
     */
    private String buildGitHubCacheKey(String owner, String repo, String prNumber) {
        return String.format("github:%s:%s:%s", owner.toLowerCase(), repo.toLowerCase(), prNumber);
    }

    /**
     * Shutdown cache service
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cache statistics DTO
     */
    public static class CacheStats {
        public final int jiraEntries;
        public final int githubEntries;
        public final int ttlMinutes;

        CacheStats(int jiraEntries, int githubEntries, int ttlMinutes) {
            this.jiraEntries = jiraEntries;
            this.githubEntries = githubEntries;
            this.ttlMinutes = ttlMinutes;
        }
    }
}
