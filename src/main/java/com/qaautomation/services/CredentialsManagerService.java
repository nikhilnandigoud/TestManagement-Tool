package com.qaautomation.services;

import com.qaautomation.models.IntegrationCredentials;
import com.qaautomation.repositories.IntegrationCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing integration credentials
 * Handles encryption/decryption of tokens
 */
@Slf4j
@Service
public class CredentialsManagerService {

    @Value("${encryption.secret-key:default-secret-key-change-in-production}")
    private String secretKey;

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private IntegrationCredentialsRepository credentialsRepository;

    /**
     * Save integration credentials
     * Tokens are encrypted before storage
     */
    @Transactional
    public void saveCredentials(String userId, IntegrationCredentials credentials, boolean isTeamLevel) {
        try {
            String key = buildStorageKey(userId, isTeamLevel);
            Optional<IntegrationCredentials> existing = credentialsRepository.findById(key);
            IntegrationCredentials stored = existing.orElseGet(IntegrationCredentials::new);

            stored.setId(key);
            stored.setJiraWorkspaceUrl(credentials.getJiraWorkspaceUrl());
            stored.setJiraAuthMethod(credentials.getJiraAuthMethod());
            stored.setJiraToken(resolveTokenForSave(credentials.getJiraToken(), existing.map(IntegrationCredentials::getJiraToken).orElse(null)));
            stored.setJiraTokenExpiry(credentials.getJiraTokenExpiry());
            stored.setGithubToken(resolveTokenForSave(credentials.getGithubToken(), existing.map(IntegrationCredentials::getGithubToken).orElse(null)));
            stored.setGithubTokenExpiry(credentials.getGithubTokenExpiry());
            stored.setGitlabToken(resolveTokenForSave(credentials.getGitlabToken(), existing.map(IntegrationCredentials::getGitlabToken).orElse(null)));
            stored.setGitlabTokenExpiry(credentials.getGitlabTokenExpiry());
            stored.setIsTeamLevel(isTeamLevel);
            stored.setCreatedAt(existing.map(IntegrationCredentials::getCreatedAt).orElse(System.currentTimeMillis()));
            stored.setUpdatedAt(System.currentTimeMillis());
            stored.setUserId(userId);

            credentialsRepository.save(stored);
            credentials.setUserId(userId);
            credentials.setIsTeamLevel(isTeamLevel);

            log.info("Credentials saved for {} (team-level: {})", userId, isTeamLevel);
        } catch (Exception e) {
            log.error("Failed to save credentials: {}", e.getMessage());
            throw new RuntimeException("Failed to save credentials", e);
        }
    }

