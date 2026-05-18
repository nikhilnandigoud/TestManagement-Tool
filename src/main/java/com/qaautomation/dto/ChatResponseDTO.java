package com.qaautomation.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {
    private String id;
    private String role;
    private String content;
    private String timestamp;
    private Boolean isActionable;
    private Integer vectorHits;
    private Integer guidesUsed;
    
    // Priority Context Information
    private String contextSummary; // "Context Used: Scenario + 2 Guide(s) + 3 File(s)"
    private List<String> processingSteps; // ["✓ Assembled scenario", "✓ Loaded 2 guides", ...]
    private Boolean hasScenario;
    private Boolean hasGuides;
    private Boolean hasFiles;
    private Boolean hasHistorical;
    private Integer filesUsed; // Count of files used
    private List<TestCaseDTO> testCases;
}
