package com.qaautomation.controller;

import com.qaautomation.dto.HealthCheckResponseDTO;
import com.qaautomation.services.AzureOpenAIEmbeddingService;
import com.qaautomation.services.AzureOpenAILLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller for Phase 3 and Phase 4
 * Verifies:
 * - PostgreSQL Connection
 * - pgvector Extension
 * - Azure OpenAI Configuration
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthCheckController {
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    @Autowired(required = false)
    private AzureOpenAIEmbeddingService azureEmbeddingService;
    
    @Autowired(required = false)
    private AzureOpenAILLMService azureLLMService;
    
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;
    
    @Value("${azure.openai.endpoint:}")
    private String azureOpenAIEndpoint;
    
    @GetMapping("/check")
    public ResponseEntity<HealthCheckResponseDTO> healthCheck() {
        Map<String, Object> checks = new HashMap<>();
        
        // Check PostgreSQL
        checks.put("PostgreSQL", checkPostgreSQL());
        
        // Check pgvector
        checks.put("pgvector", checkPgvector());
        
        // Check Azure OpenAI
        checks.put("AzureOpenAI", checkAzureOpenAI());
        
        boolean allHealthy = checks.values().stream()
            .allMatch(check -> (Boolean) ((Map<String, Object>) check).get("status"));
        
        HealthCheckResponseDTO response = HealthCheckResponseDTO.builder()
            .status(allHealthy ? "HEALTHY" : "DEGRADED")
            .timestamp(System.currentTimeMillis())
            .checks(checks)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check PostgreSQL connection
     */
    private Map<String, Object> checkPostgreSQL() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (dataSource == null) {
                result.put("status", false);
                result.put("message", "DataSource not configured");
                return result;
            }
            
            try (Connection conn = dataSource.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    result.put("status", true);
                    result.put("message", "Connected successfully");
                    result.put("url", datasourceUrl);
                    log.info("PostgreSQL health check: OK");
                } else {
                    result.put("status", false);
                    result.put("message", "Connection is closed");
                }
            }
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", e.getMessage());
            log.error("PostgreSQL health check failed", e);
        }
        return result;
    }
    
    /**
     * Check pgvector extension
     */
    private Map<String, Object> checkPgvector() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (dataSource == null) {
                result.put("status", false);
                result.put("message", "DataSource not configured");
                return result;
            }
            
            try (Connection conn = dataSource.getConnection()) {
                var stmt = conn.createStatement();
                var rs = stmt.executeQuery("SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')");
                if (rs.next() && rs.getBoolean(1)) {
                    result.put("status", true);
                    result.put("message", "pgvector extension is installed");
                    log.info("pgvector health check: OK");
                } else {
                    result.put("status", false);
                    result.put("message", "pgvector extension not found");
                    log.warn("pgvector extension not installed");
                }
            }
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", e.getMessage());
            log.error("pgvector health check failed", e);
        }
        return result;
    }
    
    /**
     * Check Azure OpenAI configuration
     */
    private Map<String, Object> checkAzureOpenAI() {
        Map<String, Object> result = new HashMap<>();
        
        if (azureEmbeddingService == null || azureLLMService == null) {
            result.put("status", false);
            result.put("message", "Azure OpenAI services not configured");
            result.put("endpoint", azureOpenAIEndpoint);
            log.warn("Azure OpenAI not configured - will use mock embeddings");
            return result;
        }
        
        try {
            boolean embeddingHealthy = azureEmbeddingService.healthCheck();
            boolean llmHealthy = azureLLMService.healthCheck();
            
            if (embeddingHealthy && llmHealthy) {
                result.put("status", true);
                result.put("message", "Azure OpenAI is healthy");
                result.put("endpoint", azureOpenAIEndpoint);
                log.info("Azure OpenAI health check: OK");
            } else {
                result.put("status", false);
                result.put("message", "Azure OpenAI services not responding");
                result.put("embeddingHealthy", embeddingHealthy);
                result.put("llmHealthy", llmHealthy);
            }
        } catch (Exception e) {
            result.put("status", false);
            result.put("message", e.getMessage());
            log.error("Azure OpenAI health check failed", e);
        }
        
        return result;
    }
}
