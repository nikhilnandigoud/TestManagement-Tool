package com.qaautomation.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaautomation.models.CodeArtifact;
import com.qaautomation.models.CodeGenerationRequest;
import com.qaautomation.services.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Automation Code Generation Agent - Converts test scenarios into runnable automation scripts.
 * Responsibilities:
 * - Accept test scenario text
 * - Generate code for specified framework/language
 * - Support Page Object Model pattern
 * - Provide quality checks and confidence scores
 * - Return artifacts with setup/teardown and assertions
 */
@Slf4j
@Component
public class AutomationCodeGenerationAgent implements Agent {

    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> FRAMEWORK_TEMPLATES = Map.ofEntries(
        Map.entry("SELENIUM_JAVA", "Selenium with Java using TestNG"),
        Map.entry("PLAYWRIGHT_JS", "Playwright with JavaScript"),
        Map.entry("PLAYWRIGHT_TS", "Playwright with TypeScript"),
        Map.entry("CYPRESS", "Cypress with JavaScript")
    );

    private static final String PROMPT_TEMPLATE = """
        You are an expert automation engineer. Generate a test automation script for the following scenario.
        
        SCENARIO:
        %s
        
        FRAMEWORK: %s
        LANGUAGE: %s
        OUTPUT TYPE: %s
        USE PAGE OBJECT MODEL: %s
        INCLUDE SETUP/TEARDOWN: %s
        
        INSTRUCTIONS:
        1. Generate well-structured, production-ready code
        2. Include appropriate imports and dependencies
        3. Add comments explaining key sections
        4. Include error handling
        5. If using Page Object Model, create separate page objects
        6. Return a JSON object with this structure:
        {
          "files": [
            {"path": "src/test/java/tests/MyTest.java", "content": "...code..."},
            {"path": "src/test/java/pages/BasePage.java", "content": "...code..."}
          ],
          "dependencies": ["org.testng:testng:7.7.0"],
          "notes": "Setup instructions or important notes",
          "confidence": 0.85
        }
        
        Return ONLY valid JSON, no additional text.
        """;

