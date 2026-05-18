package com.qaautomation.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserGuideContext - Represents extracted content from a user guide document (PDF/DOCX).
 * This context is used to enrich test case generation with real UI/workflow information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGuideContext {

    /**
     * Original filename of the user guide (e.g., "Decision_Training_for_Release_Managers.pdf")
     */
    private String guideFileName;

    /**
     * Full extracted text content from the document
     */
    private String guideContent;

    /**
     * Associated module/topic (e.g., "Decision Manager", "Modeling", "Testing")
     */
    private String module;

    /**
     * Brief summary/description of the guide
     */
    private String description;

    /**
     * The file path where the guide is stored
     */
    private String filePath;

    /**
     * Timestamp when the guide was loaded
     */
    private LocalDateTime loadedAt;

    /**
     * Size of extracted content in characters
     */
    private long contentLength;

    /**
     * Flag to indicate if content was successfully extracted
     */
    private boolean isValid;

    /**
     * Optional error message if extraction failed
     */
    private String errorMessage;
}
