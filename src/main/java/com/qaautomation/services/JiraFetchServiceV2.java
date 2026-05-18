package com.qaautomation.services;

import com.qaautomation.models.JiraTicketDTO;
import com.qaautomation.models.IntegrationCredentials;
import com.qaautomation.cache.IntegrationCacheService;
import com.qaautomation.rate_limiting.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for fetching Jira tickets with caching, rate limiting, and audit logging
 * Supports Jira Cloud (OAuth) and Jira Server/Data Center (PAT)
 */
@Slf4j
@Service
public class JiraFetchServiceV2 {

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

    @Value("${jira.api.version:3}")
    private String jiraApiVersion;

    private static final Pattern JIRA_ID_PATTERN = Pattern.compile("^([A-Z][A-Z0-9]+-\\d+)$");
    private static final Pattern JIRA_URL_PATTERN = Pattern.compile(".*/browse/([A-Z][A-Z0-9]+-\\d+).*");

    /**
     * Fetch a Jira ticket by ID with caching and rate limiting
     * @param ticketId Ticket ID (e.g., PROJ-123)
     * @param credentials Integration credentials with Jira token
     * @param userId User ID for audit logging
     * @return JiraTicketDTO with all relevant fields
     */
    public JiraTicketDTO fetchTicket(String ticketId, IntegrationCredentials credentials, String userId, String ipAddress) throws Exception {
        long startTime = System.currentTimeMillis();
        String action = "JIRA_FETCH";
        boolean success = false;
        String errorMessage = null;

        try {
            if (credentials == null || credentials.getJiraToken() == null) {
                throw new IllegalArgumentException("Jira credentials not configured");
            }

            String normalizedTicketId = normalizeTicketId(ticketId);
            if (!isValidJiraId(normalizedTicketId)) {
                throw new IllegalArgumentException("Invalid Jira ticket ID format: " + ticketId);
            }

            String workspaceId = credentials.getJiraWorkspaceUrl();

            // Check cache first
            Optional<JiraTicketDTO> cached = cacheService.getJiraTicket(workspaceId, normalizedTicketId);
            if (cached.isPresent()) {
                log.info("Returning cached Jira ticket: {}", normalizedTicketId);
                auditService.logFetch(userId, action, normalizedTicketId, "JIRA", true, null,
                    System.currentTimeMillis() - startTime, ipAddress);
                return cached.get();
            }

            // Check rate limit
            if (!rateLimitingService.allowJiraRequest(workspaceId)) {
                long backoffMs = rateLimitingService.getBackoffMs("jira:" + workspaceId);
                errorMessage = "Rate limit exceeded. Retry after " + backoffMs + "ms";
                log.warn(errorMessage);
                auditService.logFetch(userId, action, ticketId, "JIRA", false, errorMessage,
                    System.currentTimeMillis() - startTime, ipAddress);
                throw new RuntimeException(errorMessage);
            }

            // Fetch from API
            String ticketUrl = buildJiraUrl(credentials.getJiraWorkspaceUrl(), normalizedTicketId);
            HttpHeaders headers = createAuthHeaders(credentials.getJiraToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                ticketUrl, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                errorMessage = "Failed to fetch Jira ticket: " + response.getStatusCode();
                throw new RuntimeException(errorMessage);
            }

            // Parse response
            JiraTicketDTO ticket = parseJiraResponse(response.getBody());
            ticket.setSourceUrl(buildJiraBrowseUrl(credentials.getJiraWorkspaceUrl(), normalizedTicketId));
            ticket.setFetchedAt(LocalDateTime.now());

            // Cache the result
            cacheService.cacheJiraTicket(workspaceId, normalizedTicketId, ticket);

            // Reset backoff on success
            rateLimitingService.resetBackoff("jira:" + workspaceId);
            success = true;

            log.info("Successfully fetched Jira ticket: {}", normalizedTicketId);
            return ticket;

        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Failed to fetch Jira ticket {}: {}", ticketId, e.getMessage(), e);
            throw e;
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            auditService.logFetch(userId, action, ticketId, "JIRA", success, errorMessage, responseTime, ipAddress);
        }
    }

