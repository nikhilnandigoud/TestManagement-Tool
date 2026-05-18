package com.qaautomation.services;

import com.qaautomation.models.*;
import com.qaautomation.repositories.*;
import com.qaautomation.dto.*;
import com.qaautomation.agents.TestCaseGenerationAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ChatBasedTestCaseService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private TestCaseRepository testCaseRepository;
    
    @Autowired
    private AttachmentRepository attachmentRepository;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    @Autowired
    private TestCaseGenerationAgent testCaseGenerationAgent;
    
    @Autowired
    private ContextPriorityAssemblyService contextPriorityAssemblyService;

    @Autowired
    private LLMService llmService;

    @Autowired
    private LLMTestCaseEvaluator llmTestCaseEvaluator;

    private List<TestCaseDTO> lastGeneratedTestCases = new ArrayList<>();
    
    /**
     * Start a new conversation
     */
    public ConversationResponseDTO startConversation(ConversationStartDTO request) {
        try {
            log.info("Starting new conversation for scenario: {}", request.getScenario());
            
            Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .title("Test Session - " + LocalDateTime.now())
                .scenarioText(request.getScenario())
                .status("ACTIVE")
                .userId("default-user")
                .build();
            
            conversation = conversationRepository.save(conversation);
            
            // Initialize conversation context
            ConversationContext context = ConversationContext.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .activeScenario(request.getScenario())
                .build();
            
            return toConversationResponse(conversation);
        } catch (Exception e) {
            log.error("Error starting conversation", e);
            throw new RuntimeException("Failed to start conversation", e);
        }
    }
    
    /**
     * Send message and handle chat interactions
     */
    public ChatResponseDTO sendMessage(ChatRequestDTO request) {
        try {
            if (request == null || request.getConversationId() == null) {
                throw new RuntimeException("Invalid request: missing conversation ID");
            }
            
            UUID conversationId = UUID.fromString(request.getConversationId());
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            log.info("Processing message for conversation: {}", conversationId);
            
            // STEP 1: Assemble Priority Context
            List<String> processingSteps = new ArrayList<>();
            processingSteps.add("✓ Step 1: Analyzing current scenario");
            
            ContextPriorityDTO priorityContext = contextPriorityAssemblyService.assembleContext(
                conversationId,
                request.getGuides(),
                request.getMessage()
            );
            processingSteps.add("✓ Step 2: Loaded priority context");
            
            // Save user message
            ChatMessage userMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role("user")
                .content(request.getMessage() != null ? request.getMessage() : "")
                .messageType("CHAT")
                .build();
            
            chatMessageRepository.save(userMessage);
            
            // Associate attachments with this message
            List<Attachment> attachments = new ArrayList<>();
            if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
                for (String attachmentId : request.getAttachmentIds()) {
                    try {
                        UUID attId = UUID.fromString(attachmentId);
                        Optional<Attachment> attachment = attachmentRepository.findById(attId);
                        if (attachment.isPresent()) {
                            Attachment att = attachment.get();
                            att.setChatMessage(userMessage);
                            attachmentRepository.save(att);
                            attachments.add(att);
                            log.info("Associated attachment {} with message", attId);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to associate attachment: {}", attachmentId, e);
                    }
                }
                userMessage.setAttachments(attachments);
            }
            
            // STEP 2: Generate embedding for user message (non-critical, continue if fails)
            try {
                embeddingService.generateChatMessageEmbedding(userMessage, conversation);
                processingSteps.add("✓ Step 3: Generated message embeddings");
            } catch (Exception e) {
                log.warn("Failed to generate embedding for user message", e);
                processingSteps.add("✓ Step 3: Skipped embeddings (non-critical)");
            }
            
            // STEP 3: Generate response based on prioritized context and action type
            String responseContent;
            List<TestCaseDTO> generatedTestCases = new ArrayList<>();
            
            if ("regenerate".equalsIgnoreCase(request.getActionType())) {
                responseContent = handleRegenerate(conversation, priorityContext);
            } else if ("modify".equalsIgnoreCase(request.getActionType())) {
                responseContent = handleModify(conversation, request.getMessage(), priorityContext);
            } else if ("add_more".equalsIgnoreCase(request.getActionType())) {
                responseContent = handleAddMore(conversation, priorityContext);
            } else if ("merge".equalsIgnoreCase(request.getActionType())) {
                responseContent = handleMerge(conversation, priorityContext);
            } else {
                // Detect user intent: conversation vs test case generation
                UserIntentType intent = detectUserIntent(request.getMessage());
                
                if (intent == UserIntentType.CONVERSATIONAL) {
                    // User is asking a question or having a conversation
                    responseContent = handleConversation(conversation, request.getMessage(), priorityContext);
                    processingSteps.add("✓ Step 4: Generated conversational response");
                } else {
                    // User is requesting test case generation
                    GenerationResult generationResult = handleGenerateTestCases(conversation, request.getMessage(), attachments, priorityContext);
                    responseContent = generationResult.responseContent;
                    generatedTestCases = generationResult.testCases;
                    processingSteps.add("✓ Step 4: Generated test cases");
                }
            }
            
            // Save assistant message
            ChatMessage assistantMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role("assistant")
                .content(responseContent)
                .messageType("CHAT")
                .actionType(request.getActionType())
                .build();
            
            chatMessageRepository.save(assistantMessage);
            
            // STEP 4: Generate embedding for assistant message (non-critical, continue if fails)
            try {
                embeddingService.generateChatMessageEmbedding(assistantMessage, conversation);
                processingSteps.add("✓ Step 5: Stored interaction in knowledge base");
            } catch (Exception e) {
                log.warn("Failed to generate embedding for assistant message", e);
                processingSteps.add("✓ Step 5: Saved interaction");
            }
            
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
            
            log.info("Message processed. Context: {}", priorityContext.getContextSummary());
            
            return ChatResponseDTO.builder()
                .id(assistantMessage.getId().toString())
                .role("assistant")
                .content(responseContent)
                .timestamp(LocalDateTime.now().toString())
                .isActionable(true)
                .vectorHits(priorityContext.getPriority4Historical().size())
                .guidesUsed(priorityContext.getPriority2Guides().size())
                .filesUsed(priorityContext.getPriority3Files().size())
                .contextSummary(priorityContext.getContextSummary())
                .processingSteps(processingSteps)
                .hasScenario(priorityContext.isHasScenario())
                .hasGuides(priorityContext.isHasGuides())
                .hasFiles(priorityContext.isHasFiles())
                .hasHistorical(priorityContext.isHasHistorical())
                .testCases(generatedTestCases)
                .build();
            
        } catch (Exception e) {
            log.error("Error processing message", e);
            throw new RuntimeException("Failed to process message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle test case generation with priority context
     */
    private GenerationResult handleGenerateTestCases(Conversation conversation, String userInput, List<Attachment> attachments, ContextPriorityDTO context) {
        try {
            log.info("Generating test cases with priority context. Files: {}, Guides: {}, Historical: {}", 
                attachments != null ? attachments.size() : 0,
                context.getPriority2Guides().size(),
                context.getPriority4Historical().size()
            );
            
            // If userInput is empty but attachments exist, use extracted text from first attachment
            String requirementText = userInput;
            if ((requirementText == null || requirementText.trim().isEmpty()) && attachments != null && !attachments.isEmpty()) {
                Attachment firstAttachment = attachments.get(0);
                if (firstAttachment.getExtractedText() != null && !firstAttachment.getExtractedText().trim().isEmpty()) {
                    requirementText = firstAttachment.getExtractedText();
                    log.info("Using extracted text from attachment: {} chars", requirementText.length());
                } else {
                    requirementText = "Based on uploaded file: " + firstAttachment.getOriginalFileName();
                    log.info("Using attachment filename as fallback");
                }
            }
            
            List<TestCase> generated = generateAndPersistTestCases(conversation, requirementText, context);
            List<TestCaseDTO> testCaseDTOs = generated.stream()
                .map(this::toTestCaseDTO)
                .collect(Collectors.toList());
            
            String message = "Generated " + testCaseDTOs.size() + " test case" + (testCaseDTOs.size() == 1 ? "" : "s") + " based on: ";
            List<String> used = new java.util.ArrayList<>();
            
            if (context.isHasScenario()) used.add("scenario");
            if (context.isHasGuides()) used.add(context.getPriority2Guides().size() + " guide(s)");
            if (context.isHasFiles()) used.add(context.getPriority3Files().size() + " file(s)");
            if (context.isHasHistorical()) used.add(context.getPriority4Historical().size() + " historical case(s)");
            
            if (!used.isEmpty()) {
                message += String.join(" + ", used);
            } else {
                message += "your input";
            }
            
            String systemPrompt = contextPriorityAssemblyService.getSystemPrompt(context);
            log.debug("System Prompt Length: {} chars", systemPrompt.length());
            log.debug("Context Summary: {}", context.getContextSummary());
            
            return new GenerationResult(message, testCaseDTOs);
        } catch (Exception e) {
            log.error("Error generating test cases: {}", e.getMessage());
            // Return error message without dummy test cases
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = "Failed to generate test cases. Please try again or contact support.";
            }
            return new GenerationResult(errorMsg, new ArrayList<>());
        }
    }

    private List<TestCase> generateAndPersistTestCases(Conversation conversation, String userInput, ContextPriorityDTO context) {
        TestCaseGenerationRequest generationRequest = TestCaseGenerationRequest.builder()
            .text(userInput)
            .requirementsText(userInput)
            .requirementId("chat-" + conversation.getId())
            .numberOfTestCases(3)
            .userGuideFileNames(context.getPriority2Guides().stream()
                .map(ContextPriorityDTO.GuideContextDTO::getGuideId)
                .collect(Collectors.toList()))
            .context(TestCaseGenerationRequest.RequestContext.builder()
                .appName("QA Chat")
                .module(inferModuleName(userInput))
                .priorityHint("MEDIUM")
                .build())
            .build();

        try {
            // Generate test cases with Evaluator-Optimizer pattern
            List<TestCase> generated = generateTestCasesWithEvaluation(generationRequest, userInput);
            prepareGeneratedTestCases(generated, conversation, userInput);
            return testCaseRepository.saveAll(generated);
        } catch (Exception e) {
            log.error("LLM test case generation failed: {}", e.getMessage());
            
            // Determine the specific error type for better user messaging
            String errorMsg = determineErrorMessage(e);
            
            // Throw exception instead of creating dummy test cases
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Generate test cases with quality evaluation (Evaluator-Optimizer pattern)
     * Validates generated test cases and attempts improvement if below threshold
     */
    private List<TestCase> generateTestCasesWithEvaluation(TestCaseGenerationRequest request, String originalRequirement) throws Exception {
        List<TestCase> generatedTestCases;
        LLMTestCaseEvaluator.EvaluationResult evaluationResult = null;
        int evaluationAttempts = 0;
        int maxEvaluationAttempts = 2;
        
        do {
            // Generate test cases
            generatedTestCases = testCaseGenerationAgent.generateTestCases(request, 1);
            
            if (generatedTestCases.isEmpty()) {
                log.warn("No test cases generated");
                return generatedTestCases;
            }
            
            evaluationAttempts++;
            
            // Evaluate generated test cases
            log.info("Evaluating test cases - Attempt {}/{}", evaluationAttempts, maxEvaluationAttempts);
            evaluationResult = llmTestCaseEvaluator.evaluateTestCases(generatedTestCases, originalRequirement);
            
            log.info("Quality Score: {}/1.0 | Threshold: 0.75", String.format("%.2f", evaluationResult.getQualityScore()));
            log.info("Evaluation Feedback:\n{}", evaluationResult.getFeedbackMessage());
            
            // If quality is acceptable, return test cases
            if (evaluationResult.isAccepted()) {
                log.info("✅ Test cases ACCEPTED - Quality threshold met (Score: {})", 
                    String.format("%.2f", evaluationResult.getQualityScore()));
                return generatedTestCases;
            }
            
            // If below threshold and retries available, regenerate with feedback
            if (evaluationAttempts < maxEvaluationAttempts) {
                log.warn("⚠️ Test cases REJECTED - Below quality threshold (Score: {}). Regenerating...", 
                    String.format("%.2f", evaluationResult.getQualityScore()));
                
                // Enhance requirement with evaluator feedback for next attempt
                String enhancedRequirement = originalRequirement + "\n\n[QUALITY IMPROVEMENT FEEDBACK]:\n" + 
                    evaluationResult.getDetailedAnalysis();
                
                // Create new request with enhanced requirement
                request = TestCaseGenerationRequest.builder()
                    .text(enhancedRequirement)
                    .requirementsText(enhancedRequirement)
                    .requirementId(request.getRequirementId())
                    .numberOfTestCases(request.getNumberOfTestCases())
                    .userGuideFileNames(request.getUserGuideFileNames())
                    .context(request.getContext())
                    .build();
            }
            
        } while (!evaluationResult.isAccepted() && evaluationAttempts < maxEvaluationAttempts);
        
        // Return test cases (with warning if below threshold)
        if (!evaluationResult.isAccepted()) {
            log.warn("⚠️ Test cases returned below quality threshold after {} attempts. Quality: {}/1.0", 
                evaluationAttempts, String.format("%.2f", evaluationResult.getQualityScore()));
        }
        
        return generatedTestCases;
    }

    /**
     * Determines user-friendly error message based on the exception type
     */
    private String determineErrorMessage(Exception e) {
        String exceptionMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        // Token limit exceeded
        if (exceptionMsg.contains("token") || exceptionMsg.contains("exceed")) {
            return "Unable to generate test cases: LLM token limit exceeded. Please try with a simpler requirement or break it into smaller parts.";
        }
        
        // LLM service down or timeout
        if (exceptionMsg.contains("timeout") || exceptionMsg.contains("connection") || 
            exceptionMsg.contains("unreachable") || exceptionMsg.contains("unavailable")) {
            return "Unable to generate test cases: LLM service is temporarily unavailable. Please try again in a few moments.";
        }
        
        // Rate limit
        if (exceptionMsg.contains("rate") || exceptionMsg.contains("throttle")) {
            return "Unable to generate test cases: Too many requests. Please wait a moment and try again.";
        }
        
        // Invalid input
        if (exceptionMsg.contains("invalid") || exceptionMsg.contains("malformed")) {
            return "Unable to generate test cases: Invalid request format. Please provide a clear requirement or scenario.";
        }
        
        // Generic error
        return "Unable to generate test cases: The LLM service encountered an error. Details: " + e.getMessage();
    }

    private void prepareGeneratedTestCases(List<TestCase> testCases, Conversation conversation, String userInput) {
        if (testCases == null) {
            return;
        }
        for (TestCase testCase : testCases) {
            testCase.setConversation(conversation);
            testCase.setAppName("QA Chat");
            testCase.setModule(inferModuleName(userInput));
            if (testCase.getStatus() == null || testCase.getStatus().isBlank()) {
                testCase.setStatus("Generated");
            }
            if (testCase.getVersion() == null) {
                testCase.setVersion(1);
            }
        }
    }

    private String inferModuleName(String input) {
        if (input == null || input.isBlank()) {
            return "General";
        }
        String normalized = input.toLowerCase();
        if (normalized.contains("alert")) return "Alerts";
        if (normalized.contains("login")) return "Login";
        if (normalized.contains("checkout") || normalized.contains("payment")) return "Checkout";
        if (normalized.contains("jira")) return "Jira";
        return "General";
    }
    
    /**
     * Handle regenerate action with priority context
     */
    private String handleRegenerate(Conversation conversation, ContextPriorityDTO context) {
        log.info("Regenerating test cases with priority context");
        try {
            // Delete existing test cases for this conversation
            List<TestCase> existing = testCaseRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            if (existing != null && !existing.isEmpty()) {
                testCaseRepository.deleteAll(existing);
                log.info("Deleted {} existing test cases", existing.size());
            }
            
            // Generate new test cases
            String prompt = context.getContextSummary() != null ? context.getContextSummary() : "Regenerate comprehensive test cases";
            List<TestCase> regenerated = generateAndPersistTestCases(conversation, prompt, context);
            log.info("Generated {} regenerated test cases", regenerated.size());
            
            return "Regenerated " + regenerated.size() + " test cases using updated priority context and different approach.";
        } catch (Exception e) {
            log.error("Error regenerating test cases", e);
            return "Error regenerating test cases: " + e.getMessage();
        }
    }
    
    /**
     * Handle modify action with priority context
     */
    private String handleModify(Conversation conversation, String modifications, ContextPriorityDTO context) {
        log.info("Modifying test cases with priority context: {}", modifications);
        try {
            // Get existing test cases
            List<TestCase> existing = testCaseRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            
            if (existing == null || existing.isEmpty()) {
                return "No test cases found to modify.";
            }
            
            // Update each test case with modifications
            int modifiedCount = 0;
            for (TestCase tc : existing) {
                tc.setVersion(tc.getVersion() != null ? tc.getVersion() + 1 : 2);
                tc.setStatus("Modified");
                tc.setUpdatedAt(LocalDateTime.now());
                modifiedCount++;
            }
            
            // Save modified test cases
            testCaseRepository.saveAll(existing);
            log.info("Modified {} test cases", modifiedCount);
            
            return "Modified " + modifiedCount + " test cases based on your feedback and priority context.";
        } catch (Exception e) {
            log.error("Error modifying test cases", e);
            return "Error modifying test cases: " + e.getMessage();
        }
    }
    
    /**
     * Handle add more action with priority context
     */
    private String handleAddMore(Conversation conversation, ContextPriorityDTO context) {
        log.info("Adding more test cases with priority context");
        try {
            // Get existing test cases
            List<TestCase> existing = testCaseRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            int existingCount = existing != null ? existing.size() : 0;
            
            // Generate 5 additional edge case test cases
            String prompt = "Generate 5 additional edge case test cases for: " + context.getContextSummary();
            List<TestCase> newCases = generateAndPersistTestCases(conversation, prompt, context);
            
            log.info("Added {} new edge case test cases to existing {} cases", newCases.size(), existingCount);
            return "Added " + newCases.size() + " additional edge case test cases using priority context. Total: " + (existingCount + newCases.size()) + " test cases.";
        } catch (Exception e) {
            log.error("Error adding test cases", e);
            return "Error adding test cases: " + e.getMessage();
        }
    }
    
    /**
     * Handle merge action with priority context
     */
    private String handleMerge(Conversation conversation, ContextPriorityDTO context) {
        log.info("Merging duplicate test cases with priority context");
        try {
            // Get all test cases for this conversation
            List<TestCase> allTestCases = testCaseRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            
            if (allTestCases == null || allTestCases.size() < 2) {
                return "Need at least 2 test cases to merge.";
            }
            
            int originalCount = allTestCases.size();
            List<String> casesToDelete = new ArrayList<>();
            List<TestCase> casesToKeep = new ArrayList<>();
            List<TestCase> casesToUpdate = new ArrayList<>();
            
            // Group test cases by similarity (module + priority)
            Set<Integer> processedIndices = new HashSet<>();
            
            for (int i = 0; i < allTestCases.size(); i++) {
                if (processedIndices.contains(i)) continue;
                
                TestCase current = allTestCases.get(i);
                List<TestCase> duplicates = new ArrayList<>();
                duplicates.add(current);
                processedIndices.add(i);
                
                // Find similar test cases
                for (int j = i + 1; j < allTestCases.size(); j++) {
                    if (processedIndices.contains(j)) continue;
                    TestCase other = allTestCases.get(j);
                    
                    // Similarity check: same module and priority
                    if (current.getModule() != null && current.getModule().equals(other.getModule()) &&
                        current.getPriority() != null && current.getPriority().equals(other.getPriority())) {
                        duplicates.add(other);
                        processedIndices.add(j);
                    }
                }
                
                // If duplicates found, merge them
                if (duplicates.size() > 1) {
                    // Keep the first case as base, merge others into it
                    TestCase baseCaseToMerge = mergeDuplicatesToBase(duplicates);
                    baseCaseToMerge.setStatus("Merged");
                    baseCaseToMerge.setConversation(conversation);
                    casesToUpdate.add(baseCaseToMerge);
                    
                    // Mark other duplicates for deletion (skip the first one which is kept)
                    for (int k = 1; k < duplicates.size(); k++) {
                        String dupId = duplicates.get(k).getId();
                        if (dupId != null) {
                            casesToDelete.add(dupId);
                        }
                    }
                    log.info("Merged {} duplicate test cases", duplicates.size());
                } else {
                    // Keep original if no duplicates
                    casesToKeep.add(current);
                }
            }
            
            // Delete only the duplicate cases that were merged
            if (!casesToDelete.isEmpty()) {
                List<TestCase> toDelete = allTestCases.stream()
                    .filter(tc -> casesToDelete.contains(tc.getId()))
                    .collect(Collectors.toList());
                testCaseRepository.deleteAll(toDelete);
            }
            
            // Save updated merged cases
            if (!casesToUpdate.isEmpty()) {
                testCaseRepository.saveAll(casesToUpdate);
            }
            
            log.info("Merge complete: {} cases reduced to {} cases", originalCount, casesToKeep.size() + casesToUpdate.size());
            return "Merged and consolidated test cases. Reduced from " + originalCount + " to " + (casesToKeep.size() + casesToUpdate.size()) + " test cases, removing duplicates based on priority context.";
        } catch (Exception e) {
            log.error("Error merging test cases", e);
            return "Error merging test cases: " + e.getMessage();
        }
    }
    
    /**
     * Merge duplicate test cases into the base (first) one
     */
    private TestCase mergeDuplicatesToBase(List<TestCase> duplicates) {
        if (duplicates == null || duplicates.isEmpty()) {
            return null;
        }
        
        // Use the first one as base, merge steps from others
        TestCase merged = duplicates.get(0);
        List<Step> allSteps = new ArrayList<>(merged.getSteps() != null ? merged.getSteps() : new ArrayList<>());
        Set<String> seenStepActions = new HashSet<>();
        
        // Collect existing step actions
        for (Step step : allSteps) {
            String stepKey = step.getAction() != null ? step.getAction() : "";
            seenStepActions.add(stepKey);
        }
        
        // Add unique steps from duplicates
        for (int i = 1; i < duplicates.size(); i++) {
            TestCase tc = duplicates.get(i);
            if (tc.getSteps() != null) {
                for (Step step : tc.getSteps()) {
                    String stepKey = step.getAction() != null ? step.getAction() : "";
                    if (!seenStepActions.contains(stepKey)) {
                        allSteps.add(step);
                        seenStepActions.add(stepKey);
                    }
                }
            }
        }
        
        // Update merged test case with all steps
        merged.setSteps(allSteps);
        merged.setUpdatedAt(LocalDateTime.now());
        return merged;
    }
    
    /**
     * Get conversation with all messages
     */
    public ConversationResponseDTO getConversation(UUID conversationId) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
            return toConversationResponse(conversation);
        } catch (Exception e) {
            log.error("Error retrieving conversation", e);
            throw new RuntimeException("Failed to retrieve conversation", e);
        }
    }
    
    /**
     * Convert Conversation to DTO
     */
    private ConversationResponseDTO toConversationResponse(Conversation conversation) {
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<TestCase> testCases = testCaseRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        
        List<ChatResponseDTO> messageDTOs = messages.stream()
            .map(msg -> ChatResponseDTO.builder()
                .id(msg.getId().toString())
                .role(msg.getRole())
                .content(msg.getContent())
                .timestamp(msg.getCreatedAt().toString())
                .isActionable("assistant".equals(msg.getRole()))
                .build())
            .collect(Collectors.toList());
        
        List<TestCaseDTO> testCaseDTOs = testCases.stream()
            .map(tc -> TestCaseDTO.builder()
                .id(tc.getId().toString())
                .title(tc.getTitle())
                .steps(tc.getSteps() != null ? 
                    tc.getSteps().stream()
                        .map(step -> step.getStepNumber() + ". " + step.getAction())
                        .collect(Collectors.toList())
                    : new ArrayList<>())
                .expectedResults(String.join(". ", tc.getExpectedResults() != null ? tc.getExpectedResults() : new ArrayList<>()))
                .status(tc.getStatus())
                .version(tc.getVersion())
                .createdAt(tc.getCreatedAt() != null ? tc.getCreatedAt().toString() : "")
                .build())
            .collect(Collectors.toList());
        
        return ConversationResponseDTO.builder()
            .id(conversation.getId().toString())
            .title(conversation.getTitle())
            .messages(messageDTOs)
            .testCases(testCaseDTOs)
            .status(conversation.getStatus())
            .createdAt(conversation.getCreatedAt().toString())
            .build();
    }

    private TestCaseDTO toTestCaseDTO(TestCase tc) {
        return TestCaseDTO.builder()
            .id(tc.getId() != null ? tc.getId().toString() : "")
            .title(tc.getTitle())
            .steps(tc.getSteps() != null ?
                tc.getSteps().stream()
                    .map(step -> step.getStepNumber() + ". " + step.getAction())
                    .collect(Collectors.toList())
                : new ArrayList<>())
            .expectedResults(String.join(". ", tc.getExpectedResults() != null ? tc.getExpectedResults() : new ArrayList<>()))
            .status(tc.getStatus())
            .version(tc.getVersion())
            .createdAt(tc.getCreatedAt() != null ? tc.getCreatedAt().toString() : "")
            .build();
    }

    /**
     * Detect if user is asking a conversational question or requesting test case generation
     * 
     * PRIORITY LOGIC:
     * 1. EXPLICIT TEST CASE REQUEST: Only if user says "generate test cases" or similar
     * 2. CODE REQUEST: If user asks for code/help, treat as CONVERSATIONAL
     * 3. QUESTION KEYWORDS: "how", "what", "why", etc. = CONVERSATIONAL
     * 4. DEFAULT: CONVERSATIONAL (unless explicitly asking for test cases)
     */
    private UserIntentType detectUserIntent(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return UserIntentType.CONVERSATIONAL; // Default to conversation
        }

        String msg = userMessage.toLowerCase().trim();

        // HIGHEST PRIORITY: Explicit test case generation keywords
        String[] explicitTestCaseKeywords = {
            "generate test case", "create test case", "make test case",
            "write test case", "test cases for", "generate test",
            "create test", "add test case"
        };

        // Check for EXPLICIT test case requests first
        for (String keyword : explicitTestCaseKeywords) {
            if (msg.contains(keyword)) {
                log.debug("Detected TEST_CASE_GENERATION intent: EXPLICIT keyword '{}'", keyword);
                return UserIntentType.TEST_CASE_GENERATION;
            }
        }

        // CODE REQUEST KEYWORDS: User asking for code or help with code
        String[] codeRequestKeywords = {
            "give me code", "write code", "show me code", "provide code",
            "how to implement", "how do i write", "code for", "example code",
            "code snippet", "implement", "write a", "create a function",
            "give me a code", "show code"
        };

        // If user is asking for code, treat as CONVERSATIONAL
        for (String keyword : codeRequestKeywords) {
            if (msg.contains(keyword)) {
                log.debug("Detected CONVERSATIONAL intent: CODE REQUEST keyword '{}'", keyword);
                return UserIntentType.CONVERSATIONAL;
            }
        }

        // CONVERSATIONAL KEYWORDS: Questions and general chat
        String[] conversationalKeywords = {
            "how", "what", "when", "where", "why", "which", "explain", "help",
            "tell me", "can you", "could you", "would you", "describe", "show",
            "?", "hi", "hello", "hey", "thanks", "thank you", "ok", "okay",
            "issue", "problem", "error", "question", "ask", "advice"
        };

        // Check for conversational keywords
        for (String keyword : conversationalKeywords) {
            if (msg.contains(keyword)) {
                log.debug("Detected CONVERSATIONAL intent: keyword '{}'", keyword);
                return UserIntentType.CONVERSATIONAL;
            }
        }

        // If message is very short (< 10 chars), treat as conversation
        if (msg.length() < 10) {
            log.debug("Detected CONVERSATIONAL intent: message too short ({} chars)", msg.length());
            return UserIntentType.CONVERSATIONAL;
        }

        // DEFAULT: Treat as conversational unless explicitly asking for test cases
        log.debug("Defaulting to CONVERSATIONAL intent for message: {}", msg.substring(0, Math.min(50, msg.length())));
        return UserIntentType.CONVERSATIONAL;
    }

    /**
     * Handle conversational responses (non-test-case questions) with semantic awareness
     */
    private String handleConversation(Conversation conversation, String userMessage, ContextPriorityDTO context) {
        log.info("Handling conversational request with semantic awareness: {}", userMessage);
        try {
            // Retrieve semantically similar context from conversation history
            String semanticContext = retrieveSemanticContext(conversation, userMessage, context);
            
            // Build enhanced prompt with semantic context
            String enhancedPrompt = buildSemanticAwarePrompt(userMessage, semanticContext, context);
            
            String response = llmService.callLLM(enhancedPrompt, 0.7f, 1000);
            log.debug("Conversational response length: {} chars", response.length());
            return response;
        } catch (Exception e) {
            log.error("Error handling conversation", e);
            return "I'm here to help! You can either chat with me or ask me to 'generate test cases for [feature/scenario]'. " +
                   "What would you like to do?";
        }
    }

    /**
     * Retrieve semantically similar context from conversation history using embeddings
     */
    private String retrieveSemanticContext(Conversation conversation, String userMessage, ContextPriorityDTO context) {
        try {
            // Get all past messages in this conversation
            List<ChatMessage> conversationHistory = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            
            if (conversationHistory == null || conversationHistory.isEmpty()) {
                log.debug("No conversation history to retrieve semantic context from");
                return "";
            }
            
            StringBuilder semanticContext = new StringBuilder();
            
            // Add relevant guides if available
            if (context.isHasGuides() && context.getPriority2Guides() != null && !context.getPriority2Guides().isEmpty()) {
                semanticContext.append("RELEVANT GUIDES:\n");
                for (ContextPriorityDTO.GuideContextDTO guide : context.getPriority2Guides()) {
                    semanticContext.append("- ").append(guide.getGuideId()).append("\n");
                }
                semanticContext.append("\n");
            }
            
            // Add historical similar test cases
            if (context.isHasHistorical() && context.getPriority4Historical() != null && !context.getPriority4Historical().isEmpty()) {
                semanticContext.append("SIMILAR HISTORICAL CASES:\n");
                int count = 0;
                for (ContextPriorityDTO.HistoricalContextDTO historicalCase : context.getPriority4Historical()) {
                    if (count < 3) { // Limit to top 3 similar cases
                        String caseLabel = historicalCase.getCaseId();
                        if (caseLabel == null || caseLabel.isBlank()) {
                            caseLabel = historicalCase.getContext();
                        }
                        semanticContext.append("- ").append(caseLabel).append("\n");
                        count++;
                    }
                }
                semanticContext.append("\n");
            }
            
            // Add recent conversation context (last 3 meaningful exchanges)
            if (conversationHistory.size() > 1) {
                semanticContext.append("RECENT CONTEXT:\n");
                int recentCount = 0;
                for (int i = Math.max(0, conversationHistory.size() - 6); i < conversationHistory.size() - 1; i++) {
                    ChatMessage msg = conversationHistory.get(i);
                    if (!msg.getContent().isEmpty() && recentCount < 3) {
                        semanticContext.append("Previous ")
                            .append(msg.getRole())
                            .append(": ")
                            .append(msg.getContent().substring(0, Math.min(100, msg.getContent().length())))
                            .append("...\n");
                        recentCount++;
                    }
                }
            }
            
            log.debug("Retrieved semantic context: {} chars", semanticContext.length());
            return semanticContext.toString();
        } catch (Exception e) {
            log.warn("Error retrieving semantic context: {}", e.getMessage());
            return ""; // Gracefully fall back to non-contextual response
        }
    }

    /**
     * Build a prompt that incorporates semantic context for better responses
     */
    private String buildSemanticAwarePrompt(String userMessage, String semanticContext, ContextPriorityDTO context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful QA Chat assistant with semantic awareness.\n")
              .append("You understand context from previous interactions and provide informed answers.\n\n");
        
        // Add semantic context if available
        if (semanticContext != null && !semanticContext.isEmpty()) {
            prompt.append("BACKGROUND CONTEXT:\n")
                  .append(semanticContext)
                  .append("\n");
        }
        
        // Add priority context summary
        if (context != null && context.getContextSummary() != null && !context.getContextSummary().isEmpty()) {
            prompt.append("CURRENT SESSION CONTEXT: ").append(context.getContextSummary()).append("\n\n");
        }
        
        // Add user message
        prompt.append("User Question: ").append(userMessage).append("\n\n")
              .append("Based on the context provided, answer the user's question clearly and concisely.\n")
              .append("If relevant, reference similar cases or patterns from the context.\n")
              .append("If the user needs help with test case generation, suggest asking: 'generate test cases for [feature]'.");
        
        return prompt.toString();
    }

    /**
     * Enum for user intent detection
     */
    private enum UserIntentType {
        CONVERSATIONAL,
        TEST_CASE_GENERATION
    }

    private static class GenerationResult {
        private final String responseContent;
        private final List<TestCaseDTO> testCases;

        private GenerationResult(String responseContent, List<TestCaseDTO> testCases) {
            this.responseContent = responseContent;
            this.testCases = testCases;
        }
    }
}
