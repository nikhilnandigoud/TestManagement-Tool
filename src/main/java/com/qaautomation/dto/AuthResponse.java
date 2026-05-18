package com.qaautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Boolean success;
    private String message;
    private UserDTO user;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDTO {
        private Long id;
        private String username;
    }
}
