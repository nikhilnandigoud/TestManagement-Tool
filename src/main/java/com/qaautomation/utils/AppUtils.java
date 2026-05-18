package com.qaautomation.utils;

import org.springframework.stereotype.Component;

/**
 * Utility class for common helper methods.
 */
@Component
public class AppUtils {

    /**
     * Validates if an email format is correct.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Truncates text to specified length.
     */
    public static String truncate(String text, int length) {
        if (text == null) {
            return null;
        }
        return text.length() > length ? text.substring(0, length) + "..." : text;
    }

    /**
     * Generates a simple request ID.
     */
    public static String generateRequestId() {
        return "REQ-" + System.currentTimeMillis();
    }
}
