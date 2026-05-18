package com.qaautomation.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom error controller to handle SPA routing
 */
@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @GetMapping
    public String handleError() {
        return "forward:/index.html";
    }

    public String getErrorPath() {
        return "/error";
    }
}
