package com.qaautomation.services;

import com.qaautomation.agents.AutomationCodeGenerationAgent;
import com.qaautomation.models.*;
import com.qaautomation.repositories.CodeArtifactRepository;
import com.qaautomation.repositories.TestCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for automation code generation operations.
 */
@Slf4j
@Service
public class CodeGenerationService {

    private final CodeArtifactRepository codeArtifactRepository;
    private final TestCaseRepository testCaseRepository;
    private final AutomationCodeGenerationAgent automationCodeGenerationAgent;

    public CodeGenerationService(
        CodeArtifactRepository codeArtifactRepository,
        TestCaseRepository testCaseRepository,
        AutomationCodeGenerationAgent automationCodeGenerationAgent) {
        this.codeArtifactRepository = codeArtifactRepository;
        this.testCaseRepository = testCaseRepository;
        this.automationCodeGenerationAgent = automationCodeGenerationAgent;
    }

    /**
     * Generates automation code from scenario.
     */
    public CodeArtifact generateCode(CodeGenerationRequest request) throws Exception {
        log.info("Generating code for scenario using {} and {}", request.getFramework(), request.getLanguage());

        CodeArtifact artifact = automationCodeGenerationAgent.generateCode(request, 3);

        // Validate code based on language
        if (request.getLanguage() == CodeArtifact.Language.JAVA) {
            boolean isValid = automationCodeGenerationAgent.validateJavaCode(
                artifact.getFiles() != null && !artifact.getFiles().isEmpty() ?
                    artifact.getFiles().get(0).getContent() : ""
            );
            log.info("Java code validation: {}", isValid ? "PASSED" : "FAILED");
        }

        // Save artifact
        artifact = codeArtifactRepository.save(artifact);

        log.info("Generated code artifact with {} files", artifact.getFiles().size());
        return artifact;
    }

    /**
     * Generates automation code from existing test cases.
     */
    public CodeArtifact generateCodeFromTestCases(GenerateCodeFromTestCasesRequest request) throws Exception {
        log.info("Generating code from test cases - IDs: {}, Framework: {}, Language: {}",
            request.getTestCaseIds(), request.getFramework(), request.getLanguage());

        // Fetch test cases from database
        List<TestCase> testCases = testCaseRepository.findAllById(request.getTestCaseIds())
            .stream()
            .collect(Collectors.toList());

        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("No test cases found with provided IDs");
        }

        log.info("Retrieved {} test cases from database", testCases.size());

        // Convert test cases to scenario text
        String scenarioText = buildScenarioFromTestCases(testCases, request.getFramework(), request.getLanguage());

        // Create code generation request from test cases
        CodeGenerationRequest codeGenRequest = CodeGenerationRequest.builder()
            .scenarioText(scenarioText)
            .framework(request.getFramework())
            .language(request.getLanguage())
            .outputType("production")
            .options(CodeGenerationRequest.CodeGenerationOptions.builder()
                .usePageObjectModel(request.getUsePageObjectModel())
                .includeSetupTeardown(request.getIncludeSetupTeardown())
                .build())
            .build();

        // Generate code using agent
        CodeArtifact artifact = automationCodeGenerationAgent.generateCode(codeGenRequest, 3);

        // Save artifact
        artifact = codeArtifactRepository.save(artifact);

        log.info("Generated code artifact from {} test cases with {} files",
            testCases.size(), artifact.getFiles().size());
        return artifact;
    }

    /**
     * Builds a readable scenario description from test cases for LLM processing.
     */
    private String buildScenarioFromTestCases(List<TestCase> testCases, CodeArtifact.Framework framework, CodeArtifact.Language language) {
        StringBuilder sb = new StringBuilder();

        sb.append("Generate automation test code for the following test cases:\n");
        sb.append("Framework: ").append(framework).append("\n");
        sb.append("Language: ").append(language).append("\n");
        sb.append("==========================================\n\n");

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            sb.append("TEST CASE ").append(i + 1).append(": ").append(tc.getTitle()).append("\n");
            sb.append("Description: ").append(tc.getDescription() != null ? tc.getDescription() : "N/A").append("\n");
            sb.append("Priority: ").append(tc.getPriority() != null ? tc.getPriority() : "MEDIUM").append("\n");

            // Add preconditions
            if (tc.getPreconditions() != null && !tc.getPreconditions().isEmpty()) {
                sb.append("Preconditions:\n");
                sb.append("  - ").append(tc.getPreconditions()).append("\n");
            }

            // Add test steps
            if (tc.getSteps() != null && !tc.getSteps().isEmpty()) {
                sb.append("Test Steps:\n");
                int stepNum = 1;
                for (Step step : tc.getSteps()) {
                    sb.append("  Step ").append(stepNum).append(": ").append(step.getAction());
                    if (step.getTestData() != null && !step.getTestData().isEmpty()) {
                        sb.append(" [Data: ").append(step.getTestData()).append("]");
                    }
                    sb.append("\n");

                    if (step.getExpectedResult() != null && !step.getExpectedResult().isEmpty()) {
                        sb.append("    Expected: ").append(step.getExpectedResult()).append("\n");
                    }
                    stepNum++;
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Retrieves code artifact by ID.
     */
    public CodeArtifact getCodeArtifact(String artifactId) {
        return codeArtifactRepository.findById(artifactId).orElse(null);
    }
}
