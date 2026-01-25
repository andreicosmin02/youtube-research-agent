package com.youtube.research.security;

import org.springframework.stereotype.Component;

@Component
public class UserInputValidator {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    public void validateUsername(String username) throws IllegalArgumentException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must be at least " + MIN_USERNAME_LENGTH + " characters");
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username cannot exceed " + MAX_USERNAME_LENGTH + " characters");
        }

        if (!username.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, dots, dashes, and underscores");
        }
    }

    public void validatePassword(String password) throws IllegalArgumentException {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password cannot exceed " + MAX_PASSWORD_LENGTH + " characters");
        }

        // Password must contain at least one uppercase, one lowercase, one number
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, and one number");
        }
    }
}