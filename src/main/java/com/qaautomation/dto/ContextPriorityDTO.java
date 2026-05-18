package com.qaautomation.dto;

import lombok.*;
import java.util.List;

/**
 * DTO to represent the Priority Context Engine
 * PRIORITY 1: Current Scenario
 * PRIORITY 2: User Guides & Rules  
 * PRIORITY 3: Uploaded Files & Content
 * PRIORITY 4: Historical Context (future pgvector)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextPriorityDTO {
    
    /**
     * PRIORITY 1: Current Scenario (highest priority)
     * The active requirement/scenario user is working on
     */
    @Builder.Default
    private String priority1Scenario = "";
    
    /**
     * PRIORITY 2: User Guides & Rules
     * Selected best practices and guidelines
     */
    @Builder.Default
    private List<GuideContextDTO> priority2Guides = List.of();
    
    /**
     * PRIORITY 3: Uploaded Files & Extracted Content
     * Content from PDF, DOCX, images, etc.
     */
    @Builder.Default
    private List<FileContextDTO> priority3Files = List.of();
    
    /**
     * PRIORITY 4: Historical Context
     * Similar past cases from vector search (Future: pgvector)
     */
    @Builder.Default
    private List<HistoricalContextDTO> priority4Historical = List.of();
    
    /**
     * Indicator flags
     */
    @Builder.Default
    private boolean hasScenario = false;
    
    @Builder.Default
    private boolean hasGuides = false;
    
    @Builder.Default
    private boolean hasFiles = false;
    
    @Builder.Default
    private boolean hasHistorical = false;
    
    /**
     * Assemble priorities into a system prompt for LLM
     */
    public String assembleSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert QA & Testing AI Agent.\n");
        prompt.append("Follow this context hierarchy strictly:\n\n");
        
        // P1: Scenario
        if (hasScenario && !priority1Scenario.isEmpty()) {
            prompt.append("═══ PRIORITY 1 (CURRENT SCENARIO - Highest) ═══\n");
            prompt.append(priority1Scenario).append("\n\n");
        }
        
        // P2: Guides
        if (hasGuides && !priority2Guides.isEmpty()) {
            prompt.append("═══ PRIORITY 2 (GUIDES & RULES) ═══\n");
            for (GuideContextDTO guide : priority2Guides) {
                prompt.append("📋 ").append(guide.getGuideName()).append(":\n");
                prompt.append(guide.getContent()).append("\n\n");
            }
        }
        
        // P3: Files
        if (hasFiles && !priority3Files.isEmpty()) {
            prompt.append("═══ PRIORITY 3 (UPLOADED FILES & CONTENT) ═══\n");
            for (FileContextDTO file : priority3Files) {
                prompt.append("📎 File: ").append(file.getFileName())
                    .append(" (").append(file.getFileType()).append(")\n");
                if (file.getExtractedContent() != null && !file.getExtractedContent().isEmpty()) {
                    prompt.append("Content:\n").append(file.getExtractedContent()).append("\n\n");
                }
            }
        }
        
        // P4: Historical
        if (hasHistorical && !priority4Historical.isEmpty()) {
            prompt.append("═══ PRIORITY 4 (HISTORICAL CONTEXT from Vector Search) ═══\n");
            prompt.append("Note: These are similar cases found in our knowledge base.\n");
            for (HistoricalContextDTO historical : priority4Historical) {
                prompt.append("• Similar Case #").append(historical.getCaseId())
                    .append(" (Match: ").append(historical.getSimilarityScore()).append("%)\n")
                    .append(historical.getContext()).append("\n\n");
            }
        }
        
        return prompt.toString();
    }
    
    /**
     * Get summary of what context is being used
     */
    public String getContextSummary() {
        StringBuilder summary = new StringBuilder("Context Used: ");
        List<String> used = new java.util.ArrayList<>();
        
        if (hasScenario) used.add("Scenario");
        if (hasGuides) used.add(priority2Guides.size() + " Guide(s)");
        if (hasFiles) used.add(priority3Files.size() + " File(s)");
        if (hasHistorical) used.add(priority4Historical.size() + " Historical Case(s)");
        
        summary.append(String.join(" + ", used));
        return summary.toString();
    }
    
    /**
     * Nested DTOs
     */
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuideContextDTO {
        private String guideId; // bestPractices, dan, dmdedt
        private String guideName; // Display name
        private String content; // Full guide content
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileContextDTO {
        private String fileId; // Attachment UUID
        private String fileName; // Original filename
        private String fileType; // pdf, docx, jpg, etc.
        private String category; // DOCUMENT, IMAGE, SPREADSHEET
        private String extractedContent; // Extracted/OCR text
        private Long fileSize;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoricalContextDTO {
        private String caseId; // ID from vector DB
        private String context; // Case content
        private Double similarityScore; // 0-100 match percentage
        private String source; // Where from
    }
}
