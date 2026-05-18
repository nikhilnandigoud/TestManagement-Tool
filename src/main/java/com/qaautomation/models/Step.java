package com.qaautomation.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Data model representing a single test step within a test case.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Step {
    private int stepNumber;
    
    @Column(columnDefinition = "TEXT")
    private String action;
    
    private String testData;
    
    @Column(columnDefinition = "TEXT")
    private String expectedResult;
}
