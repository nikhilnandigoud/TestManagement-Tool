package com.qaautomation.controllers;

import com.qaautomation.models.JiraTicketDTO;
import com.qaautomation.models.GitHubPRDTO;
import com.qaautomation.models.IntegrationCredentials;
import com.qaautomation.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST Controller for Jira/GitHub integration endpoints
 * Exposes APIs for fetching tickets and PRs
 */
@Slf4j
@RestController
@RequestMapping("/api/integrations")
@CrossOrigin(origins = "*")
public class IntegrationController {

    @Autowired
    private JiraFetchServiceV2 jiraFetchService;

    @Autowired
    private GitHubFetchServiceV2 gitHubFetchService;

    @Autowired
    private CredentialsManagerService credentialsManager;

    @Autowired
    private IntegrationAuditService auditService;

    /**
     * Fetch Jira ticket by ID
     * GET /api/integrations/jira/ticket/{ticketId}
     */
    @GetMapping("/jira/ticket/{ticketId}")
    public ResponseEntity<?> fetchJiraTicket(
            @PathVariable String ticketId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            if (userId == null) userId = "anonymous";

            // Get credentials (user-specific or team-level)
            IntegrationCredentials credentials = credentialsManager.getCredentials(userId, true);
            if (credentials == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Jira credentials not configured"));
            }

            // Fetch ticket
            JiraTicketDTO ticket = jiraFetchService.fetchTicket(ticketId, credentials, userId, ipAddress);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ticket);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid Jira ticket ID: {}", ticketId);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to fetch Jira ticket: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch Jira ticket: " + e.getMessage()));
        }
    }

    /**
     * Fetch GitHub PR by URL
     * POST /api/integrations/github/pr
     * Body: { "url": "https://github.com/owner/repo/pull/123" }
     */
    @PostMapping("/github/pr")
    public ResponseEntity<?> fetchGitHubPR(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            HttpServletRequest httpRequest) {
        try {
            List<String> prUrls = extractPrUrls(request);
            if (prUrls.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'url' or 'urls' parameter"));
            }

            String ipAddress = getClientIpAddress(httpRequest);
            if (userId == null) userId = "anonymous";

            // Get credentials
            IntegrationCredentials credentials = credentialsManager.getCredentials(userId, true);
            if (credentials == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "GitHub credentials not configured"));
            }

            List<GitHubPRDTO> prs = new ArrayList<>();
            for (String prUrl : prUrls) {
                prs.add(gitHubFetchService.fetchPR(prUrl, credentials, userId, ipAddress));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            if (prs.size() == 1) {
                response.put("data", prs.get(0));
            } else {
                response.put("data", Map.of(
                    "pullRequests", prs,
                    "scopeCategories", prs.stream()
                        .flatMap(pr -> pr.getScopeCategories() == null ? java.util.stream.Stream.empty() : pr.getScopeCategories().stream())
                        .distinct()
                        .sorted()
                        .toList(),
                    "totalAdditions", prs.stream().mapToInt(pr -> pr.getTotalAdditions() == null ? 0 : pr.getTotalAdditions()).sum(),
                    "totalDeletions", prs.stream().mapToInt(pr -> pr.getTotalDeletions() == null ? 0 : pr.getTotalDeletions()).sum(),
                    "diffSummary", buildCombinedDiffSummary(prs)
                ));
            }
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid GitHub PR URL: {}", request.get("url"));
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to fetch GitHub PR: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch GitHub PR: " + e.getMessage()));
        }
    }

    /**
     * Save integration credentials
     * POST /api/integrations/settings
     */
    @PostMapping("/settings")
    public ResponseEntity<?> saveCredentials(
            @RequestBody IntegrationCredentials credentials,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Is-Team-Level", defaultValue = "false") boolean isTeamLevel) {
        try {
            if (userId == null) userId = "anonymous";

            // Save credentials
            credentialsManager.saveCredentials(userId, credentials, isTeamLevel);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Credentials saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to save credentials: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to save credentials: " + e.getMessage()));
        }
    }

    /**
     * Test connections to Jira/GitHub
     * POST /api/integrations/test-connections
     */
    @PostMapping("/test-connections")
    public ResponseEntity<?> testConnections(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null) userId = "anonymous";

            IntegrationCredentials credentials = credentialsManager.getCredentials(userId, true);
            if (credentials == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No credentials configured"));
            }

            Map<String, Boolean> results = credentialsManager.testConnections(credentials);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("connections", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to test connections: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to test connections"));
        }
    }

    /**
     * Get audit logs for current user
     * GET /api/integrations/audit-logs?limit=50
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null) userId = "anonymous";

            var logs = auditService.getUserLogs(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("logs", logs);
            response.put("count", logs.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch audit logs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch audit logs"));
        }
    }

    /**
     * Get integration settings
     * GET /api/integrations/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null) userId = "anonymous";

            IntegrationCredentials credentials = credentialsManager.getCredentials(userId, false);
            if (credentials == null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "configured", false
                ));
            }

            // Redact tokens in response
            credentials.setJiraToken(credentials.getJiraToken() != null ? "***" : null);
            credentials.setGithubToken(credentials.getGithubToken() != null ? "***" : null);
            credentials.setGitlabToken(credentials.getGitlabToken() != null ? "***" : null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configured", true);
            response.put("data", credentials);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch settings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch settings"));
        }
    }

    /**
     * Revoke a specific token
     * DELETE /api/integrations/settings/{tokenType}
     */
    @DeleteMapping("/settings/{tokenType}")
    public ResponseEntity<?> revokeToken(
            @PathVariable String tokenType,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null) userId = "anonymous";

            credentialsManager.revokeToken(userId, tokenType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", tokenType + " token revoked");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to revoke token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to revoke token"));
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0]; // Take first IP if multiple
        }
        return clientIp;
    }

    private List<String> extractPrUrls(Map<String, Object> request) {
        List<String> urls = new ArrayList<>();
        Object singleUrl = request.get("url");
        Object multiUrls = request.get("urls");

        if (singleUrl instanceof String singleUrlText && !singleUrlText.isBlank()) {
            urls.addAll(splitPrInput(singleUrlText));
        }
        if (multiUrls instanceof String multiUrlsText && !multiUrlsText.isBlank()) {
            urls.addAll(splitPrInput(multiUrlsText));
        }
        if (multiUrls instanceof List<?> multiUrlsList) {
            multiUrlsList.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .forEach(value -> urls.addAll(splitPrInput(value)));
        }

        return urls.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private List<String> splitPrInput(String input) {
        return java.util.Arrays.stream(input.split("[,\\n\\r]+"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private String buildCombinedDiffSummary(List<GitHubPRDTO> prs) {
        StringBuilder summary = new StringBuilder("## Combined PR Diff Summary\n\n");
        summary.append("**Pull Requests:** ").append(prs.size()).append("\n");
        summary.append("**Total Files Changed:** ")
            .append(prs.stream().mapToInt(pr -> pr.getChangedFilesCount() == null ? 0 : pr.getChangedFilesCount()).sum())
            .append("\n");
        summary.append("**Total Additions:** ")
            .append(prs.stream().mapToInt(pr -> pr.getTotalAdditions() == null ? 0 : pr.getTotalAdditions()).sum())
            .append("\n");
        summary.append("**Total Deletions:** ")
            .append(prs.stream().mapToInt(pr -> pr.getTotalDeletions() == null ? 0 : pr.getTotalDeletions()).sum())
            .append("\n\n");

        prs.forEach(pr -> summary.append("- ")
            .append(pr.getRepository() == null ? "" : pr.getRepository() + " ")
            .append("#").append(pr.getPrNumber())
            .append(": ").append(pr.getTitle())
            .append(" (").append(pr.getChangedFilesCount()).append(" files)\n"));

        return summary.toString();
    }
}
