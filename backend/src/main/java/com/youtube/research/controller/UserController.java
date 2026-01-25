package com.youtube.research.controller;

import com.youtube.research.dto.LoginResponse;
import com.youtube.research.dto.UserDTO;
import com.youtube.research.entity.User;
import com.youtube.research.security.JwtTokenProvider;
import com.youtube.research.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) {
        log.info("Registration attempt for username: {}", sanitize(request.username()));

        try {
            User registeredUser = userService.registerUser(request.username(), request.password());
            UserDTO userDTO = new UserDTO(registeredUser.getId(), registeredUser.getUsername());

            log.info("User registered successfully: {}", sanitize(request.username()));
            return ResponseEntity.ok(userDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            throw e;  // Let GlobalExceptionHandler catch it
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody RegisterRequest request) {
        // Validate input is not null
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
            log.warn("Login attempt with empty credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        log.info("Login attempt for username: {}", sanitize(request.username()));

        // Use constant time comparison to prevent timing attacks
        // authenticateUser() already uses BCrypt.matches() which is timing-safe
        if (!userService.authenticateUser(request.username(), request.password())) {
            log.warn("Login failed for username: {}", sanitize(request.username()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        // Safe Optional handling - if user was deleted between auth and fetch, return 500
        Optional<User> userOpt = userService.getUserByUsername(request.username());
        if (userOpt.isEmpty()) {
            log.error("User authenticated but not found: {}", sanitize(request.username()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Authentication failed"));
        }

        User user = userOpt.get();
        String token = jwtTokenProvider.generateToken(request.username());

        log.info("User logged in successfully: {}", sanitize(request.username()));
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getId()));
    }

    record RegisterRequest(
            @NotBlank(message = "Username cannot be blank")
            String username,
            @NotBlank(message = "Password cannot be blank")
            String password
    ) {}

    record ErrorResponse(String error) {}

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            log.warn("Attempted to get user {} with no authentication", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String authenticatedUsername = authentication.getName();
        log.debug("User {} requesting profile for user id: {}", authenticatedUsername, id);

        return userService.getUserById(id)
                .filter(user -> user.getUsername().equals(authenticatedUsername))
                .map(user -> {
                    log.debug("User {} viewed their own profile", authenticatedUsername);
                    return ResponseEntity.ok(new UserDTO(user.getId(), user.getUsername()));
                })
                // Return 404 instead of 403 - don't reveal if user exists to non-owners
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            log.warn("Attempted to delete user {} with no authentication", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String authenticatedUsername = authentication.getName();
        log.info("User {} requesting to delete account {}", authenticatedUsername, id);

        Optional<User> userOpt = userService.getUserById(id);

        // If user exists and owns the account, delete it
        if (userOpt.isPresent() && userOpt.get().getUsername().equals(authenticatedUsername)) {
            userService.deleteUser(id);
            log.info("User {} deleted their account", authenticatedUsername);
            return ResponseEntity.noContent().build();
        }

        // Return 404 instead of 403 - don't reveal if user exists to non-owners
        log.warn("Unauthorized deletion attempt by {} for user {}", authenticatedUsername, id);
        return ResponseEntity.notFound().build();
    }

    /**
     * Sanitize username for logging (only show first 2 chars to protect privacy)
     * Example: "john_doe" → "jo****"
     */
    private String sanitize(String username) {
        if (username == null || username.length() < 3) {
            return "***";
        }
        return username.substring(0, 2) + "****";
    }
}