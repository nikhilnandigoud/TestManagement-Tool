package com.qaautomation.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LoginController {

    /**
     * Serve the login page
     */
    @GetMapping("/login")
    public ResponseEntity<String> login() {
        log.info("Accessing login page");
        // Redirect to login.html
        return ResponseEntity.status(307)
            .header("Location", "/testmanagement/login.html")
            .build();
    }

    /**
     * Logout endpoint
     */
    @GetMapping("/logout")
    public ResponseEntity<String> logout() {
        log.info("User logged out");
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(307)
            .header("Location", "/testmanagement/login.html")
            .build();
    }
}

