package com.qaautomation.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IntegrationSettingsViewController {

    @GetMapping(value = "/integrations/settings", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> integrationSettings() {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(new ClassPathResource("templates/integrations/settings.html"));
    }
}
