package com.youtube.research.service;

import com.youtube.research.entity.User;
import com.youtube.research.repository.UserRepository;
import com.youtube.research.security.UserInputValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserInputValidator inputValidator;

    public UserService(UserRepository userRepository, UserInputValidator inputValidator) {
        this.userRepository = userRepository;
        this.inputValidator = inputValidator;
        this.passwordEncoder = new BCryptPasswordEncoder();
        log.info("UserService initialized with validator: {}", inputValidator != null);
    }

    public User registerUser(String username, String password) {
        log.info("Attempting to register user: {}", username);

        // Validate input FIRST
        try {
            inputValidator.validateUsername(username);
            inputValidator.validatePassword(password);
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for user {}: {}", username, e.getMessage());
            throw e;  // Re-throw the exception
        }

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
        log.info("User registered successfully: {}", username);
        return saved;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public boolean authenticateUser(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(password, user.get().getPasswordHash());
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}