    /**
     * Parse Jira API response JSON
     */
    private JiraTicketDTO parseJiraResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JiraTicketDTO ticket = new JiraTicketDTO();

        // Extract basic fields
        ticket.setId(root.path("id").asText());
        ticket.setKey(root.path("key").asText());
        ticket.setProjectKey(root.path("fields").path("project").path("key").asText());
        ticket.setTitle(root.path("fields").path("summary").asText());
        
        // Convert ADF description to plain text
        JsonNode description = root.path("fields").path("description");
        ticket.setDescription(convertADFToPlainText(description));

        // Extract acceptance criteria
        ticket.setAcceptanceCriteria(extractAcceptanceCriteria(root));

        // Extract status
        ticket.setStatus(root.path("fields").path("status").path("name").asText());

        // Extract assignee
        String assignee = root.path("fields").path("assignee").path("displayName").asText();
        ticket.setAssignee(assignee.isEmpty() ? "Unassigned" : assignee);

        String reporter = root.path("fields").path("reporter").path("displayName").asText();
        ticket.setReporter(reporter.isEmpty() ? "Unknown" : reporter);

        ticket.setIssueType(root.path("fields").path("issuetype").path("name").asText());

        // Map priority
        String jiraPriority = root.path("fields").path("priority").path("name").asText();
        ticket.setPriority(mapJiraPriority(jiraPriority));

        // Extract sprint
        String sprint = extractSprint(root);
        ticket.setSprint(sprint);

        // Extract story points
        int storyPoints = root.path("fields").path("customfield_10021").asInt(0);
        ticket.setStoryPoints(storyPoints);

        ticket.setCreated(parseJiraTimestamp(root.path("fields").path("created").asText()));
        ticket.setUpdated(parseJiraTimestamp(root.path("fields").path("updated").asText()));

        // Extract labels
        List<String> labels = new ArrayList<>();
        root.path("fields").path("labels").forEach(node -> labels.add(node.asText()));
        ticket.setLabels(labels);

        // Extract linked issues
        List<String> linkedIssues = new ArrayList<>();
        root.path("fields").path("issuelinks").forEach(link -> {
            String linkedId = link.path("outwardIssue").path("key").asText();
            if (linkedId.isEmpty()) {
                linkedId = link.path("inwardIssue").path("key").asText();
            }
            if (!linkedId.isEmpty()) {
                linkedIssues.add(linkedId);
            }
        });
        ticket.setLinkedIssues(linkedIssues);

        // Extract attachments
        List<String> attachments = new ArrayList<>();
        root.path("fields").path("attachment").forEach(attachment -> {
            attachments.add(attachment.path("filename").asText());
        });
        ticket.setAttachments(attachments);

