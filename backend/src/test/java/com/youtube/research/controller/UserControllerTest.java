package com.youtube.research.controller;

import com.youtube.research.entity.User;
import com.youtube.research.security.JwtTokenProvider;
import com.youtube.research.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldRegisterUserAndReturn200() {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        when(userService.registerUser(anyString(), anyString()))
                .thenReturn(mockUser);

        String requestBody = "{\"username\":\"testuser\",\"password\":\"password123\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1L)
                .jsonPath("$.username").isEqualTo("testuser");
    }

    @Test
    void shouldGetUserByIdAndReturn200() {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserById(userId))
                .thenReturn(Optional.of(mockUser));

        // Act & Assert
        webTestClient.get()
                .uri("/api/users/{id}", userId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.username").isEqualTo(username);
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() {
        // Arrange
        Long userId = 999L;
        String token = "Bearer " + jwtTokenProvider.generateToken("testuser");

        when(userService.getUserById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        webTestClient.get()
                .uri("/api/users/{id}", userId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnBadRequestWhenUsernameAlreadyExists() {
        // Arrange
        String requestBody = "{\"username\":\"existinguser\",\"password\":\"password123\"}";

        when(userService.registerUser(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        // Act & Assert
        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldDeleteUserAndReturn204() {
        // Arrange
        Long userId = 1L;
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);  // Same username as token!

        when(userService.getUserById(userId))
                .thenReturn(Optional.of(mockUser));

        doNothing().when(userService).deleteUser(userId);

        // Act & Assert
        webTestClient.delete()
                .uri("/api/users/{id}", userId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNoContent();
    }


    @Test
    void shouldLoginUserAndReturnToken() {
        // Arrange
        String username = "testuser";
        String password = "password123";

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(username);
        mockUser.setPasswordHash("hashedpassword");

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));
        when(userService.authenticateUser(username, password))
                .thenReturn(true);

        String requestBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.username").isEqualTo(username);
    }

    @Test
    void shouldDenyAccessWithoutToken() {
        // Act & Assert
        webTestClient.get()
                .uri("/api/users/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowAccessWithValidToken() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(username);

        when(userService.getUserById(1L))
                .thenReturn(Optional.of(mockUser));

        // Act & Assert
        webTestClient.get()
                .uri("/api/users/1")
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo(username);
    }

    @Test
    void shouldNotAllowUserToViewOtherUsersProfile() {
        // Arrange
        Long otherUserId = 2L;
        String myUsername = "user1";
        String token = "Bearer " + jwtTokenProvider.generateToken(myUsername);

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setUsername("user2");

        when(userService.getUserById(otherUserId))
                .thenReturn(Optional.of(otherUser));

        // Act & Assert
        webTestClient.get()
                .uri("/api/users/{id}", otherUserId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound();  // Return 404, not 403
    }

    @Test
    void shouldNotAllowUserToDeleteOtherUsersAccount() {
        // Arrange
        Long otherUserId = 2L;
        String myUsername = "user1";
        String token = "Bearer " + jwtTokenProvider.generateToken(myUsername);

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setUsername("user2");

        when(userService.getUserById(otherUserId))
                .thenReturn(Optional.of(otherUser));

        // Act & Assert
        webTestClient.delete()
                .uri("/api/users/{id}", otherUserId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound();  // Return 404, not 403
    }


    @Test
    void shouldRejectWeakPassword() { // Name matches the command you tried
        // Arrange
        String requestBody = "{\"username\":\"testuser\",\"password\":\"weak\"}"; // Example weak password

        when(userService.registerUser(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Password must be at least 8 characters")); // Or whatever message/validator throws

        // Act & Assert
        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest() // Expects 400
                .expectBody()
                .jsonPath("$.message").isEqualTo("Password must be at least 8 characters"); // Or check error structure
    }
}