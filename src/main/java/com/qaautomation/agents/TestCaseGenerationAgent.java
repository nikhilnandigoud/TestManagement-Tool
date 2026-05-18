package com.qaautomation.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaautomation.models.Step;
import com.qaautomation.models.TestCase;
import com.qaautomation.models.TestCaseGenerationRequest;
import com.qaautomation.models.UserGuideContext;
import com.qaautomation.services.LLMService;
import com.qaautomation.services.LLMTestCaseEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Test Case Generation Agent - Converts requirements text into structured test cases.
 * Responsibilities:
 * - Accept requirement text
 * - Generate multiple test case types (positive, negative, boundary)
 * - Produce structured test cases with steps and expected results
 * - Return test cases in JSON format
 */
@Slf4j
@Component
public class TestCaseGenerationAgent implements Agent {

    private final LLMService llmService;
    private final ObjectMapper objectMapper;
    private final UserGuideProcessor userGuideProcessor;
    private final LLMTestCaseEvaluator llmTestCaseEvaluator;

    private static final String PROMPT_TEMPLATE = """
        You are an expert QA engineer and test case designer. Your role is to generate comprehensive, 
        executable test cases that are UI-synchronized, reproducible by any user, and ready for both manual 
        and automated execution.
        
              
        =====================================================================
        TEST CASE GENERATION INSTRUCTIONS - 18 COMPREHENSIVE REQUIREMENTS
        =====================================================================
        
        1. OBJECTIVE: Convert scenarios into Excel-ready test cases that are:
        Fully reproducible | UI-synchronized | Executable by freshers | Ready for manual & automation
        
        2. INPUT ANALYSIS & REFERENCE PRIORITY (STRICT ORDER):
        PRIMARY: User scenario text | SECONDARY: User guides (UI/UX/navigation) | TERTIARY: Attached files
        Must fully analyze without inventing UI/workflows/behaviors
        
        3. TEST CASE COVERAGE REQUIREMENTS:
        Generate positive, negative & edge case scenarios. Exclude accessibility unless explicitly requested.
        
        4. TEST CASE TITLE RULES (STRICT & MANDATORY):
        4.1 Title Placement: Only on FIRST row per test case. NO title repetition in subsequent rows.
        4.2 Prefix Rule: EXACTLY ONE prefix per title (from allowed list). No multiple/missing prefixes.
        4.3 Allowed Prefixes: UAT-Additional-Information | UAT-Administration-Security | UAT-Administration-Settings |
        UAT-Modeling-Decision | UAT-Modeling-DecisionFlow | UAT-Modeling-FactType | UAT-Modeling-KnowledgeModel |
        UAT-Modeling-KnowledgeSource | UAT-Modeling-ModelAI | UAT-Modeling-RuleFamily | UAT-Repo-Assets |
        UAT-Repo-DeployAssets | UAT-Repo-ModelingProjects | UAT-Repo-ReleaseProjects | UAT-Repo-Tasks&Governance |
        UAT-Reports | UAT-Search | UAT-Task-FactType | UAT-Task-Governance | UAT-Task-KnowledgeSource |
        UAT-Testing-Decision | UAT-Testing-DecisionFlow | UAT-Testing-RuleFamily | UAT-Validation-Decision |
        UAT-Validation-RuleFamily
        4.4 Title Structure: Comma-separated | Concise | Descriptive | Unique | MUST END WITH "Verify" |
        Use user domain terminology (NOT generic). Examples: Update Event Definition, Default Operators, Verify Operators,
        Decision Flow, Task, Community, Fact Type, Rule Family, Business Information Model (BIM), BIM Entity,
        Knowledge Source (KS), Draft, Testing Panel, Validation Panel, Asset Validation Status, Clone Test Case, etc.
        NO invented naming styles allowed.
        
        5. PRECONDITIONS & LOGIN RULES:
        Preconditions: FIRST step | MUST include word "Pre-condition" | Example: "Pre-condition: User logged in & has module access"
        Login: AFTER preconditions | BEFORE functional steps | Mandatory for all user-level operations
        
        6. TEST STEPS WRITING RULES (FRESHER-FRIENDLY - MANDATORY):
        One action per step | Sequential | Explicit | Script-ready | Unambiguous | Executable by non-domain user
        
        7. UI NAVIGATION RULES (CRITICAL FOR REPRODUCIBILITY):
        Every step MUST specify: Current screen/module/workspace | Exact UI element name | Exact interaction type
        AVOID vague terms: "Navigate to", "Open", "Check", "Go to" | REPLACE WITH specific steps:
        "From [Module Name] screen, click [Exact Button Name]" | "Select [Exact Dropdown Option]"
        
        8. STEP STRUCTURING RULES:
        NO combined actions | Navigation & validation are SEPARATE steps | Each step = verifiable outcome
        
        9. EXPECTED RESULT RULES:
        Every step requires expected result | Use "should" in all results | Describe system behavior (not user action) |
        Must be UI-verifiable | Example: "System should display Fact Type Summary panel with 'Operators' field"
        
        10. VALIDATION & DEFAULT SELECTION RULES:
        State available options | State default selection | State verification location | Capture EXACT error messages |
        NO paraphrasing of system messages
        
        11. ASSUMPTION CONTROL:
        DO NOT assume: User permissions | Draft availability | Rule Family association | State all as Pre-conditions
        
        12. OUTPUT FORMAT (STRICT):
        Table format only | Ready for Excel paste | Exact column order (below):
        | Test Case Title | Test Steps | Expected Result | Labels | Automation State | Test Case Status | Created in Sprint |
        
        13. EXCEL ROW RULES:
        Each test step = own row | Expected result = SAME row as step | NO merged steps allowed
        
        14. COLUMN-SPECIFIC RULES:
        "Labels": Leave blank | "Automation State": Always "Not Automated" | "Test Case Status": Leave blank |
        "Created in Sprint": Leave blank
        
        15. MANDATORY INCLUSIONS IN EVERY TEST CASE:
        ✓ Correct prefix label ✓ One unique title per test case ✓ Preconditions (if applicable) as first step
        ✓ UI-specific reproducible steps ✓ Expected results with "should" ✓ Automation State = "Not Automated"
        
        16. SCENARIO REPRODUCTION SUPPORT:
        Provide step-by-step execution flow | Maintain execution order | Include system behavior at each step
        
        17. YOUR WORKFLOW:
        1) Analyze scenario | 2) Identify testable components | 3) Create prefixed convention-compliant titles |
        4) Add detailed UI-synced steps | 5) Add step-level expected results | 6) Output Excel-ready table
        
        18. OUTPUT CONSTRAINTS:
        Provide JSON only. Do not include markdown, explanations, code fences, or table formatting.
        
        ===================================================================
        FINAL QUALITY CHECKLIST:
        ✔ Exact prefix usage (no generic labels) ✔ "Verify" title ending ✔ User-defined domain terminology
        ✔ No duplicate/vague titles ✔ Fresher-friendly UI-accurate steps ✔ Reproducible by non-domain user
        ✔ No assumptions about permissions/drafts ✔ Clear Excel column structure
        ===================================================================

        ===================================================================
        INPUT TO TEST
        ===================================================================
        Requirement:
        %s

        Application: %s
        Module: %s
        Priority hint: %s

        ===================================================================
        REQUIRED OUTPUT JSON SCHEMA
        ===================================================================
        Return a JSON array only. Each array item must be one complete test case:
        [
          {
            "title": "UAT-Repo-ReleaseProjects, Create Release Project, Verify",
            "description": "Short purpose of this test case",
            "preconditions": "Pre-condition: User logged in and has required access",
            "priority": "HIGH",
            "tags": ["release-project"],
            "steps": [
              {
                "action": "Pre-condition: User logged in and has required access",
                "testData": "",
                "expectedResult": "System should allow the user to access the required module"
              },
              {
                "action": "From Repository screen, click Release Projects tab",
                "testData": "",
                "expectedResult": "System should display Release Projects list"
              }
            ],
            "expectedResults": ["System should display Release Projects list"],
            "estimatedComplexity": 0.5
          }
        ]

        Generate 6 to 8 complete, non-empty test cases. Every test case must have a meaningful title and at least 3 steps.
        The first item in every steps array must be the Pre-condition step, and every step object must include a non-empty expectedResult.
        """;

