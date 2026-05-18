package com.qaautomation.models;

import com.qaautomation.models.CodeArtifact.Framework;
import com.qaautomation.models.CodeArtifact.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to generate automation code from existing test cases
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateCodeFromTestCasesRequest {
    private List<String> testCaseIds;
    private Framework framework;
    private Language language;
    private Boolean usePageObjectModel;
    private Boolean includeSetupTeardown;

    public Boolean getUsePageObjectModel() {
        return usePageObjectModel != null ? usePageObjectModel : true;
    }

    public Boolean getIncludeSetupTeardown() {
        return includeSetupTeardown != null ? includeSetupTeardown : true;
    }
}
