package com.qaautomation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the AI QA Automation Platform.
 * Launches the Spring Boot application with support for async operations and scheduling.
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class QAAutomationApplication {

    public static void main(String[] args) {
        log.info("🚀 Starting QA Automation Platform...");
        SpringApplication.run(QAAutomationApplication.class, args);
        log.info("✅ QA Automation Platform started successfully");
    }
}
