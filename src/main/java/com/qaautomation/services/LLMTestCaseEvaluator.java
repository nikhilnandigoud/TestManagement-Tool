package com.qaautomation.services;

import com.qaautomation.models.TestCase;
import com.qaautomation.models.Step;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM Test Case Evaluator - Part of Evaluator-Optimizer pattern
 * 
 * Validates quality of generated test cases using LLM evaluation
 * Provides feedback for improvement if quality threshold not met
 */
@Slf4j
@Service
public class LLMTestCaseEvaluator {
    
    @Autowired
    private LLMService llmService;
    
    /**
     * Evaluate quality of generated test cases
     * Returns score between 0.0 and 1.0
     */
    public EvaluationResult evaluateTestCases(List<TestCase> testCases, String originalRequirement) {
        try {
            if (testCases == null || testCases.isEmpty()) {
                log.warn("No test cases to evaluate");
                return new EvaluationResult(0.0, "No test cases generated", "Generate test cases first");
            }
            
            // Build evaluation prompt
            String evaluationPrompt = buildEvaluationPrompt(testCases, originalRequirement);
            
            // Call LLM for evaluation
            String evaluationResponse = llmService.callLLM(evaluationPrompt, 0.3f, 1500);
            log.debug("Evaluation response length: {} chars", evaluationResponse.length());
            
            // Parse evaluation response
            return parseEvaluationResponse(evaluationResponse, testCases);
            
        } catch (Exception e) {
            log.error("Error evaluating test cases: {}", e.getMessage());
            return new EvaluationResult(0.5, "Evaluation error", e.getMessage());
        }
    }
    
    /**
     * Build evaluation prompt for LLM
     */
    private String buildEvaluationPrompt(List<TestCase> testCases, String originalRequirement) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert QA test case evaluator.\n\n");
        prompt.append("ORIGINAL REQUIREMENT:\n")
              .append(originalRequirement).append("\n\n");
        
        prompt.append("GENERATED TEST CASES:\n");
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            prompt.append("\nTest Case ").append(i + 1).append(": ").append(tc.getTitle()).append("\n");
            
            if (tc.getSteps() != null && !tc.getSteps().isEmpty()) {
                prompt.append("Steps:\n");
                for (Step step : tc.getSteps()) {
                    prompt.append("  ").append(step.getStepNumber()).append(". ").append(step.getAction()).append("\n");
                }
            }
            
