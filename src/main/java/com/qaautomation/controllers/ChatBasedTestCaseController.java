package com.qaautomation.controllers;

import com.qaautomation.services.ChatBasedTestCaseService;
import com.qaautomation.services.VectorSearchService;
import com.qaautomation.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatBasedTestCaseController {
    
    @Autowired
    private ChatBasedTestCaseService chatService;
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    /**
     * Start a new conversation
     * POST /api/chat/start
     */
    @PostMapping("/start")
    public ResponseEntity<ConversationResponseDTO> startConversation(
        @RequestBody ConversationStartDTO request
    ) {
        try {
            log.info("Starting new conversation");
            ConversationResponseDTO response = chatService.startConversation(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Send message to conversation
     * POST /api/chat/message
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponseDTO> sendMessage(
        @RequestBody ChatRequestDTO request
    ) {
        try {
            log.info("Sending message to conversation: {}", request.getConversationId());
            ChatResponseDTO response = chatService.sendMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get conversation details
     * GET /api/chat/{conversationId}
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponseDTO> getConversation(
        @PathVariable String conversationId
    ) {
        try {
            log.info("Fetching conversation: {}", conversationId);
            ConversationResponseDTO response = chatService.getConversation(UUID.fromString(conversationId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Regenerate test cases
     * POST /api/chat/{conversationId}/regenerate
     */
    @PostMapping("/{conversationId}/regenerate")
    public ResponseEntity<ChatResponseDTO> regenerateTestCases(
        @PathVariable String conversationId,
        @RequestBody Map<String, String> request
    ) {
        try {
            log.info("Regenerating test cases for conversation: {}", conversationId);
            ChatRequestDTO chatRequest = ChatRequestDTO.builder()
                .conversationId(conversationId)
                .message(request.getOrDefault("message", ""))
                .actionType("regenerate")
                .build();
            
            ChatResponseDTO response = chatService.sendMessage(chatRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error regenerating test cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Modify test cases
     * POST /api/chat/{conversationId}/modify
     */
    @PostMapping("/{conversationId}/modify")
    public ResponseEntity<ChatResponseDTO> modifyTestCases(
        @PathVariable String conversationId,
        @RequestBody Map<String, String> request
    ) {
        try {
            log.info("Modifying test cases for conversation: {}", conversationId);
            ChatRequestDTO chatRequest = ChatRequestDTO.builder()
                .conversationId(conversationId)
                .message(request.getOrDefault("modifications", request.getOrDefault("message", "")))
                .actionType("modify")
                .build();
            
            ChatResponseDTO response = chatService.sendMessage(chatRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error modifying test cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Add more test cases
     * POST /api/chat/{conversationId}/add-more
     */
    @PostMapping("/{conversationId}/add-more")
    public ResponseEntity<ChatResponseDTO> addMoreTestCases(
        @PathVariable String conversationId
    ) {
        try {
            log.info("Adding more test cases for conversation: {}", conversationId);
            ChatRequestDTO chatRequest = ChatRequestDTO.builder()
                .conversationId(conversationId)
                .message("Add more edge cases")
                .actionType("add_more")
                .build();
            
            ChatResponseDTO response = chatService.sendMessage(chatRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding test cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Merge duplicate test cases
     * POST /api/chat/{conversationId}/merge
     */
    @PostMapping("/{conversationId}/merge")
    public ResponseEntity<ChatResponseDTO> mergeTestCases(
        @PathVariable String conversationId
    ) {
        try {
            log.info("Merging test cases for conversation: {}", conversationId);
            ChatRequestDTO chatRequest = ChatRequestDTO.builder()
                .conversationId(conversationId)
                .message("Merge duplicates")
                .actionType("merge")
                .build();
            
            ChatResponseDTO response = chatService.sendMessage(chatRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error merging test cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Find similar test cases
     * GET /api/chat/{conversationId}/similar
     */
    @GetMapping("/{conversationId}/similar")
    public ResponseEntity<List<SimilarTestCaseDTO>> findSimilarCases(
        @PathVariable String conversationId,
        @RequestParam(defaultValue = "5") int limit,
        @RequestParam(defaultValue = "0.7") float threshold
    ) {
        try {
            log.info("Finding similar test cases for conversation: {}", conversationId);
            // TODO: Get query embedding from request or latest test case
            float[] mockEmbedding = new float[1536];
            List<SimilarTestCaseDTO> response = vectorSearchService.findSimilarTestCases(
                mockEmbedding,
                UUID.fromString(conversationId),
                limit,
                threshold
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error finding similar cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Detect duplicate test cases
     * GET /api/chat/{conversationId}/duplicates
     */
    @GetMapping("/{conversationId}/duplicates")
    public ResponseEntity<Map<String, Object>> detectDuplicates(
        @PathVariable String conversationId
    ) {
        try {
            log.info("Detecting duplicates for conversation: {}", conversationId);
            // TODO: Implement duplicate detection
            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("duplicateCount", 0);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error detecting duplicates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Export test cases to Excel
     * GET /api/chat/{conversationId}/export
     */
    @GetMapping("/{conversationId}/export")
    public ResponseEntity<String> exportTestCases(
        @PathVariable String conversationId
    ) {
        try {
            log.info("Exporting test cases for conversation: {}", conversationId);
            // TODO: Implement Excel export
            return ResponseEntity.ok("Export started...");
        } catch (Exception e) {
            log.error("Error exporting test cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
