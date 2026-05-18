package com.qaautomation.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaautomation.models.TestCase;
import com.qaautomation.repositories.TestCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Service to initialize test cases from JSON files into the database on startup.
 * DISABLED: Data initialization is disabled to start with 0 test cases.
 * Enable by removing the comment from @Service annotation if needed.
 */
// @Service
@Slf4j
public class DataInitializationService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // @EventListener(ApplicationReadyEvent.class)
    public void loadInitialData() {
        try {
            log.info("Starting data initialization...");
            
            // Check if database already has test cases
            long existingCount = testCaseRepository.count();
            if (existingCount > 0) {
                log.info("Database already contains {} test cases. Skipping initialization.", existingCount);
                return;
            }

            log.info("Database is empty. Loading test cases from JSON file...");
            
            // Try to load from tmp_testcases.json
            File jsonFile = new File("tmp_testcases.json");
            if (jsonFile.exists()) {
                loadTestCasesFromFile(jsonFile);
            } else {
                log.warn("tmp_testcases.json file not found at: {}", jsonFile.getAbsolutePath());
            }
            
            // Check if loading was successful
            long newCount = testCaseRepository.count();
            log.info("Data initialization complete. Database now contains {} test cases.", newCount);
            
        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage(), e);
        }
    }

    private void loadTestCasesFromFile(File jsonFile) {
        try {
            Map<String, Object> data = objectMapper.readValue(jsonFile, Map.class);
            
            if (data.containsKey("testCases")) {
                String testCasesJson = objectMapper.writeValueAsString(data.get("testCases"));
                List<TestCase> testCases = objectMapper.readValue(
                    testCasesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TestCase.class)
                );
                
                if (!testCases.isEmpty()) {
                    List<TestCase> savedTestCases = testCaseRepository.saveAll(testCases);
                    log.info("Successfully loaded {} test cases from JSON file", savedTestCases.size());
                }
            }
        } catch (Exception e) {
            log.error("Error loading test cases from file {}: {}", jsonFile.getAbsolutePath(), e.getMessage(), e);
        }
    }
}