            if (tc.getExpectedResults() != null && !tc.getExpectedResults().isEmpty()) {
                prompt.append("Expected Results:\n");
                for (String result : tc.getExpectedResults()) {
                    prompt.append("  - ").append(result).append("\n");
                }
            }
        }
        
        prompt.append("\n\nEVALUATION CRITERIA:\n");
        prompt.append("1. STRUCTURE (25%): Each test case has clear title, steps, and expected results\n");
        prompt.append("2. CLARITY (25%): Steps are specific, actionable, and unambiguous\n");
        prompt.append("3. COVERAGE (25%): Test cases cover happy path, edge cases, and error scenarios\n");
        prompt.append("4. COMPLETENESS (15%): All steps logically lead to the expected result\n");
        prompt.append("5. REQUIREMENT MATCH (10%): Test cases align with original requirement\n\n");
        
        prompt.append("Evaluate the test cases and provide:\n");
        prompt.append("1. Overall quality score (0-100)\n");
        prompt.append("2. Score for each criterion (0-100)\n");
        prompt.append("3. Strengths (what's good)\n");
        prompt.append("4. Weaknesses (what needs improvement)\n");
        prompt.append("5. Specific feedback for improvement\n");
        prompt.append("6. Decision: ACCEPTED or REJECTED\n\n");
        
        prompt.append("Format your response as:\n");
        prompt.append("OVERALL_SCORE: [number]\n");
        prompt.append("STRUCTURE_SCORE: [number]\n");
        prompt.append("CLARITY_SCORE: [number]\n");
        prompt.append("COVERAGE_SCORE: [number]\n");
        prompt.append("COMPLETENESS_SCORE: [number]\n");
        prompt.append("REQUIREMENT_MATCH_SCORE: [number]\n");
        prompt.append("STRENGTHS: [describe]\n");
        prompt.append("WEAKNESSES: [describe]\n");
        prompt.append("FEEDBACK: [specific improvements needed]\n");
        prompt.append("DECISION: [ACCEPTED or REJECTED]\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse evaluation response from LLM
     */
    private EvaluationResult parseEvaluationResponse(String response, List<TestCase> testCases) {
        try {
            // Extract scores using regex
            double overallScore = extractScore(response, "OVERALL_SCORE:");
            double structureScore = extractScore(response, "STRUCTURE_SCORE:");
            double clarityScore = extractScore(response, "CLARITY_SCORE:");
            double coverageScore = extractScore(response, "COVERAGE_SCORE:");
            double completenessScore = extractScore(response, "COMPLETENESS_SCORE:");
            double requirementMatchScore = extractScore(response, "REQUIREMENT_MATCH_SCORE:");
            
            // Normalize to 0-1 range
            double normalizedScore = overallScore / 100.0;
            
            // Extract text sections
            String strengths = extractSection(response, "STRENGTHS:", "WEAKNESSES:");
            String weaknesses = extractSection(response, "WEAKNESSES:", "FEEDBACK:");
            String feedback = extractSection(response, "FEEDBACK:", "DECISION:");
            String decision = extractSection(response, "DECISION:", null);
            
            // Determine if accepted
            boolean isAccepted = decision.toUpperCase().contains("ACCEPTED");
            
            log.info("Test Case Evaluation - Overall: {}, Structure: {}, Clarity: {}, Coverage: {}, Completeness: {}, Match: {}",
                overallScore, structureScore, clarityScore, coverageScore, completenessScore, requirementMatchScore);
            
            String feedbackMessage = buildFeedbackMessage(normalizedScore, strengths, weaknesses, feedback);
            
            return new EvaluationResult(normalizedScore, feedbackMessage, 
                "Strengths: " + strengths + "\nWeaknesses: " + weaknesses + "\nDecision: " + decision);
        } catch (Exception e) {
            log.error("Error parsing evaluation response: {}", e.getMessage());
            return new EvaluationResult(0.5, "Failed to parse evaluation", response.substring(0, Math.min(200, response.length())));
        }
    }
    
    /**
     * Extract score from response
     */
    private double extractScore(String response, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern + "\\s*([0-9]+)");
            Matcher m = p.matcher(response);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception e) {
            log.debug("Could not extract score for pattern: {}", pattern);
        }
        return 50.0; // Default to middle value if not found
    }
    
    /**
     * Extract text section from response
     */
    private String extractSection(String response, String startPattern, String endPattern) {
        try {
            int startIdx = response.indexOf(startPattern);
            if (startIdx == -1) return "";
            
            startIdx += startPattern.length();
            
            int endIdx;
            if (endPattern != null) {
                endIdx = response.indexOf(endPattern, startIdx);
                if (endIdx == -1) endIdx = response.length();
            } else {
                endIdx = response.length();
            }
            
            String section = response.substring(startIdx, endIdx).trim();
            return section.substring(0, Math.min(300, section.length())); // Limit length
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Build user-friendly feedback message
     */
    private String buildFeedbackMessage(double normalizedScore, String strengths, String weaknesses, String feedback) {
        StringBuilder msg = new StringBuilder();
        
        if (normalizedScore >= 0.75) {
            msg.append("✅ Quality Assessment: GOOD\n");
        } else if (normalizedScore >= 0.50) {
            msg.append("⚠️ Quality Assessment: NEEDS IMPROVEMENT\n");
        } else {
            msg.append("❌ Quality Assessment: POOR\n");
        }
        
        msg.append(String.format("Score: %.0f%%\n", normalizedScore * 100));
        
        if (!strengths.isEmpty()) {
            msg.append("✓ Strengths: ").append(strengths).append("\n");
        }
        if (!weaknesses.isEmpty()) {
            msg.append("✗ Weaknesses: ").append(weaknesses).append("\n");
        }
        if (!feedback.isEmpty()) {
            msg.append("💡 Feedback: ").append(feedback);
        }
        
        return msg.toString();
    }
    
    /**
     * Evaluation Result DTO
     */
    public static class EvaluationResult {
        private final double qualityScore; // 0.0 to 1.0
        private final String feedbackMessage;
        private final String detailedAnalysis;
        
        public EvaluationResult(double qualityScore, String feedbackMessage, String detailedAnalysis) {
            this.qualityScore = Math.max(0.0, Math.min(1.0, qualityScore)); // Normalize to 0-1
            this.feedbackMessage = feedbackMessage;
            this.detailedAnalysis = detailedAnalysis;
        }
        
        public double getQualityScore() {
            return qualityScore;
        }
        
        public String getFeedbackMessage() {
            return feedbackMessage;
        }
        
        public String getDetailedAnalysis() {
            return detailedAnalysis;
        }
        
        public boolean isAccepted() {
            return qualityScore >= 0.75; // 75% threshold
        }
        
        public String getImprovementSuggestion() {
            if (isAccepted()) {
                return "Test cases meet quality standards.";
            } else if (qualityScore >= 0.50) {
                return "Regenerating with improvement feedback...";
            } else {
                return "Quality below acceptable threshold. Regenerating completely...";
            }
        }
    }
}
