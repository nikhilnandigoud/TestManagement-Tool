package com.qaautomation.services;

import com.qaautomation.models.*;
import com.qaautomation.repositories.*;
import com.qaautomation.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Priority Context Assembly Service
 * 
 * Assembles context hierarchy for LLM prompts:
 * PRIORITY 1: Current Scenario (highest)
 * PRIORITY 2: User Guides & Rules
 * PRIORITY 3: Uploaded Files Content
 * PRIORITY 4: Historical Context (future pgvector)
 */
@Slf4j
@Service
@Transactional
public class ContextPriorityAssemblyService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private AttachmentRepository attachmentRepository;
    
    /**
     * Load guide content
     * In production, this would load from database or file
     */
    private static final Map<String, GuideContent> GUIDES_DATABASE = new HashMap<String, GuideContent>() {{
        put("bestPractices", new GuideContent(
            "Best Practices",
            "1. Always validate all input fields before processing\n" +
            "2. Test edge cases: empty strings, null values, special characters\n" +
            "3. Verify error handling for network failures\n" +
            "4. Check boundary conditions (min/max values)\n" +
            "5. Test with various data types and encodings\n" +
            "6. Ensure proper logging and error messages\n" +
            "7. Performance testing for large datasets\n" +
            "8. Security testing for SQL injection, XSS attacks\n" +
            "9. Cross-browser and cross-device compatibility\n" +
            "10. Accessibility compliance (WCAG 2.1)"
        ));
        
        put("dan", new GuideContent(
            "DAN (Data Access Notification)",
            "DAN Guidelines:\n" +
            "• All database queries must have proper indexing\n" +
            "• Use parameterized queries to prevent SQL injection\n" +
            "• Implement connection pooling for performance\n" +
            "• Cache frequently accessed data\n" +
            "• Monitor query execution time\n" +
            "• Backup critical data regularly\n" +
            "• Never hardcode credentials\n" +
            "• Use encryption for sensitive data\n" +
            "• Implement audit logging for data access\n" +
            "• Test disaster recovery procedures"
        ));
        
        put("dmdedt", new GuideContent(
            "DMDEDT (Database & Maintenance Design)",
            "Database & Maintenance Design Requirements:\n" +
            "• Design schemas following normalization rules\n" +
            "• Implement proper foreign key constraints\n" +
            "• Create comprehensive backup strategy\n" +
            "• Plan for scalability and sharding\n" +
            "• Monitor database health metrics\n" +
            "• Document all schema changes\n" +
            "• Implement version control for DDL scripts\n" +
            "• Test migration procedures\n" +
            "• Plan for zero-downtime deployments\n" +
            "• Regular performance tuning"
        ));
    }};
    
    /**
     * Assemble complete priority context for a conversation
     * 
     * @param conversationId The conversation ID
     * @param selectedGuides List of selected guide IDs (bestPractices, dan, dmdedt)
     * @param message Current user message for context
     * @return Complete ContextPriorityDTO
     */
    public ContextPriorityDTO assembleContext(
            UUID conversationId,
            List<String> selectedGuides,
            String message) {
        
        try {
            log.info("Assembling priority context for conversation: {}", conversationId);
            
            ContextPriorityDTO context = ContextPriorityDTO.builder().build();
            
            // PRIORITY 1: Load Current Scenario
            try {
                Conversation conversation = conversationRepository.findById(conversationId)
                    .orElse(null);
                
                if (conversation != null && conversation.getScenarioText() != null) {
                    context.setPriority1Scenario(conversation.getScenarioText());
                    context.setHasScenario(true);
                    log.debug("P1 Scenario loaded: {} chars", conversation.getScenarioText().length());
                }
            } catch (Exception e) {
                log.warn("Failed to load scenario", e);
            }
            
            // PRIORITY 2: Load Selected Guides
            try {
                if (selectedGuides != null && !selectedGuides.isEmpty()) {
                    List<ContextPriorityDTO.GuideContextDTO> guideContexts = new ArrayList<>();
                    
                    for (String guideId : selectedGuides) {
                        GuideContent guide = GUIDES_DATABASE.get(guideId);
                        if (guide != null) {
                            guideContexts.add(ContextPriorityDTO.GuideContextDTO.builder()
                                .guideId(guideId)
                                .guideName(guide.name)
                                .content(guide.content)
                                .build());
                            log.debug("P2 Guide loaded: {}", guideId);
                        }
                    }
                    
                    if (!guideContexts.isEmpty()) {
                        context.setPriority2Guides(guideContexts);
                        context.setHasGuides(true);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load guides", e);
            }
            
            // PRIORITY 3: Load Uploaded Files Content
            try {
                List<Attachment> attachments = new ArrayList<>();
                
                // Get all messages for this conversation
                Conversation conversation = conversationRepository.findById(conversationId)
                    .orElse(null);
                
                if (conversation != null && conversation.getMessages() != null) {
                    for (ChatMessage msg : conversation.getMessages()) {
                        if (msg.getAttachments() != null) {
                            attachments.addAll(msg.getAttachments());
                        }
                    }
                }
                
                if (!attachments.isEmpty()) {
                    List<ContextPriorityDTO.FileContextDTO> fileContexts = attachments.stream()
                        .map(att -> ContextPriorityDTO.FileContextDTO.builder()
                            .fileId(att.getId().toString())
                            .fileName(att.getOriginalFileName())
                            .fileType(att.getFileType())
                            .category(att.getCategory())
                            .extractedContent(att.getExtractedText())
                            .fileSize(att.getFileSize())
                            .build())
                        .collect(Collectors.toList());
                    
                    context.setPriority3Files(fileContexts);
                    context.setHasFiles(true);
                    log.debug("P3 Files loaded: {}", fileContexts.size());
                }
            } catch (Exception e) {
                log.warn("Failed to load files", e);
            }
            
            // PRIORITY 4: Historical Context (placeholder for pgvector)
            // This will be populated from pgvector in Phase 3
            // For now, it's empty but the structure is ready
            try {
                // TODO: After pgvector implementation:
                // 1. Generate embedding for the message
                // 2. Query pgvector for similar cases
                // 3. Populate priority4Historical with results
                
                log.debug("P4 Historical context: Not yet implemented (awaiting pgvector)");
            } catch (Exception e) {
                log.warn("Failed to load historical context", e);
            }
            
            log.info("Priority context assembled: {}", context.getContextSummary());
            return context;
            
        } catch (Exception e) {
            log.error("Error assembling priority context", e);
            return ContextPriorityDTO.builder().build();
        }
    }
    
    /**
     * Get system prompt from context
     */
    public String getSystemPrompt(ContextPriorityDTO context) {
        return context.assembleSystemPrompt();
    }
    
    /**
     * Get context summary for logging/debugging
     */
    public String getContextSummary(ContextPriorityDTO context) {
        return context.getContextSummary();
    }
    
    /**
     * Helper class to store guide information
     */
    private static class GuideContent {
        String name;
        String content;
        
        GuideContent(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }
}
