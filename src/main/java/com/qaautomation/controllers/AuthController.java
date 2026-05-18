package com.qaautomation.controllers;

import com.qaautomation.dto.AuthResponse;
import com.qaautomation.dto.LoginRequest;
import com.qaautomation.dto.SignupRequest;
import com.qaautomation.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                    .success(false)
                    .message("Username is required")
                    .build());
        }
        
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                    .success(false)
                    .message("Password is required")
                    .build());
        }
        
        AuthResponse response = authService.login(request);
        if (response.getSuccess()) {
            // create an Authentication and store it in the SecurityContext so subsequent requests are authenticated
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(request.getUsername(), null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            // ensure a session exists so JSESSIONID cookie is issued
            httpRequest.getSession(true);
            // also store the security context in the HTTP session so it persists across requests
            httpRequest.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    /**
     * User signup/registration endpoint
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        log.info("Signup attempt for user: {}", request.getUsername());
        
        // Validate input
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                    .success(false)
                    .message("Username is required")
                    .build());
        }
        
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                    .success(false)
                    .message("Password must be at least 6 characters")
                    .build());
        }
        
        AuthResponse response = authService.signup(request);
        return response.getSuccess() 
            ? ResponseEntity.status(HttpStatus.CREATED).body(response)
            : ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Check if user is authenticated
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && 
            !"anonymousUser".equals(auth.getPrincipal());
        
        if (isAuthenticated) {
            return ResponseEntity.ok().body(new Object() {
                public final boolean authenticated = true;
                public final String principal = auth.getName();
            });
        } else {
            return ResponseEntity.status(401).body(new Object() {
                public final boolean authenticated = false;
            });
        }
    }
}
