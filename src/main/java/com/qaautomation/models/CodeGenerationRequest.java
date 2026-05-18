package com.qaautomation.models;

import com.qaautomation.models.CodeArtifact.Framework;
import com.qaautomation.models.CodeArtifact.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for automation code generation from scenario text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeGenerationRequest {
    private String scenarioText;
    private Framework framework;
    private Language language;
    private String outputType; // "production" or "test"
    private CodeGenerationOptions options;
    public Boolean getUsePageObjectModel() {
        return options != null ? options.getUsePageObjectModel() : true;
    }
    public Boolean getIncludeSetupTeardown() {
        return options != null ? options.getIncludeSetupTeardown() : true;
    }
    /**
     * Options for code generation behavior.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodeGenerationOptions {
        private Boolean usePageObjectModel;
        private Boolean includeSetupTeardown;
        private String codeStyle; // "camelCase", "snake_case"
        private Boolean includeComments;
        private Boolean includeAssertions;
        public Boolean getUsePageObjectModel() {
            return usePageObjectModel != null ? usePageObjectModel : true;
        }
        public Boolean getIncludeSetupTeardown() {
            return includeSetupTeardown != null ? includeSetupTeardown : true;
        }
        public Boolean getIncludeComments() {
            return includeComments != null ? includeComments : true;
        }
        public Boolean getIncludeAssertions() {
            return includeAssertions != null ? includeAssertions : true;
        }
        public String getCodeStyle() {
            return codeStyle != null ? codeStyle : "camelCase";
        }
    }
}