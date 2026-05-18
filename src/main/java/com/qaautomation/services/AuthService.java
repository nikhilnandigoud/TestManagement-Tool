package com.qaautomation.services;

import com.qaautomation.dto.AuthResponse;
import com.qaautomation.dto.LoginRequest;
import com.qaautomation.dto.SignupRequest;
import com.qaautomation.models.User;
import com.qaautomation.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Register a new user
     */
    public AuthResponse signup(SignupRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.builder()
                .success(false)
                .message("Username already exists")
                .build();
        }
        
        // Create new user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getUsername() + "@local.testmanagement")
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getUsername())
            .isActive(true)
            .build();
        
        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());
        
        return AuthResponse.builder()
            .success(true)
            .message("User registered successfully")
            .user(AuthResponse.UserDTO.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .build())
            .build();
    }
    
    /**
     * Authenticate user login
     */
    public AuthResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (userOpt.isEmpty()) {
            log.warn("Login failed: User not found - {}", request.getUsername());
            return AuthResponse.builder()
                .success(false)
                .message("Invalid username or password")
                .build();
        }
        
        User user = userOpt.get();
        
        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Login failed: Account inactive - {}", user.getUsername());
            return AuthResponse.builder()
                .success(false)
                .message("Account is inactive")
                .build();
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Invalid password - {}", user.getUsername());
            return AuthResponse.builder()
                .success(false)
                .message("Invalid username or password")
                .build();
        }
        
        log.info("User logged in successfully: {}", user.getUsername());
        
        return AuthResponse.builder()
            .success(true)
            .message("Login successful")
            .user(AuthResponse.UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build())
            .build();
    }
    
    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