    public AutomationCodeGenerationAgent(LLMService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getDescription() {
        return "Generates automation code scripts for Selenium, Playwright, Cypress, and other frameworks";
    }

    @Override
    public boolean canHandle(Object input) {
        return input instanceof CodeGenerationRequest;
    }

    @Override
    public boolean execute() {
        // Implementation handled by service layer
        return true;
    }

    /**
     * Generates automation code from a scenario.
     */
    public CodeArtifact generateCode(CodeGenerationRequest request, int maxRetries) throws Exception {
        String scenarioText = request.getScenarioText();
        if (scenarioText == null || scenarioText.isEmpty()) {
            throw new IllegalArgumentException("Scenario text cannot be empty");
        }

        CodeGenerationRequest.CodeGenerationOptions options = request.getOptions() != null ?
            request.getOptions() : CodeGenerationRequest.CodeGenerationOptions.builder().build();

        String frameworkLabel = getFrameworkLabel(request.getFramework(), request.getLanguage());
        String prompt = String.format(PROMPT_TEMPLATE,
            scenarioText,
            frameworkLabel,
            request.getLanguage(),
            request.getOutputType() != null ? request.getOutputType() : "skeleton",
            options.getUsePageObjectModel() != null ? options.getUsePageObjectModel() : true,
            options.getIncludeSetupTeardown() != null ? options.getIncludeSetupTeardown() : true
        );

        String response = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Generating code - Attempt {}/{}", attempt, maxRetries);
                response = llmService.callLLM(prompt, 0.3f, 3000);
                response = extractJsonFromResponse(response);
                break;
            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(1000 * attempt);
                }
            }
        }

        if (response == null) {
            throw new RuntimeException("Failed to generate code after " + maxRetries + " attempts", lastException);
        }

        return parseCodeArtifactFromJson(response, request);
    }

    /**
     * Gets a descriptive label for the framework/language combination.
     */
    private String getFrameworkLabel(CodeArtifact.Framework framework, CodeArtifact.Language language) {
        return String.format("%s with %s", framework.name(), language.name());
    }

    /**
     * Extracts JSON object from LLM response.
     */
    private String extractJsonFromResponse(String response) {
        if (response == null) {
            throw new IllegalArgumentException("Response is null");
        }

        int startIdx = response.indexOf('{');
        int endIdx = response.lastIndexOf('}');

        if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
            throw new IllegalArgumentException("No valid JSON object found in response");
        }

        return response.substring(startIdx, endIdx + 1);
    }

    /**
     * Parses code artifact from JSON response.
     */
    private CodeArtifact parseCodeArtifactFromJson(String jsonString, CodeGenerationRequest request) throws Exception {
        try {
            var rootNode = objectMapper.readTree(jsonString);

            List<CodeArtifact.CodeFile> files = new ArrayList<>();
            StringBuilder combinedCode = new StringBuilder();
            var filesNode = rootNode.get("files");
            if (filesNode != null && filesNode.isArray()) {
                for (var fileNode : filesNode) {
                    String path = fileNode.get("path").asText();
                    String content = fileNode.get("content").asText();
                    CodeArtifact.CodeFile codeFile = new CodeArtifact.CodeFile();
                    codeFile.setFilePath(path);
                    codeFile.setContent(content);
                    files.add(codeFile);
                    
                    // Accumulate code for the code property
                    if (combinedCode.length() > 0) {
                        combinedCode.append("\n\n// ========== FILE: ").append(path).append(" ==========\n\n");
                    }
                    combinedCode.append(content);
                }
            }

            // Use top-level code property if available, otherwise use combined code from files
            String code = rootNode.has("code") ? rootNode.get("code").asText() : 
                         (combinedCode.length() > 0 ? combinedCode.toString() : null);

            List<String> dependencies = new ArrayList<>();
            var depsNode = rootNode.get("dependencies");
            if (depsNode != null && depsNode.isArray()) {
                for (var depNode : depsNode) {
                    dependencies.add(depNode.asText());
                }
            }

            String notes = rootNode.has("notes") ? rootNode.get("notes").asText() : "";
            double confidence = rootNode.has("confidence") ? rootNode.get("confidence").asDouble() : 0.7;

            return CodeArtifact.builder()
                .framework(request.getFramework())
                .language(request.getLanguage())
                .code(code)
                .files(files)
                .dependencies(dependencies)
                .notes(notes)
                .confidence(confidence)
                .requiresHumanReview(confidence < 0.8)
                .build();

        } catch (Exception e) {
            log.error("Error parsing code artifact from JSON: {}", jsonString, e);
            throw e;
        }
    }

    /**
     * Validates generated Java code by simulating compilation checks.
     */
    public boolean validateJavaCode(String code) {
        // Basic validation checks
        boolean hasMissingImports = checkMissingImports(code);
        boolean hasSyntaxErrors = checkSyntaxErrors(code);

        return !hasMissingImports && !hasSyntaxErrors;
    }

    /**
     * Performs basic linting simulation for JavaScript/TypeScript.
     */
    public List<String> lintJavaScriptCode(String code) {
        List<String> issues = new ArrayList<>();

        // Check for common issues
        if (code.contains("var ")) {
            issues.add("Use 'const' or 'let' instead of 'var'");
        }
        if (!code.contains("'use strict'") && !code.contains("\"use strict\"")) {
            issues.add("Consider adding 'use strict' directive");
        }
        if (code.contains("console.log")) {
            issues.add("Remove console.log statements or use proper logging");
        }

        return issues;
    }

    private boolean checkMissingImports(String code) {
        // Simplified check - in real implementation, would parse AST
        return false;
    }

    private boolean checkSyntaxErrors(String code) {
        // Simplified check - in real implementation, would use JavaParser
        return false;
    }
}
