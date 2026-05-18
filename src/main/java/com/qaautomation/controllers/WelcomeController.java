package com.qaautomation.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root controller for health checks and API info.
 * Removed catch-all route to prevent SPA routing conflicts.
 */
@Slf4j
@RestController
@RequestMapping("")
public class WelcomeController {

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<ApiResponse> health() {
        log.info("Health check endpoint called");
        return ResponseEntity.ok(new ApiResponse("UP", "QA Automation Platform is running", "8081", "/api/v1"));
    }

    public static class ApiResponse {
        public String status;
        public String message;
        public String port;
        public String info;

        public ApiResponse(String status, String message, String port, String info) {
            this.status = status;
            this.message = message;
            this.port = port;
            this.info = info;
        }

        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getPort() { return port; }
        public String getInfo() { return info; }
    }
}
