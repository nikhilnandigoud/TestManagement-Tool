package com.qaautomation.dto;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseDTO {
    private String id;
    private String title;
    private List<String> steps;
    private String expectedResults;
    private String status;
    private Integer version;
    private String createdAt;
}
