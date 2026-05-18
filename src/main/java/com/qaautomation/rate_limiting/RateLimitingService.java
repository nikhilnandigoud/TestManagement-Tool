package com.qaautomation.rate_limiting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service for external API calls
 * Implements token bucket algorithm for Jira and GitHub APIs
 */
@Slf4j
@Service
public class RateLimitingService {

    // Jira: 300 requests per minute
    private static final int JIRA_RATE_LIMIT = 300;
    private static final long JIRA_WINDOW_MS = 60 * 1000; // 1 minute

    // GitHub: 5000 requests per hour
    private static final int GITHUB_RATE_LIMIT = 5000;
    private static final long GITHUB_WINDOW_MS = 60 * 60 * 1000; // 1 hour

    // Exponential backoff parameters
    private static final int INITIAL_BACKOFF_MS = 1000; // 1 second
    private static final int MAX_BACKOFF_MS = 30000; // 30 seconds
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final Map<String, RateLimitBucket> jiraLimiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimitBucket> githubLimiters = new ConcurrentHashMap<>();
    private final Map<String, ExponentialBackoff> backoffTrackers = new ConcurrentHashMap<>();

    /**
     * Check if Jira request is allowed
     */
    public boolean allowJiraRequest(String workspaceId) {
        String key = "jira:" + workspaceId;
        RateLimitBucket bucket = jiraLimiters.computeIfAbsent(key, 
            k -> new RateLimitBucket(JIRA_RATE_LIMIT, JIRA_WINDOW_MS));

        if (bucket.allowRequest()) {
            log.debug("Jira request allowed for workspace: {}", workspaceId);
            return true;
        }

        log.warn("Jira rate limit exceeded for workspace: {}", workspaceId);
        return false;
    }

    /**
     * Check if GitHub request is allowed
     */
    public boolean allowGitHubRequest(String owner) {
        String key = "github:" + owner;
        RateLimitBucket bucket = githubLimiters.computeIfAbsent(key,
            k -> new RateLimitBucket(GITHUB_RATE_LIMIT, GITHUB_WINDOW_MS));

        if (bucket.allowRequest()) {
            log.debug("GitHub request allowed for owner: {}", owner);
            return true;
        }

        log.warn("GitHub rate limit exceeded for owner: {}", owner);
        return false;
    }

    /**
     * Get remaining requests for Jira
     */
    public int getJiraRemaining(String workspaceId) {
        String key = "jira:" + workspaceId;
        RateLimitBucket bucket = jiraLimiters.get(key);
        return bucket != null ? bucket.getRemaining() : JIRA_RATE_LIMIT;
    }

    /**
     * Get remaining requests for GitHub
     */
    public int getGitHubRemaining(String owner) {
        String key = "github:" + owner;
        RateLimitBucket bucket = githubLimiters.get(key);
        return bucket != null ? bucket.getRemaining() : GITHUB_RATE_LIMIT;
    }

    /**
     * Record a rate limit hit and calculate backoff
     */
    public long getBackoffMs(String key) {
        ExponentialBackoff backoff = backoffTrackers.computeIfAbsent(key,
            k -> new ExponentialBackoff());

        long backoffMs = backoff.getNextBackoff();
        log.warn("Rate limit hit for {}. Backing off for {} ms", key, backoffMs);
        return backoffMs;
    }

    /**
     * Reset backoff on success
     */
    public void resetBackoff(String key) {
        backoffTrackers.remove(key);
    }

    /**
     * Get rate limit status
     */
    public RateLimitStatus getStatus(String key) {
        if (key.startsWith("jira:")) {
            RateLimitBucket bucket = jiraLimiters.get(key);
            return new RateLimitStatus(
                JIRA_RATE_LIMIT,
                bucket != null ? bucket.getRemaining() : JIRA_RATE_LIMIT,
                bucket != null ? bucket.getResetTime() : System.currentTimeMillis() + JIRA_WINDOW_MS
            );
        } else if (key.startsWith("github:")) {
            RateLimitBucket bucket = githubLimiters.get(key);
            return new RateLimitStatus(
                GITHUB_RATE_LIMIT,
                bucket != null ? bucket.getRemaining() : GITHUB_RATE_LIMIT,
                bucket != null ? bucket.getResetTime() : System.currentTimeMillis() + GITHUB_WINDOW_MS
            );
        }
        return null;
    }

    /**
     * Rate limit bucket using token bucket algorithm
     */
    private static class RateLimitBucket {
        private final int capacity;
        private final long windowMs;
        private int tokens;
        private long lastRefillTime;

        RateLimitBucket(int capacity, long windowMs) {
            this.capacity = capacity;
            this.windowMs = windowMs;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean allowRequest() {
            refillTokens();

            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        synchronized int getRemaining() {
            refillTokens();
            return tokens;
        }

        synchronized long getResetTime() {
            return lastRefillTime + windowMs;
        }

        private void refillTokens() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;

            if (timePassed >= windowMs) {
                tokens = capacity;
                lastRefillTime = now;
            }
        }
    }

    /**
     * Exponential backoff tracker
     */
    private static class ExponentialBackoff {
        private int retries = 0;

        long getNextBackoff() {
            long backoffMs = (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, retries));
            backoffMs = Math.min(backoffMs, MAX_BACKOFF_MS);
            retries++;
            return backoffMs;
        }
    }

    /**
     * Rate limit status DTO
     */
    public static class RateLimitStatus {
        public final int limit;
        public final int remaining;
        public final long resetTime;

        RateLimitStatus(int limit, int remaining, long resetTime) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }
    }
}