    public TestCaseGenerationAgent(LLMService llmService, ObjectMapper objectMapper, UserGuideProcessor userGuideProcessor, LLMTestCaseEvaluator llmTestCaseEvaluator) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.userGuideProcessor = userGuideProcessor;
        this.llmTestCaseEvaluator = llmTestCaseEvaluator;
    }

    @Override
    public String getDescription() {
        return "Generates structured test cases from requirement text using AI";
    }

    @Override
    public boolean canHandle(Object input) {
        return input instanceof TestCaseGenerationRequest;
    }

    @Override
    public boolean execute() {
        // Implementation handled by service layer
        return true;
    }

    /**
     * Generates test cases with LLM Evaluator-Optimizer pattern
     * Validates generated test cases and regenerates if quality threshold not met
     * 
     * @param request Test case generation request
     * @param maxRetries Maximum retry attempts
     * @param requirementText Original requirement for evaluation context
     * @return List of validated test cases
     */
    public List<TestCase> generateTestCasesWithEvaluation(TestCaseGenerationRequest request, int maxRetries, String requirementText) throws Exception {
        List<TestCase> generatedTestCases;
        LLMTestCaseEvaluator.EvaluationResult evaluationResult = null;
        int evaluationAttempts = 0;
        int maxEvaluationAttempts = 2; // Max times to retry based on evaluation feedback
        
        do {
            // Generate test cases
            generatedTestCases = generateTestCases(request, maxRetries);
            
            if (generatedTestCases.isEmpty()) {
                log.warn("No test cases generated");
                return generatedTestCases;
            }
            
            evaluationAttempts++;
            
            // Evaluate generated test cases
            log.info("Evaluating test cases - Attempt {}/{}", evaluationAttempts, maxEvaluationAttempts);
            evaluationResult = llmTestCaseEvaluator.evaluateTestCases(generatedTestCases, requirementText);
            
            log.info("Evaluation Score: {}", evaluationResult.getQualityScore());
            log.info("Evaluation Feedback:\n{}", evaluationResult.getFeedbackMessage());
            
            // If quality is acceptable, return test cases
            if (evaluationResult.isAccepted()) {
                log.info("✅ Test cases passed quality evaluation (Score: {}, Threshold: 0.75)", 
                    String.format("%.2f", evaluationResult.getQualityScore()));
                return generatedTestCases;
            }
            
            // If quality is below threshold and we have retries left, regenerate with feedback
            if (evaluationAttempts < maxEvaluationAttempts) {
                log.warn("⚠️ Test cases below quality threshold (Score: {}, Threshold: 0.75). Regenerating...", 
                    String.format("%.2f", evaluationResult.getQualityScore()));
                
                // Enhance request with evaluation feedback
                String originalText = request.getText();
                String enhancedText = originalText + "\n\n[EVALUATOR FEEDBACK FOR IMPROVEMENT]:\n" + 
                    evaluationResult.getFeedbackMessage();
                request.setText(enhancedText);
            }
            
        } while (!evaluationResult.isAccepted() && evaluationAttempts < maxEvaluationAttempts);
        
        // Return test cases even if evaluation failed after max attempts
        if (!evaluationResult.isAccepted()) {
            log.warn("❌ Test cases still below quality threshold after {} evaluation attempts. " +
                "Returning with quality warning. Score: {}", 
                evaluationAttempts, String.format("%.2f", evaluationResult.getQualityScore()));
        }
        
        return generatedTestCases;
    }

    /**
     * Generates test cases from requirement text.
     */
    public List<TestCase> generateTestCases(TestCaseGenerationRequest request, int maxRetries) throws Exception {
        String text = request.getText();
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Requirement text cannot be empty");
        }

        String appName = request.getContext() != null ? request.getContext().getAppName() : "Unknown";
        String module = request.getContext() != null ? request.getContext().getModule() : "General";
        String priorityHint = request.getContext() != null ? request.getContext().getPriorityHint() : "MEDIUM";

        String prompt = String.format(PROMPT_TEMPLATE, text, appName, module, priorityHint);

        String response = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Generating test cases - Attempt {}/{}", attempt, maxRetries);
                response = llmService.callLLM(prompt, 0.3f, 2000);
                log.debug("LLM raw response length: {} chars. First 300 chars: {}", response.length(), 
                    response.substring(0, Math.min(300, response.length())));
                response = extractJsonFromResponse(response);
                log.debug("Extracted JSON length: {} chars", response.length());
                break;
            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            }
        }

        if (response == null) {
            throw new RuntimeException("Failed to generate test cases after " + maxRetries + " attempts", lastException);
        }

        return parseTestCasesFromJson(response, request.getRequirementId());
    }

    /**
     * Generates test cases from requirement text with optional user guide context.
     * If user guides are specified, they will be loaded and used to enrich the prompt.
     * Supports multiple folders (userGuideFolderNames), single folder (userGuideFolderName),
     * multiple guides (userGuideFileNames), and single guide (userGuideFileName) for backward compatibility.
     * 
     * @param request TestCaseGenerationRequest with optional folder or file selection
     * @param maxRetries Maximum retry attempts
     * @return List of generated test cases
     * @throws Exception if test case generation fails
     */
    public List<TestCase> generateTestCasesWithGuide(TestCaseGenerationRequest request, int maxRetries) throws Exception {
        String text = request.getText();
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Requirement text cannot be empty");
        }

        String appName = request.getContext() != null ? request.getContext().getAppName() : "Unknown";
        String module = request.getContext() != null ? request.getContext().getModule() : "General";
        String priorityHint = request.getContext() != null ? request.getContext().getPriorityHint() : "MEDIUM";

        // Load user guides if specified (supports multiple folders, single folder, multiple guides, or single guide)
        String guideContent = "";
        
        // Check for multiple folders (highest priority)
        if (request.getUserGuideFolderNames() != null && !request.getUserGuideFolderNames().isEmpty()) {
            log.info("Loading user guides from {} folders", request.getUserGuideFolderNames().size());
            StringBuilder combinedContent = new StringBuilder();
            
            for (String folderName : request.getUserGuideFolderNames()) {
                try {
                    UserGuideContext folderContext = userGuideProcessor.loadUserGuidesFromFolder(folderName);
                    
                    if (folderContext != null && folderContext.isValid()) {
                        if (combinedContent.length() > 0) {
                            combinedContent.append("\n\n--- NEXT FOLDER ---\n\n");
                        }
                        combinedContent.append("📁 Folder: ").append(folderName).append("\n");
                        combinedContent.append(folderContext.getGuideContent());
                        log.info("Folder loaded: {}. Content length: {}", folderName, folderContext.getContentLength());
                    } else {
                        log.warn("Failed to load guides from folder: {}", folderName);
                    }
                } catch (Exception e) {
                    log.warn("Error loading folder {}: {}", folderName, e.getMessage());
                }
            }
            
            guideContent = combinedContent.toString();
        }
        // Check for single folder selection (second priority - for backward compatibility)
        else if (request.getUserGuideFolderName() != null && !request.getUserGuideFolderName().isEmpty()) {
            log.info("Loading user guides from folder: {}", request.getUserGuideFolderName());
            UserGuideContext folderContext = userGuideProcessor.loadUserGuidesFromFolder(request.getUserGuideFolderName());
            
            if (folderContext != null && folderContext.isValid()) {
                guideContent = folderContext.getGuideContent();
                log.info("Folder loaded successfully. Files: {}, Content length: {}", 
                    folderContext.getDescription(), folderContext.getContentLength());
            } else {
                log.warn("Failed to load guides from folder: {}", request.getUserGuideFolderName());
            }
        }
        // Check for multiple guides (third priority)
        else if (request.getUserGuideFileNames() != null && !request.getUserGuideFileNames().isEmpty()) {
            log.info("Loading {} user guides", request.getUserGuideFileNames().size());
            StringBuilder combinedContent = new StringBuilder();
            
            for (String fileName : request.getUserGuideFileNames()) {
                try {
                    UserGuideContext guideContext = userGuideProcessor.loadUserGuide(fileName);
                    
                    if (guideContext != null && guideContext.isValid()) {
                        if (combinedContent.length() > 0) {
                            combinedContent.append("\n\n--- NEXT GUIDE ---\n\n");
                        }
                        combinedContent.append("📘 Guide: ").append(fileName).append(" (Module: ")
                            .append(guideContext.getModule()).append(")\n");
                        combinedContent.append(guideContext.getGuideContent());
                        log.info("Guide loaded: {}. Length: {}", fileName, guideContext.getContentLength());
                    } else {
                        log.warn("Failed to load user guide: {}", fileName);
                    }
                } catch (Exception e) {
                    log.warn("Error loading guide {}: {}", fileName, e.getMessage());
                }
            }
            
            guideContent = combinedContent.toString();
        } 
        // Fall back to single guide for backward compatibility
        else if (request.getUserGuideFileName() != null && !request.getUserGuideFileName().isEmpty()) {
            log.info("Loading single user guide: {}", request.getUserGuideFileName());
            UserGuideContext guideContext = userGuideProcessor.loadUserGuide(request.getUserGuideFileName());
            
            if (guideContext != null && guideContext.isValid()) {
                guideContent = guideContext.getGuideContent();
                log.info("User guide loaded successfully. Module: {}, Content length: {}", 
                    guideContext.getModule(), guideContext.getContentLength());
            } else {
                log.warn("Failed to load user guide: {}", request.getUserGuideFileName());
            }
        }

        // Build enhanced prompt with guide context
        String promptWithGuide = buildEnhancedPrompt(text, appName, module, priorityHint, guideContent);

        String response = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Generating test cases with guide(s) - Attempt {}/{}", attempt, maxRetries);
                response = llmService.callLLM(promptWithGuide, 0.3f, 2000);
                log.debug("LLM raw response length: {} chars. First 300 chars: {}", response.length(), 
                    response.substring(0, Math.min(300, response.length())));
                response = extractJsonFromResponse(response);
                log.debug("Extracted JSON length: {} chars", response.length());
                break;
            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            }
        }

        if (response == null) {
            throw new RuntimeException("Failed to generate test cases after " + maxRetries + " attempts", lastException);
        }

        return parseTestCasesFromJson(response, request.getRequirementId());
    }

    /**
     * Builds an enhanced prompt that includes user guide context while maintaining the original instructions.
     * The PROMPT_TEMPLATE remains unchanged. Guide context is added as additional reference.
     */
    private String buildEnhancedPrompt(String requirement, String appName, String module, 
                                       String priorityHint, String guideContent) {
        String basePrompt = String.format(PROMPT_TEMPLATE, requirement, appName, module, priorityHint);
        
        if (guideContent == null || guideContent.isEmpty()) {
            return basePrompt;
        }

        // Add guide context at the end as reference material
        return basePrompt + "\n\n" +
            "===============================\n" +
            "USER GUIDE REFERENCE FOR ACCURACY:\n" +
            "===============================\n" +
            "Use the following information from the application's user guide to ensure test cases are accurate to actual UI and workflows:\n\n" +
            guideContent + "\n\n" +
            "===============================\n" +
            "END OF USER GUIDE REFERENCE\n" +
            "===============================\n\n" +
            "Note: Ensure all test steps and expected results align with the UI and workflows described in the user guide above.";
    }

    /**
     * Extracts JSON array from LLM response (handles extra text or markdown tables).
     * Tries to locate a JSON array or object first; if absent, attempts to parse a
     * markdown table into a JSON array matching the expected columns.
     */
    private String extractJsonFromResponse(String response) throws Exception {
        if (response == null) {
            throw new IllegalArgumentException("Response is null");
        }

        // Find first JSON array opening bracket
        int startArray = response.indexOf('[');
        if (startArray != -1) {
            String extracted = extractCompleteJsonArray(response, startArray);
            if (extracted != null && isValidJson(extracted)) {
                log.debug("Successfully extracted complete JSON array");
                return extracted;
            }
            
            // If extraction failed, try to fix incomplete JSON
            String fixed = fixIncompleteJson(response, startArray);
            if (fixed != null && isValidJson(fixed)) {
                log.debug("Fixed incomplete JSON array");
                return fixed;
            }
        }

        // Next attempt: find a JSON object (wrap in array if needed)
        int startObj = response.indexOf('{');
        int endObj = response.lastIndexOf('}');
        if (startObj != -1 && endObj != -1 && startObj < endObj) {
            String obj = response.substring(startObj, endObj + 1);
            if (isValidJson(obj)) {
                // Return as array to keep downstream parsing consistent
                return "[" + obj + "]";
            }
        }

        // Fallback: try to detect a markdown table and convert to JSON
        String mdTableJson = tryParseMarkdownTable(response);
        if (mdTableJson != null) return mdTableJson;

        throw new IllegalArgumentException("No valid JSON or table found in response");
    }

    /**
     * Extracts a complete JSON array from response by matching brackets.
     * Returns null if no complete array is found.
     */
    private String extractCompleteJsonArray(String response, int startArray) {
        int bracketCount = 0;
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = startArray; i < response.length(); i++) {
            char c = response.charAt(i);

            // Handle escape sequences in strings
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            // Track string boundaries
            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }

            // Only count brackets outside of strings
            if (!inString) {
                if (c == '[') bracketCount++;
                else if (c == ']') {
                    bracketCount--;
                    // Found matching closing bracket
                    if (bracketCount == 0) {
                        return response.substring(startArray, i + 1);
                    }
                } else if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
            }
        }

        // No complete array found
        return null;
    }

    /**
     * Attempts to fix incomplete JSON by finding and removing the last incomplete object.
     * Returns the fixed JSON array, or null if it cannot be fixed.
     */
    private String fixIncompleteJson(String response, int startArray) {
        log.debug("Attempting to fix incomplete JSON from position {}", startArray);
        
        // Start with the raw substring and try to close it properly
        String rawJson = response.substring(startArray);
        
        if (rawJson.isEmpty()) {
            log.debug("Empty JSON string");
            return null;
        }

        try {
            // Strategy 1: Find the last COMPLETE object by matching braces properly
            String fixedJson = findLastCompleteJsonObject(rawJson);
            if (fixedJson != null && isValidJson(fixedJson)) {
                log.debug("Fixed by extracting last complete object");
                return fixedJson;
            }

            // Strategy 2: Try truncating at the last comma before the last incomplete field
            int lastComma = rawJson.lastIndexOf(',');
            if (lastComma > 0) {
                // Look back from the last comma to find a complete object boundary
                String beforeLastComma = rawJson.substring(0, lastComma);
                int lastOpenBrace = beforeLastComma.lastIndexOf('{');
                int lastCloseBrace = beforeLastComma.lastIndexOf('}');
                
                if (lastCloseBrace > lastOpenBrace && lastCloseBrace > 0) {
                    // We have a complete object, close the array after it
                    String attempt = beforeLastComma.substring(0, lastCloseBrace + 1) + "]";
                    if (isValidJson(attempt)) {
                        log.debug("Fixed by truncating at last complete object before incomplete field");
                        return attempt;
                    }
                }
            }

            // Strategy 3: Find all closing braces and work backwards
            int lastCloseBrace = rawJson.lastIndexOf('}');
            while (lastCloseBrace > 0) {
                String attempt = rawJson.substring(0, lastCloseBrace + 1) + "]";
                if (isValidJson(attempt)) {
                    log.debug("Fixed by finding valid closing position");
                    return attempt;
                }
                lastCloseBrace = rawJson.lastIndexOf('}', lastCloseBrace - 1);
            }

        } catch (Exception e) {
            log.debug("Error attempting to fix incomplete JSON: {}", e.getMessage());
        }

        log.debug("Could not fix incomplete JSON");
        return null;
    }

    /**
     * Finds the last complete JSON object in the string by properly matching braces.
     * Handles incomplete/truncated objects by removing them.
     */
    private String findLastCompleteJsonObject(String json) {
        int braceCount = 0;
        int lastCompleteObjectEnd = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"' && !escaped) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // Found a complete object
                        lastCompleteObjectEnd = i;
                    }
                }
            }
        }

        if (lastCompleteObjectEnd > 0) {
            // We found at least one complete object, close the array after it
            String result = json.substring(0, lastCompleteObjectEnd + 1) + "]";
            return result;
        }

        return null;
    }

    /**
     * Validates if a string is valid JSON.
     */
    private boolean isValidJson(String candidate) {
        try {
            objectMapper.readTree(candidate);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Attempts to parse a markdown-style table from the response and convert it
     * to a JSON array string. Expects header row and separator line (|---|).
     * Returns null if no table-like content is found.
     */
    private String tryParseMarkdownTable(String response) {
        log.debug("Attempting to parse markdown table from response");
        
        String[] lines = response.split("\\r?\\n");
        List<String> tableLines = new ArrayList<>();
        boolean inTable = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            // Must have at least 3 pipes: |header|header| or be a separator
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.split("\\|").length >= 3) {
                tableLines.add(trimmed);
                inTable = true;
            } else if (inTable && trimmed.isEmpty()) {
                // End of table
                break;
            } else if (!inTable) {
                // Not in table yet, skip
            }
        }

        if (tableLines.size() < 3) {
            log.debug("Not enough table rows found. Found: {}", tableLines.size());
            return null; // need at least header + separator + one data row
        }

        // header = first line, separator = second line
        String headerLine = tableLines.get(0);
        String sepLine = tableLines.get(1);
        
        // Check if second line is actually a separator (contains dashes and pipes)
        if (!sepLine.matches("^\\|?\\s*:[?-]+[\\s|:?-]*\\|.*") && !sepLine.contains("---")) {
            log.debug("Line 2 does not appear to be a markdown separator: {}", sepLine);
            return null;
        }

        String[] headers = Arrays.stream(headerLine.split("\\|"))
                .map(String::trim)
                .filter(h -> !h.isEmpty())
                .toArray(String[]::new);

        if (headers.length == 0) {
            log.debug("No headers extracted from table");
            return null;
        }

        log.debug("Parsed {} headers from table: {}", headers.length, String.join(", ", headers));

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 2; i < tableLines.size(); i++) {
            String rowLine = tableLines.get(i);
            String[] cols = Arrays.stream(rowLine.split("\\|"))
                    .map(String::trim)
                    .toArray(String[]::new);

            if (cols.length < 2) {
                continue; // Skip malformed rows
            }

            // Build a map for this row using header alignment
            Map<String, String> map = new LinkedHashMap<>();
            for (int h = 0; h < headers.length && h + 1 < cols.length; h++) {
                String key = cleanMarkdownCell(headers[h]);
                String val = cleanMarkdownCell(cols[h + 1]); // +1 to skip leading empty cell from split
                map.put(key, val == null ? "" : val);
            }
            
            // Only add rows that have at least one non-empty field
            if (map.values().stream().anyMatch(v -> !v.isEmpty())) {
                rows.add(map);
            }
        }

        if (rows.isEmpty()) {
            log.debug("No valid data rows extracted from table");
            return null;
        }

        try {
            String jsonResult = objectMapper.writeValueAsString(rows);
            log.debug("Successfully converted markdown table to JSON: {} rows", rows.size());
            return jsonResult;
        } catch (Exception e) {
            log.warn("Failed to convert parsed table to JSON", e);
            return null;
        }
    }

    /**
     * Parses test cases from JSON response.
     */
    private List<TestCase> parseTestCasesFromJson(String jsonString, String requirementId) throws Exception {
        List<TestCase> testCases = new ArrayList<>();

        try {
            log.debug("Parsing JSON response: {}", jsonString.substring(0, Math.min(200, jsonString.length())));
            JsonNode rootNode = objectMapper.readTree(jsonString);

            if (!rootNode.isArray()) {
                throw new IllegalArgumentException("Response is not a JSON array. Type: " + rootNode.getNodeType());
            }

            log.info("Found {} test case entries in JSON", rootNode.size());
            
            for (int i = 0; i < rootNode.size(); i++) {
                try {
                    JsonNode node = rootNode.get(i);
                    TestCase testCase = mapJsonToTestCase(node, requirementId);
                    testCases.add(testCase);
                } catch (Exception e) {
                    log.warn("Error parsing test case at index {}: {}", i, e.getMessage());
                    // Continue processing other test cases even if one fails
                }
            }

            if (testCases.isEmpty()) {
                throw new RuntimeException("No test cases could be parsed from the JSON response");
            }

            log.info("Successfully parsed {} test cases from JSON response", testCases.size());
        } catch (Exception e) {
            log.error("Error parsing test cases from JSON: {} | First 200 chars: {}", 
                e.getClass().getSimpleName(), 
                jsonString.substring(0, Math.min(200, jsonString.length())), 
                e);
            throw new RuntimeException("Failed to parse test cases: " + e.getMessage(), e);
        }

        return testCases;
    }

    /**
     * Maps a JSON node to a TestCase entity.
     * Handles both direct JSON format and markdown table converted format.
     */
    private TestCase mapJsonToTestCase(JsonNode node, String requirementId) {
        log.debug("Mapping JSON node to TestCase. Node keys: {}", String.join(", ", 
            (Iterable<String>) () -> node.fieldNames()));
        
        // Handle both direct field names and markdown table field names with case-insensitive lookup
        String title = findFieldValue(node, "title", "Test Case Title", "test case title", "**Test Case Title**");
        title = cleanMarkdownCell(title);
        if (title == null || title.isEmpty() || title.equalsIgnoreCase("Untitled Test Case")) {
            title = "Untitled Test Case";
        }
        
        String description = cleanMarkdownCell(findFieldValue(node, "description", "Description", ""));
        
        String explicitPreconditions = cleanMarkdownCell(findFieldValue(node, "preconditions", "Preconditions", "Pre-conditions", "Pre Conditions"));
        
        double complexity = getDoubleField(node, "estimatedComplexity", 0.5);

        // Parse steps from "steps" field (array format) or "Test Steps" field (string format)
        List<Step> steps = new ArrayList<>();
        JsonNode stepsNode = node.get("steps");
        String testStepsStr = "";
        String expectedResultStr = "";
        
        if (stepsNode != null && stepsNode.isArray()) {
            log.debug("Found steps array with {} items", stepsNode.size());
            for (int i = 0; i < stepsNode.size(); i++) {
                JsonNode stepNode = stepsNode.get(i);
                Step step = Step.builder()
                    .stepNumber(i + 1)
                    .action(cleanMarkdownCell(getStringField(stepNode, "action", "")))
                    .testData(cleanMarkdownCell(getStringField(stepNode, "testData", "")))
                    .expectedResult(cleanMarkdownCell(getStringField(stepNode, "expectedResult", "")))
                    .build();
                if (step.getExpectedResult() == null || step.getExpectedResult().isBlank()) {
                    step.setExpectedResult(defaultExpectedResultForStep(step.getAction()));
                }
                steps.add(step);
            }
        } else {
            // Fall back to string parsing for markdown format
            testStepsStr = cleanMarkdownCell(findFieldValue(node, "steps", "Test Steps", "action", "**Test Steps**"));
            if (!testStepsStr.isEmpty()) {
                expectedResultStr = cleanMarkdownCell(findFieldValue(node, "expectedResult", "Expected Result", "expected result", "**Expected Result**"));
                steps = parseStepsFromString(testStepsStr, expectedResultStr);
            }
        }
        
        log.debug("Extracted fields - Title: {}, Steps: {} items found", title, steps.size());
        
        String preconditions = !explicitPreconditions.isEmpty() ? explicitPreconditions : extractPreconditions(testStepsStr);

        // Parse expected results from expectedResults array if present
        List<String> expectedResults = new ArrayList<>();
        JsonNode resultsNode = node.get("expectedResults");
        if (resultsNode != null && resultsNode.isArray()) {
            log.debug("Found expectedResults array with {} items", resultsNode.size());
            for (JsonNode result : resultsNode) {
                expectedResults.add(result.asText());
            }
        } else if (!expectedResultStr.isEmpty()) {
            // Split by common delimiters from string format
            String[] results = expectedResultStr.split("\\n|,(?!\\s*\\w+:)");
            for (String result : results) {
                String trimmed = result.trim();
                if (!trimmed.isEmpty()) {
                    expectedResults.add(trimmed);
                }
            }
        }

        // Parse tags from "tags" or "Labels" field
        List<String> tags = new ArrayList<>();
        String labels = cleanMarkdownCell(findFieldValue(node, "labels", "Labels", "Tags", "tags", "**Labels**"));
        if (!labels.isEmpty() && !labels.equals("-")) {
            String[] labelArray = labels.split(",");
            for (String label : labelArray) {
                String trimmed = label.trim();
                if (!trimmed.isEmpty()) {
                    tags.add(trimmed);
                }
            }
        }
        
        JsonNode tagsNode = node.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                tags.add(tag.asText());
            }
        }

        // Parse priority (try multiple field names)
        String priorityStr = cleanMarkdownCell(findFieldValue(node, "priority", "Priority", "MEDIUM"));
        if (priorityStr.isEmpty()) {
            priorityStr = "MEDIUM";
        }
        TestCase.Priority priority = TestCase.Priority.MEDIUM;
        try {
            priority = TestCase.Priority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            priority = TestCase.Priority.MEDIUM;
        }

        // Add preconditions as the first step if they exist
        if (preconditions != null && !preconditions.isEmpty() && !startsWithPrecondition(steps)) {
            if (steps == null) {
                steps = new ArrayList<>();
            }
            // Insert precondition as first step
            Step preconditionStep = Step.builder()
                .stepNumber(1)
                .action(preconditions)
                .testData("")
                .expectedResult(defaultExpectedResultForStep(preconditions))
                .build();
            steps.add(0, preconditionStep);
            
            // Re-number all steps
            for (int i = 0; i < steps.size(); i++) {
                steps.get(i).setStepNumber(i + 1);
            }
        }

        for (Step step : steps) {
            if (step.getExpectedResult() == null || step.getExpectedResult().isBlank()) {
                step.setExpectedResult(defaultExpectedResultForStep(step.getAction()));
            }
        }

        if (expectedResults.isEmpty()) {
            expectedResults = steps.stream()
                .map(Step::getExpectedResult)
                .filter(result -> result != null && !result.isBlank())
                .collect(Collectors.toList());
        }

        return TestCase.builder()
            .title(title)
            .description(description)
            .preconditions("")  // Clear preconditions - they're now in steps
            .steps(steps)
            .expectedResults(expectedResults)
            .priority(priority)
            .tags(tags)
            .requirementId(requirementId)
            .estimatedComplexity(complexity)
            .build();
    }

    /**
     * Extracts preconditions from test steps string if present.
     */
    private String extractPreconditions(String testStepsStr) {
        if (testStepsStr == null || testStepsStr.isEmpty()) {
            return "";
        }
        
        // Look for "Pre-condition:" or "Precondition:" at the beginning
        if (testStepsStr.toLowerCase().contains("pre-condition:")) {
            int startIdx = testStepsStr.toLowerCase().indexOf("pre-condition:");
            int endIdx = testStepsStr.indexOf("\n", startIdx);
            if (endIdx == -1) {
                endIdx = testStepsStr.indexOf(",", startIdx);
            }
            if (endIdx == -1) {
                return testStepsStr.substring(startIdx).trim();
            }
            return testStepsStr.substring(startIdx, endIdx).trim();
        }
        return "";
    }

    /**
     * Parses test steps from a formatted string.
     */
    private List<Step> parseStepsFromString(String testStepsStr, String expectedResultStr) {
        List<Step> steps = new ArrayList<>();
        if (testStepsStr == null || testStepsStr.isEmpty()) {
            return steps;
        }

        // Split steps by "Step X:" pattern or by line breaks
        String[] stepArray = testStepsStr.split("(?=Step\\s+\\d+:|\\d+\\.|\\n)");
        
        int stepNum = 1;
        for (String stepText : stepArray) {
            String trimmed = stepText.trim();
            if (!trimmed.isEmpty() && !trimmed.toLowerCase().startsWith("pre-condition")) {
                Step step = Step.builder()
                    .stepNumber(stepNum++)
                    .action(trimmed)
                    .testData("")
                    .expectedResult(!expectedResultStr.isBlank() ? expectedResultStr : defaultExpectedResultForStep(trimmed))
                    .build();
                steps.add(step);
            }
        }

        // If no steps were parsed, create one with the full text
        if (steps.isEmpty() && !testStepsStr.isEmpty()) {
            steps.add(Step.builder()
                .stepNumber(1)
                .action(testStepsStr)
                .testData("")
                .expectedResult(!expectedResultStr.isBlank() ? expectedResultStr : defaultExpectedResultForStep(testStepsStr))
                .build());
        }

        return steps;
    }

    private boolean startsWithPrecondition(List<Step> steps) {
        return steps != null
            && !steps.isEmpty()
            && steps.get(0).getAction() != null
            && steps.get(0).getAction().toLowerCase(Locale.ROOT).contains("pre-condition");
    }

    private String defaultExpectedResultForStep(String action) {
        if (action != null && action.toLowerCase(Locale.ROOT).contains("pre-condition")) {
            return "System should allow the test to start only when the pre-condition is met";
        }
        return "System should complete the step successfully and display the expected behavior";
    }

    private String getStringField(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : defaultValue;
    }

    private double getDoubleField(JsonNode node, String fieldName, double defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asDouble() : defaultValue;
    }

    /**
     * Finds a field value using case-insensitive field name matching.
     * Tries multiple field name variants.
     */
    private String findFieldValue(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return "";
        }

        // First try exact matches
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && !field.isNull()) {
                String value = field.asText();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }

        // Then try case-insensitive matches on all fields
        Iterator<String> fieldIterator = node.fieldNames();
        while (fieldIterator.hasNext()) {
            String nodeName = fieldIterator.next();
            for (String fieldName : fieldNames) {
                if (nodeName.equalsIgnoreCase(fieldName)) {
                    JsonNode field = node.get(nodeName);
                    if (field != null && !field.isNull()) {
                        String value = field.asText();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }

                if (normalizeFieldName(nodeName).equals(normalizeFieldName(fieldName))) {
                    JsonNode field = node.get(nodeName);
                    if (field != null && !field.isNull()) {
                        String value = field.asText();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            }
        }

        return "";
    }

    private String normalizeFieldName(String fieldName) {
        if (fieldName == null) {
            return "";
        }
        return fieldName.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String cleanMarkdownCell(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
            .replaceAll("`(.*?)`", "$1")
            .trim();
    }

    /**
     * Builds a quality assessment report with evaluation details and save prompt
     */
    public QualityAssessmentReport buildQualityAssessmentReport(List<TestCase> testCases, 
                                                                double qualityScore) {
        return QualityAssessmentReport.builder()
            .testCaseCount(testCases.size())
            .qualityScore(qualityScore)
            .qualityStatus(getQualityStatus(qualityScore))
            .assessmentMessage(buildQualityAssessmentMessage(qualityScore))
            .savePrompt(buildSaveToLibraryPrompt(testCases.size(), qualityScore))
            .testCases(testCases)
            .build();
    }

    /**
     * Gets quality status based on score
     */
    private String getQualityStatus(double qualityScore) {
        if (qualityScore >= 0.85) {
            return "EXCELLENT";
        } else if (qualityScore >= 0.75) {
            return "GOOD";
        } else if (qualityScore >= 0.60) {
            return "ACCEPTABLE";
        } else {
            return "NEEDS_IMPROVEMENT";
        }
    }

    /**
     * Builds quality assessment message for user display
     */
    private String buildQualityAssessmentMessage(double qualityScore) {
        StringBuilder message = new StringBuilder();
        message.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append("✓ Quality Assessment: ").append(getQualityStatus(qualityScore)).append("\n");
        message.append("Score: ").append(String.format("%.0f", qualityScore * 100)).append("%\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        if (qualityScore >= 0.75) {
            message.append("✅ Test cases passed quality evaluation!\n");
            message.append("These test cases are ready for use in your library.\n");
        } else {
            message.append("⚠️ Test cases below optimal quality threshold.\n");
            message.append("Consider regenerating or editing before saving.\n");
        }
        
        return message.toString();
    }

    /**
     * Build save to library prompt for user
     */
    public String buildSaveToLibraryPrompt(int testCaseCount, double qualityScore) {
        return "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "💾 SAVE TO LIBRARY?\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "These " + testCaseCount + " test cases have quality score " + 
            String.format("%.0f", qualityScore * 100) + "%.\n" +
            "Would you like to save them to your personal library for future reuse?\n" +
            "\nAction options:\n" +
            "├─ [Save to Library] - Save with title, description, category, priority, and tags\n" +
            "├─ [Skip] - Keep only in this conversation\n" +
            "└─ Regular actions: [Regenerate] [Modify] [Add More] [Merge]\n";
    }

    /**
     * Builds a formatted display of test cases for the user
     */
    public String buildTestCaseDisplayMessage(List<TestCase> testCases) {
        StringBuilder display = new StringBuilder();
        
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            display.append("\n[Test Case ").append(i + 1).append(": ").append(tc.getTitle()).append("]\n");
            display.append("  Priority: ").append(tc.getPriority()).append("\n");
            display.append("  Complexity: ").append(String.format("%.1f", tc.getEstimatedComplexity())).append("\n");
            display.append("  Steps: ").append(tc.getSteps().size()).append("\n");
            
            if (tc.getTags() != null && !tc.getTags().isEmpty()) {
                display.append("  Tags: ").append(String.join(", ", tc.getTags())).append("\n");
            }
            
            if (i < testCases.size() - 1) {
                display.append("\n" + "─".repeat(50) + "\n");
            }
        }
        
        return display.toString();
    }

    /**
     * Data class for quality assessment report
     */
    @lombok.Data
    @lombok.Builder
    public static class QualityAssessmentReport {
        private int testCaseCount;
        private double qualityScore;
        private String qualityStatus;
        private String assessmentMessage;
        private String savePrompt;
        private List<TestCase> testCases;
    }
}
