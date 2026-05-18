package com.qaautomation.services;

import com.qaautomation.models.GitHubPRDTO;
import com.qaautomation.models.IntegrationCredentials;
import com.qaautomation.cache.IntegrationCacheService;
import com.qaautomation.rate_limiting.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for fetching GitHub PRs with file analysis, scope categorization, and caching
 */
@Slf4j
@Service
public class GitHubFetchServiceV2 {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IntegrationCacheService cacheService;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private IntegrationAuditService auditService;

    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final Pattern GITHUB_URL_PATTERN = 
        Pattern.compile("github\\.com/([^/]+)/([^/]+)/pull/(\\d+)");
    private static final Pattern GITHUB_SHORT_PATTERN =
        Pattern.compile("^([^/\\s]+)/([^#\\s]+)#(\\d+)$");
    private static final int LARGE_PR_LINE_THRESHOLD = 500;
    private static final int DIFF_TRUNCATION_LINES = 200;

    /**
     * Fetch GitHub PR with all metadata and file analysis
     */
    public GitHubPRDTO fetchPR(String prUrl, IntegrationCredentials credentials, String userId, String ipAddress) throws Exception {
        long startTime = System.currentTimeMillis();
        String action = "GITHUB_FETCH";
        boolean success = false;
        String errorMessage = null;

        try {
            if (credentials == null || credentials.getGithubToken() == null) {
                throw new IllegalArgumentException("GitHub credentials not configured");
            }

            GitHubPRRef prRef = parsePRReference(prUrl);
            String owner = prRef.owner();
            String repo = prRef.repo();
            String prNumber = prRef.prNumber();

            String cacheKey = owner + ":" + repo + ":" + prNumber;

            // Check cache
            Optional<GitHubPRDTO> cached = cacheService.getGitHubPR(owner, repo, prNumber);
            if (cached.isPresent()) {
                log.info("Returning cached GitHub PR: {}", prUrl);
                auditService.logFetch(userId, action, cacheKey, "GITHUB", true, null,
                    System.currentTimeMillis() - startTime, ipAddress);
                return cached.get();
            }

            // Check rate limit
            if (!rateLimitingService.allowGitHubRequest(owner)) {
                long backoffMs = rateLimitingService.getBackoffMs("github:" + owner);
                errorMessage = "Rate limit exceeded. Retry after " + backoffMs + "ms";
                log.warn(errorMessage);
                auditService.logFetch(userId, action, cacheKey, "GITHUB", false, errorMessage,
                    System.currentTimeMillis() - startTime, ipAddress);
                throw new RuntimeException(errorMessage);
            }

            // Fetch PR metadata
            GitHubPRDTO pr = fetchPRMetadata(owner, repo, prNumber, credentials);

            // Fetch PR files and analyze
            List<GitHubPRDTO.FileChangeDTO> files = fetchPRFiles(owner, repo, prNumber, credentials);
            pr.setFilesChanged(files);
            pr.setChangedFilesCount(files.size());

            // Analyze diffs
            analyzeDiffs(files);

            // Generate summary
            String summary = generateDiffSummary(files);
            pr.setDiffSummary(summary);
            pr.setTotalAdditions(files.stream().mapToInt(GitHubPRDTO.FileChangeDTO::getAdditions).sum());
            pr.setTotalDeletions(files.stream().mapToInt(GitHubPRDTO.FileChangeDTO::getDeletions).sum());
            pr.setScopeCategories(files.stream()
                .map(GitHubPRDTO.FileChangeDTO::getScopeCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList());

            // Add metadata
            pr.setSourceUrl(prUrl);
            pr.setFetchedAt(LocalDateTime.now());

            // Cache result
            cacheService.cacheGitHubPR(owner, repo, prNumber, pr);

            // Reset backoff
            rateLimitingService.resetBackoff("github:" + owner);
            success = true;

            log.info("Successfully fetched GitHub PR: {}", prUrl);
            return pr;

        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Failed to fetch GitHub PR {}: {}", prUrl, e.getMessage(), e);
            throw e;
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            String prId = prUrl.replaceAll(".*/pull/", "");
            auditService.logFetch(userId, action, prId, "GITHUB", success, errorMessage, responseTime, ipAddress);
        }
    }

    /**
     * Fetch PR metadata (title, description, status, etc.)
     */
    private GitHubPRDTO fetchPRMetadata(String owner, String repo, String prNumber, 
                                        IntegrationCredentials credentials) throws Exception {
        String url = GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/pulls/" + prNumber;
        String response = makeGitHubRequest(url, credentials);

        JsonNode root = objectMapper.readTree(response);
        GitHubPRDTO pr = new GitHubPRDTO();

        pr.setId(root.path("id").asText());
        pr.setPrNumber(root.path("number").asInt());
        pr.setRepository(owner + "/" + repo);
        pr.setTitle(root.path("title").asText());
        pr.setBody(root.path("body").asText());
        pr.setAuthor(root.path("user").path("login").asText());
        pr.setState(root.path("state").asText());
        pr.setMergeStatus(resolveMergeStatus(root));
        pr.setBaseBranch(root.path("base").path("ref").asText());
        pr.setHeadBranch(root.path("head").path("ref").asText());

        List<String> labels = new ArrayList<>();
        root.path("labels").forEach(label -> labels.add(label.path("name").asText()));
        pr.setLabels(labels);

        List<String> reviewers = new ArrayList<>();
        root.path("requested_reviewers").forEach(reviewer -> reviewers.add(reviewer.path("login").asText()));
        pr.setReviewers(reviewers);
        
        // Parse timestamps
        String createdStr = root.path("created_at").asText();
        if (!createdStr.isEmpty()) {
            pr.setCreatedAt(parseGitHubTimestamp(createdStr));
        }
        
        String updatedStr = root.path("updated_at").asText();
        if (!updatedStr.isEmpty()) {
            pr.setUpdatedAt(parseGitHubTimestamp(updatedStr));
        }

        return pr;
    }

    /**
     * Fetch all files changed in the PR with pagination
     */
    private List<GitHubPRDTO.FileChangeDTO> fetchPRFiles(String owner, String repo, String prNumber,
                                                         IntegrationCredentials credentials) throws Exception {
        List<GitHubPRDTO.FileChangeDTO> allFiles = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            String url = String.format("%s/repos/%s/%s/pulls/%s/files?page=%d&per_page=%d",
                GITHUB_API_URL, owner, repo, prNumber, page, perPage);

            String response = makeGitHubRequest(url, credentials);
            JsonNode files = objectMapper.readTree(response);

            if (files.isEmpty()) break;

            for (JsonNode fileNode : files) {
                GitHubPRDTO.FileChangeDTO file = new GitHubPRDTO.FileChangeDTO();
                file.setFilename(fileNode.path("filename").asText());
                file.setStatus(fileNode.path("status").asText());
                file.setAdditions(fileNode.path("additions").asInt());
                file.setDeletions(fileNode.path("deletions").asInt());
                file.setPatch(fileNode.path("patch").asText());
                
                // Determine file type
                String filename = file.getFilename();
                if (filename.endsWith(".tsx") || filename.endsWith(".ts") || filename.endsWith(".jsx") 
                    || filename.endsWith(".js") || filename.endsWith(".css") || filename.endsWith(".scss")) {
                    file.setFileType("Frontend");
                } else if (filename.endsWith(".java") || filename.endsWith(".py") || filename.endsWith(".go")
                          || filename.contains("src/api") || filename.contains("src/service")) {
                    file.setFileType("Backend");
                } else if (filename.contains("migration") || filename.contains("db") || filename.endsWith(".sql")) {
                    file.setFileType("Database");
                } else if (filename.contains("auth") || filename.contains("security")) {
                    file.setFileType("Auth");
                } else {
                    file.setFileType("Other");
                }

                // Categorize scope
                file.setScopeCategory(categorizeScope(filename, file.getFileType()));

                // Check if sensitive
                file.setIsSensitive(isSensitiveFile(filename));

                allFiles.add(file);
            }

            // Check if more pages
            if (files.size() < perPage) break;
            page++;
        }

        return allFiles;
    }

