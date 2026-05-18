package com.qaautomation.controllers;

import com.qaautomation.models.CodeArtifact;
import com.qaautomation.models.CodeGenerationRequest;
import com.qaautomation.models.CodeGenerationResponse;
import com.qaautomation.models.GenerateCodeFromTestCasesRequest;
import com.qaautomation.services.CodeGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for automation code generation endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/agents/code")
@CrossOrigin(origins = "*")
public class CodeGenerationController {

    private final CodeGenerationService codeGenerationService;

    public CodeGenerationController(CodeGenerationService codeGenerationService) {
        this.codeGenerationService = codeGenerationService;
    }

    /**
     * POST /agents/code/from-testcases - Generates automation code from existing test cases.
     */
    @PostMapping("/from-testcases")
    public ResponseEntity<?> generateCodeFromTestCases(@RequestBody GenerateCodeFromTestCasesRequest request) {
        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Received code generation request from test cases: {} - Test Case IDs: {}, Framework: {}, Language: {}",
                requestId, request.getTestCaseIds(), request.getFramework(), request.getLanguage());

            if (request.getTestCaseIds() == null || request.getTestCaseIds().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("At least one test case must be selected"));
            }

            if (request.getFramework() == null || request.getLanguage() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Framework and Language must be specified"));
            }

            CodeArtifact artifact = codeGenerationService.generateCodeFromTestCases(request);

            var artifactDto = CodeGenerationResponse.CodeArtifactDto.builder()
                .code(artifact.getCode())
                .files(artifact.getFiles().stream()
                    .map(f -> new CodeGenerationResponse.CodeArtifactDto.CodeFile(f.getFilePath(), f.getContent()))
                    .collect(Collectors.toList()))
                .requiredDependencies(artifact.getDependencies())
                .confidence(artifact.getConfidence())
                .requiresHumanReview(artifact.getRequiresHumanReview())
                .notes(artifact.getNotes())
                .build();

            CodeGenerationResponse response = CodeGenerationResponse.builder()
                .requestId(requestId)
                .status("SUCCESS")
                .artifact(artifactDto)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

            log.info("Successfully generated code from test cases for request {}", requestId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating code from test cases for request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to generate code: " + e.getMessage()));
        }
    }

    /**
     * POST /agents/code - Generates automation code from scenario.
     */
    @PostMapping
    public ResponseEntity<?> generateCode(@RequestBody CodeGenerationRequest request) {
        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Received code generation request: {} for {} {}", requestId, request.getFramework(), request.getLanguage());

            if (request.getScenarioText() == null || request.getScenarioText().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Scenario text cannot be empty"));
            }

            CodeArtifact artifact = codeGenerationService.generateCode(request);

            var artifactDto = CodeGenerationResponse.CodeArtifactDto.builder()
                .code(artifact.getCode())
                .files(artifact.getFiles().stream()
                    .map(f -> new CodeGenerationResponse.CodeArtifactDto.CodeFile(f.getFilePath(), f.getContent()))
                    .collect(Collectors.toList()))
                .requiredDependencies(artifact.getDependencies())
                .confidence(artifact.getConfidence())
                .requiresHumanReview(artifact.getRequiresHumanReview())
                .notes(artifact.getNotes())
                .build();

            CodeGenerationResponse response = CodeGenerationResponse.builder()
                .requestId(requestId)
                .status("SUCCESS")
                .artifact(artifactDto)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

            log.info("Successfully generated code for request {}", requestId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating code for request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to generate code: " + e.getMessage()));
        }
    }

    /**
     * GET /agents/code/{artifactId} - Retrieves generated code artifact.
     */
    @GetMapping("/{artifactId}")
    public ResponseEntity<?> getCodeArtifact(@PathVariable String artifactId) {
        try {
            CodeArtifact artifact = codeGenerationService.getCodeArtifact(artifactId);

            if (artifact == null) {
                return ResponseEntity.notFound().build();
            }

            var artifactDto = CodeGenerationResponse.CodeArtifactDto.builder()
                .code(artifact.getCode())
                .files(artifact.getFiles().stream()
                    .map(f -> new CodeGenerationResponse.CodeArtifactDto.CodeFile(f.getFilePath(), f.getContent()))
                    .collect(Collectors.toList()))
                .requiredDependencies(artifact.getDependencies())
                .confidence(artifact.getConfidence())
                .requiresHumanReview(artifact.getRequiresHumanReview())
                .notes(artifact.getNotes())
                .build();

            return ResponseEntity.ok(artifactDto);

        } catch (Exception e) {
            log.error("Error retrieving code artifact {}: {}", artifactId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve code artifact"));
        }
    }

    /**
     * Error response DTO.
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
