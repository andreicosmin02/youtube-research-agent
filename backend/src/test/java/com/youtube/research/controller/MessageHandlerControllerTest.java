package com.youtube.research.controller;

import com.youtube.research.security.JwtTokenProvider;
import com.youtube.research.service.ConversationService;
import com.youtube.research.service.MessageHandlerService;
import com.youtube.research.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class MessageHandlerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MessageHandlerService messageHandlerService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ConversationService conversationService;

    @Test
    void shouldHandleMessageAndReturn200() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";

        com.youtube.research.entity.User mockUser = new com.youtube.research.entity.User();
        mockUser.setId(1L);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        com.youtube.research.entity.Conversation mockConversation =
                new com.youtube.research.entity.Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);

        when(conversationService.getConversationsByUser(1L))
                .thenReturn(java.util.List.of(mockConversation));

        when(messageHandlerService.handleMessage(conversationId, userMessage))
                .thenReturn("Here are some videos");

        String requestBody = "{\"message\":\"" + userMessage + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations/{conversationId}/send-message", conversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldReturnNotFoundWhenUserAccessesOtherUsersConversation() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long otherUsersConversationId = 999L;
        String userMessage = "Find me videos about machine learning";

        com.youtube.research.entity.User mockUser = new com.youtube.research.entity.User();
        mockUser.setId(1L);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        String requestBody = "{\"message\":\"" + userMessage + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations/{conversationId}/send-message", otherUsersConversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnBadRequestWhenMessageIsEmpty() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long conversationId = 1L;

        String requestBody = "{\"message\":\"\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations/{conversationId}/send-message", conversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnInternalServerErrorWhenOllamaFails() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";

        com.youtube.research.entity.User mockUser = new com.youtube.research.entity.User();
        mockUser.setId(1L);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        com.youtube.research.entity.Conversation mockConversation =
                new com.youtube.research.entity.Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);

        when(conversationService.getConversationsByUser(1L))
                .thenReturn(java.util.List.of(mockConversation));

        doThrow(new IOException("Ollama service unavailable"))
                .when(messageHandlerService)
                .handleMessage(conversationId, userMessage);

        String requestBody = "{\"message\":\"" + userMessage + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations/{conversationId}/send-message", conversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldReturnAssistantResponseInBody() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";
        String assistantResponse = "I found some great videos on machine learning";

        com.youtube.research.entity.User mockUser = new com.youtube.research.entity.User();
        mockUser.setId(1L);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        com.youtube.research.entity.Conversation mockConversation =
                new com.youtube.research.entity.Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);

        when(conversationService.getConversationsByUser(1L))
                .thenReturn(java.util.List.of(mockConversation));

        when(messageHandlerService.handleMessage(conversationId, userMessage))
                .thenReturn(assistantResponse);

        String requestBody = "{\"message\":\"" + userMessage + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations/{conversationId}/send-message", conversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.response").isEqualTo(assistantResponse);
    }

    @Test
    void shouldRouteToYouTubeSearchBasedOnAgentDecision() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";

        com.youtube.research.entity.User mockUser = new com.youtube.research.entity.User();
        mockUser.setId(1L);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        com.youtube.research.entity.Conversation mockConversation =
                new com.youtube.research.entity.Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);

        when(conversationService.getConversationsByUser(1L))
                .thenReturn(java.util.List.of(mockConversation));

        // Mock the service to verify it's called
        when(messageHandlerService.handleMessage(conversationId, userMessage))
                .thenReturn("Here are some videos");

        String requestBody = "{\"message\":\"" + userMessage + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations/{conversationId}/send-message", conversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.response").isEqualTo("Here are some videos");

        // Verify the service was called with the right parameters
        verify(messageHandlerService).handleMessage(conversationId, userMessage);
    }
}