    /**
     * Categorize file scope based on path
     */
    private String categorizeScope(String filename, String fileType) {
        filename = filename.toLowerCase();

        if (filename.contains("src/components") || filename.contains("src/ui") || 
            filename.contains("src/pages") || filename.endsWith(".css") || filename.endsWith(".scss")) {
            return "Frontend";
        } else if (filename.contains("migrations") || filename.contains("db/") || 
                  filename.contains("schema") || filename.endsWith(".sql")) {
            return "Database";
        } else if (filename.contains("auth") || filename.contains("security") || 
                  filename.contains("permission")) {
            return "Auth";
        } else if (filename.contains("payment") || filename.contains("billing") || 
                  filename.contains("stripe")) {
            return "Payments";
        } else if (filename.contains("src/api") || filename.contains("src/service") || 
                  filename.contains("src/handler")) {
            return "Backend";
        }

        return fileType;
    }

    /**
     * Analyze diffs - truncate large patches
     */
    private void analyzeDiffs(List<GitHubPRDTO.FileChangeDTO> files) {
        int totalChangedLines = files.stream()
            .mapToInt(file -> safeInt(file.getAdditions()) + safeInt(file.getDeletions()))
            .sum();
        boolean largePR = totalChangedLines > LARGE_PR_LINE_THRESHOLD;

        for (GitHubPRDTO.FileChangeDTO file : files) {
            if (Boolean.TRUE.equals(file.getIsSensitive())) {
                file.setPatch(null);
                continue;
            }

            if (largePR) {
                file.setPatch(null);
                continue;
            }

            String patch = file.getPatch();
            if (patch != null && patch.length() > 0) {
                String[] lines = patch.split("\n");
                if (lines.length > DIFF_TRUNCATION_LINES) {
                    StringBuilder truncated = new StringBuilder();
                    for (int i = 0; i < DIFF_TRUNCATION_LINES; i++) {
                        truncated.append(lines[i]).append("\n");
                    }
                    truncated.append("\n... (").append(lines.length - DIFF_TRUNCATION_LINES)
                             .append(" more lines)");
                    file.setPatch(truncated.toString());
                    log.debug("Truncated diff for {}: {} lines -> {}", 
                             file.getFilename(), lines.length, DIFF_TRUNCATION_LINES);
                }
            }
        }
    }