    /**
     * Retrieve integration credentials
     * Tokens are decrypted after retrieval
     */
    public IntegrationCredentials getCredentials(String userId, boolean allowTeamLevel) {
        try {
            IntegrationCredentials creds = credentialsRepository.findById(userId)
                .orElseGet(() -> allowTeamLevel ? credentialsRepository.findById("TEAM").orElse(null) : null);

            if (creds == null) {
                return null;
            }

            // Decrypt tokens
            IntegrationCredentials decrypted = new IntegrationCredentials();
            decrypted.setId(creds.getId());
            decrypted.setJiraWorkspaceUrl(creds.getJiraWorkspaceUrl());
            decrypted.setJiraAuthMethod(creds.getJiraAuthMethod());
            decrypted.setJiraToken(creds.getJiraToken() != null ? decrypt(creds.getJiraToken()) : null);
            decrypted.setJiraTokenExpiry(creds.getJiraTokenExpiry());
            decrypted.setGithubToken(creds.getGithubToken() != null ? decrypt(creds.getGithubToken()) : null);
            decrypted.setGithubTokenExpiry(creds.getGithubTokenExpiry());
            decrypted.setGitlabToken(creds.getGitlabToken() != null ? decrypt(creds.getGitlabToken()) : null);
            decrypted.setGitlabTokenExpiry(creds.getGitlabTokenExpiry());
            decrypted.setUserId(creds.getUserId());
            decrypted.setCreatedAt(creds.getCreatedAt());
            decrypted.setUpdatedAt(creds.getUpdatedAt());
            decrypted.setIsTeamLevel(creds.getIsTeamLevel());

            return decrypted;
        } catch (Exception e) {
            log.error("Failed to retrieve credentials: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Delete credentials
     */
    @Transactional
    public void deleteCredentials(String userId) {
        credentialsRepository.deleteById(userId);
        log.info("Credentials deleted for {}", userId);
    }

    /**
     * Test connection to Jira/GitHub
     */
    public Map<String, Boolean> testConnections(IntegrationCredentials credentials) {
        Map<String, Boolean> results = new HashMap<>();

        // Test Jira
        try {
            // Decrypted token available in credentials object
            results.put("jira", testJiraConnection(credentials));
        } catch (Exception e) {
            log.warn("Jira connection test failed: {}", e.getMessage());
            results.put("jira", false);
        }

        // Test GitHub
        try {
            results.put("github", testGitHubConnection(credentials));
        } catch (Exception e) {
            log.warn("GitHub connection test failed: {}", e.getMessage());
            results.put("github", false);
        }

        return results;
    }

    /**
     * Test Jira connection
     */
    private boolean testJiraConnection(IntegrationCredentials credentials) throws Exception {
        // This would be called from JiraFetchService
        // For now, just check if credentials exist
        return credentials.getJiraWorkspaceUrl() != null && 
               !credentials.getJiraWorkspaceUrl().isEmpty() &&
               credentials.getJiraToken() != null && 
               !credentials.getJiraToken().isEmpty();
    }

    /**
     * Test GitHub connection
     */
    private boolean testGitHubConnection(IntegrationCredentials credentials) throws Exception {
        // This would be called from GitHubFetchService
        return credentials.getGithubToken() != null && 
               !credentials.getGithubToken().isEmpty();
    }

    private String resolveTokenForSave(String candidate, String existingEncrypted) throws Exception {
        if (candidate == null || candidate.isBlank() || "***".equals(candidate)) {
            return existingEncrypted;
        }
        return encrypt(candidate);
    }

    /**
     * Encrypt sensitive data using AES
     */
    private String encrypt(String data) throws Exception {
        if (data == null || data.isEmpty()) {
            return data;
        }

        // In production, use proper key management
        byte[] key = secretKey.getBytes();
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            // Pad or truncate to valid key size (16 bytes for AES-128)
            byte[] newKey = new byte[16];
            System.arraycopy(key, 0, newKey, 0, Math.min(key.length, 16));
            key = newKey;
        }

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        SecretKey secretKeySpec = new SecretKeySpec(key, 0, key.length, ALGORITHM);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return "v1:" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypt sensitive data using AES
     */
    private String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        byte[] key = secretKey.getBytes();
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            byte[] newKey = new byte[16];
            System.arraycopy(key, 0, newKey, 0, Math.min(key.length, 16));
            key = newKey;
        }

        SecretKey secretKeySpec = new SecretKeySpec(key, 0, key.length, ALGORITHM);

        if (encryptedData.startsWith("v1:")) {
            String[] parts = encryptedData.split(":", 3);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] decodedData = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(decodedData));
        }

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decodedData));
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String expiryDate) {
        if (expiryDate == null || expiryDate.isEmpty()) {
            return false; // No expiry set
        }

        try {
            long expiryTime = Long.parseLong(expiryDate);
            return System.currentTimeMillis() > expiryTime;
        } catch (NumberFormatException e) {
            log.warn("Invalid expiry date format: {}", expiryDate);
            return false;
        }
    }

    /**
     * Revoke a token
     */
    @Transactional
    public void revokeToken(String userId, String tokenType) {
        IntegrationCredentials creds = credentialsRepository.findById(userId).orElse(null);
        if (creds != null) {
            switch (tokenType.toLowerCase()) {
                case "jira" -> creds.setJiraToken(null);
                case "github" -> creds.setGithubToken(null);
                case "gitlab" -> creds.setGitlabToken(null);
            }
            creds.setUpdatedAt(System.currentTimeMillis());
            credentialsRepository.save(creds);
            log.info("Token revoked for user {} - type: {}", userId, tokenType);
        }
    }

    private String buildStorageKey(String userId, boolean isTeamLevel) {
        return isTeamLevel ? "TEAM" : userId;
    }
}