        return ticket;
    }

    /**
     * Convert Atlassian Document Format to plain text
     */
    private String convertADFToPlainText(JsonNode adfNode) {
        if (adfNode == null || adfNode.isMissingNode() || adfNode.isNull()) {
            return "";
        }

        try {
            if (adfNode.isTextual()) {
                String text = adfNode.asText();
                if (text.isBlank()) {
                    return "";
                }
                try {
                    return convertADFToPlainText(objectMapper.readTree(text));
                } catch (Exception ignored) {
                    return text;
                }
            }

            JsonNode root = adfNode;
            StringBuilder text = new StringBuilder();

            if (root.isObject() && root.has("content")) {
                root.path("content").forEach(node -> {
                    String nodeText = extractTextFromNode(node).trim();
                    if (!nodeText.isEmpty()) {
                        text.append(nodeText).append("\n");
                    }
                });
            }

            return text.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to parse ADF: {}", e.getMessage());
            return adfNode.asText("");
        }
    }

    /**
     * Recursively extract text from ADF node
     */
    private String extractTextFromNode(JsonNode node) {
        StringBuilder text = new StringBuilder();

        if (node.has("text")) {
            text.append(node.path("text").asText());
        }

        if (node.has("content")) {
            node.path("content").forEach(child -> {
                String childText = extractTextFromNode(child);
                if (!childText.isBlank()) {
                    if (!text.isEmpty() && needsSpacing(node.path("type").asText())) {
                        text.append(" ");
                    }
                    text.append(childText);
                }
            });
        }

        String type = node.path("type").asText();
        if ("hardBreak".equals(type)) {
            text.append("\n");
        } else if ("listItem".equals(type)) {
            text.insert(0, "- ");
        }

        return text.toString();
    }

    /**
     * Extract acceptance criteria from custom field or description
     */
    private String extractAcceptanceCriteria(JsonNode root) {
        // First check custom field
        JsonNode customACNode = root.path("fields").path("customfield_10050");
        String customAC = customACNode.isMissingNode() ? "" : convertADFToPlainText(customACNode);
        if (!customAC.isBlank()) {
            return customAC.trim();
        }

        // Fall back to parsing description
        try {
            String plainText = convertADFToPlainText(root.path("fields").path("description"));
            
            String[] lines = plainText.split("\n");
            StringBuilder ac = new StringBuilder();
            boolean inAC = false;

            for (String line : lines) {
                String trimmed = line.trim();
                String lower = trimmed.toLowerCase();
                if (lower.contains("acceptance criteria") || lower.equals("ac")) {
                    inAC = true;
                    continue;
                }
                if (inAC && !trimmed.isBlank()) {
                    ac.append(trimmed).append("\n");
                }
            }

            return ac.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extract sprint from issue fields
     */
    private String extractSprint(JsonNode root) {
        JsonNode sprintNode = root.path("fields").path("customfield_10020");
        if (sprintNode.isArray() && sprintNode.size() > 0) {
            String sprintText = sprintNode.get(0).asText();
            // Parse sprint name from sprint JSON string
            if (sprintText.contains("name=")) {
                int start = sprintText.indexOf("name=") + 5;
                int end = sprintText.indexOf(",", start);
                if (end == -1) end = sprintText.indexOf("}", start);
                return sprintText.substring(start, end);
            }
        }
        return "";
    }

    /**
     * Map Jira priority to P1-P4
     */
    private String mapJiraPriority(String jiraPriority) {
        if (jiraPriority == null) return "P3";

        return switch (jiraPriority.toLowerCase()) {
            case "highest" -> "P1";
            case "high" -> "P2";
            case "medium" -> "P3";
            case "low" -> "P4";
            default -> "P3";
        };
    }

    /**
     * Validate Jira ticket ID format
     */
    private boolean isValidJiraId(String ticketId) {
        return JIRA_ID_PATTERN.matcher(ticketId).matches();
    }

    private String normalizeTicketId(String ticketId) {
        if (ticketId == null) {
            return "";
        }
        String trimmed = ticketId.trim();
        java.util.regex.Matcher urlMatcher = JIRA_URL_PATTERN.matcher(trimmed);
        if (urlMatcher.matches()) {
            return urlMatcher.group(1).toUpperCase();
        }
        return trimmed.toUpperCase();
    }

    /**
     * Build Jira API URL
     */
    private String buildJiraUrl(String workspaceUrl, String ticketId) {
        String baseUrl = workspaceUrl.endsWith("/") ? workspaceUrl : workspaceUrl + "/";
        return baseUrl + "rest/api/" + jiraApiVersion + "/issue/" + ticketId;
    }

    private String buildJiraBrowseUrl(String workspaceUrl, String ticketId) {
        String baseUrl = workspaceUrl.endsWith("/") ? workspaceUrl.substring(0, workspaceUrl.length() - 1) : workspaceUrl;
        return baseUrl + "/browse/" + ticketId;
    }

    /**
     * Create HTTP headers with authentication
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        return headers;
    }

    private LocalDateTime parseJiraTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean needsSpacing(String nodeType) {
        return !"bulletList".equals(nodeType) && !"orderedList".equals(nodeType);
    }
}