    /**
     * Generate human-readable diff summary
     */
    private String generateDiffSummary(List<GitHubPRDTO.FileChangeDTO> files) {
        Map<String, Integer> scopeCounts = new HashMap<>();
        Map<String, Integer> scopeAdditions = new HashMap<>();
        int sensitiveFiles = 0;
        int totalChangedLines = 0;

        for (GitHubPRDTO.FileChangeDTO file : files) {
            String scope = file.getScopeCategory();
            scopeCounts.merge(scope, 1, Integer::sum);
            scopeAdditions.merge(scope, file.getAdditions(), Integer::sum);
            if (file.getIsSensitive()) sensitiveFiles++;
            totalChangedLines += safeInt(file.getAdditions()) + safeInt(file.getDeletions());
        }

        StringBuilder summary = new StringBuilder();
        summary.append("## Diff Summary\n\n");
        summary.append("**Total Files Changed:** ").append(files.size()).append("\n");
        summary.append("**Total Additions:** ").append(files.stream().mapToInt(GitHubPRDTO.FileChangeDTO::getAdditions).sum()).append("\n");
        summary.append("**Total Deletions:** ").append(files.stream().mapToInt(GitHubPRDTO.FileChangeDTO::getDeletions).sum()).append("\n\n");

        if (totalChangedLines > LARGE_PR_LINE_THRESHOLD) {
            summary.append("Large PR - manual review recommended. File list only; raw patches hidden.\n\n");
        } else if (totalChangedLines > 50) {
            summary.append("Medium PR - showing file list and truncated per-file patches where available.\n\n");
        }

        summary.append("### Changes by Scope\n");
        scopeCounts.forEach((scope, count) -> {
            int additions = scopeAdditions.getOrDefault(scope, 0);
            summary.append("- **").append(scope).append("**: ")
                   .append(count).append(" files (+").append(additions).append(")\n");
        });

        if (sensitiveFiles > 0) {
            summary.append("\n⚠️ **WARNING:** ").append(sensitiveFiles)
                   .append(" sensitive files detected\n");
        }

        return summary.toString();
    }

    /**
     * Check if file is sensitive (credentials, secrets, env files)
     */
    private boolean isSensitiveFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.contains(".env") || lower.contains("secret") || 
               lower.contains("credential") || lower.contains("password") ||
               lower.contains(".pem") || lower.contains("key") || 
               lower.contains("token") || lower.contains("api_key");
    }

    /**
     * Make authenticated GitHub API request
     */
    private String makeGitHubRequest(String url, IntegrationCredentials credentials) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + credentials.getGithubToken());
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("GitHub API error: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private GitHubPRRef parsePRReference(String prUrl) {
        String trimmed = prUrl == null ? "" : prUrl.trim();
        Matcher urlMatcher = GITHUB_URL_PATTERN.matcher(trimmed);
        if (urlMatcher.find()) {
            return new GitHubPRRef(urlMatcher.group(1), urlMatcher.group(2), urlMatcher.group(3));
        }

        Matcher shortMatcher = GITHUB_SHORT_PATTERN.matcher(trimmed);
        if (shortMatcher.matches()) {
            return new GitHubPRRef(shortMatcher.group(1), shortMatcher.group(2), shortMatcher.group(3));
        }

        throw new IllegalArgumentException("Invalid GitHub PR URL format");
    }

    private String resolveMergeStatus(JsonNode root) {
        if (root.path("merged").asBoolean(false)) {
            return "merged";
        }
        if (root.path("mergeable").isBoolean()) {
            return root.path("mergeable").asBoolean() ? "mergeable" : "conflict";
        }
        return "unknown";
    }

    private LocalDateTime parseGitHubTimestamp(String timestamp) {
        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record GitHubPRRef(String owner, String repo, String prNumber) {}
}
