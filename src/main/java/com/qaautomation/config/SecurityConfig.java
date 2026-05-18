package com.qaautomation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .authorizeHttpRequests(authz -> authz
                // Allow login and signup endpoints
                .requestMatchers(
                    "/auth/login",
                    "/auth/signup",
                    "/auth/status",
                    "/login"
                ).permitAll()
                // Allow all static resources
                .requestMatchers(
                    "/login.html",
                    "/index.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",
                    "/static/**",
                    "/webjars/**"
                ).permitAll()
                // Health check endpoint
                .requestMatchers("/health/**").permitAll()
                // Error endpoint
                .requestMatchers("/error").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );
        
        return http.build();
    }
}
