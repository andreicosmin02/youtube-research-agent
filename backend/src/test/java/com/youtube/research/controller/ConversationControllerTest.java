package com.youtube.research.controller;


import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.User;
import com.youtube.research.security.JwtTokenProvider;
import com.youtube.research.service.ConversationService;
import com.youtube.research.service.MessageService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class ConversationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MessageService messageService;

    @Test
    void shouldCreateConversationAndReturn201() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);

        Long userId = 1L;
        String title = "Machine Learning Discussion";

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(1L);
        mockConversation.setTitle(title);

        when(conversationService.createConversation(eq(userId), eq(title)))
                .thenReturn(mockConversation);

        String requestBody = "{\"title\":\"" + title + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1L)
                .jsonPath("$.title").isEqualTo(title);
    }

    @Test
    void shouldGetAllConversationsForUserAndReturn200() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation conv1 = new Conversation();
        conv1.setId(1L);
        conv1.setTitle("Machine Learning");

        Conversation conv2 = new Conversation();
        conv2.setId(2L);
        conv2.setTitle("Web Development");

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(conv1, conv2));

        // Act & Assert
        webTestClient.get()
                .uri("/api/conversations")
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(1L)
                .jsonPath("$[0].title").isEqualTo("Machine Learning")
                .jsonPath("$[1].id").isEqualTo(2L)
                .jsonPath("$[1].title").isEqualTo("Web Development");
    }

    @Test
    void shouldGetConversationByIdAndReturn200() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long conversationId = 1L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);
        mockConversation.setTitle("Machine Learning");

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(mockConversation));

        // Act & Assert
        webTestClient.get()
                .uri("/api/conversations/{id}", conversationId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(conversationId)
                .jsonPath("$.title").isEqualTo("Machine Learning");
    }

    @Test
    void shouldReturnNotFoundWhenUserAccessesOtherUsersConversation() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long otherUsersConversationId = 999L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        // User has no conversations
        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of());

        // Act & Assert
        webTestClient.get()
                .uri("/api/conversations/{id}", otherUsersConversationId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldUpdateConversationTitleAndReturn200() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long conversationId = 1L;
        String newTitle = "Updated Title";

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);
        mockConversation.setTitle(newTitle);

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(mockConversation));

        when(conversationService.updateConversation(conversationId, newTitle))
                .thenReturn(mockConversation);

        String requestBody = "{\"title\":\"" + newTitle + "\"}";

        // Act & Assert
        webTestClient.put()
                .uri("/api/conversations/{id}", conversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(conversationId)
                .jsonPath("$.title").isEqualTo(newTitle);
    }

    @Test
    void shouldDeleteConversationAndReturn204() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long conversationId = 1L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);
        mockConversation.setTitle("To Delete");

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(mockConversation));

        doNothing().when(conversationService).deleteConversation(conversationId);

        // Act & Assert
        webTestClient.delete()
                .uri("/api/conversations/{id}", conversationId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldReturnNotFoundWhenUserDeletesOtherUsersConversation() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long otherUsersConversationId = 999L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        // User has no conversations
        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of());

        // Act & Assert
        webTestClient.delete()
                .uri("/api/conversations/{id}", otherUsersConversationId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingConversationsWithoutToken() {
        // Act & Assert
        webTestClient.get()
                .uri("/api/conversations")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldGetConversationWithMessagesAndReturn200() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long conversationId = 1L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);
        mockConversation.setTitle("Machine Learning");

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(mockConversation));

        com.youtube.research.entity.Message msg1 = new com.youtube.research.entity.Message();
        msg1.setId(1L);
        msg1.setRole("user");
        msg1.setContent("{\"type\":\"text\",\"text\":\"Hello\"}");

        com.youtube.research.entity.Message msg2 = new com.youtube.research.entity.Message();
        msg2.setId(2L);
        msg2.setRole("assistant");
        msg2.setContent("{\"type\":\"text\",\"text\":\"Hi there\"}");

        when(conversationService.getConversationMessages(conversationId))
                .thenReturn(java.util.List.of(msg1, msg2));

        // Act & Assert
        webTestClient.get()
                .uri("/api/conversations/{id}?includeMessages=true", conversationId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(conversationId)
                .jsonPath("$.title").isEqualTo("Machine Learning")
                .jsonPath("$.messages.length()").isEqualTo(2)
                .jsonPath("$.messages[0].role").isEqualTo("user")
                .jsonPath("$.messages[1].role").isEqualTo("assistant");
    }

    @Test
    void shouldCreateMessageInConversationAndReturn201() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long conversationId = 1L;
        String messageContent = "{\"type\":\"text\",\"text\":\"Find videos about machine learning\"}";

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);
        mockConversation.setTitle("Machine Learning");

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(mockConversation));

        com.youtube.research.entity.Message savedMessage = new com.youtube.research.entity.Message();
        savedMessage.setId(1L);
        savedMessage.setRole("user");
        savedMessage.setContent(messageContent);

        when(messageService.saveMessage(conversationId, "user", messageContent))
                .thenReturn(savedMessage);

        String requestBody = "{\"content\":\"" + messageContent.replace("\"", "\\\"") + "\"}";

        // Act & Assert
        webTestClient.post()
                .uri("/api/conversations/{conversationId}/messages", conversationId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1L)
                .jsonPath("$.role").isEqualTo("user")
                .jsonPath("$.content").isNotEmpty();
    }

    @Test
    void shouldGetConversationMessagesAndReturn200() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long conversationId = 1L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);
        mockConversation.setTitle("Machine Learning");

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(mockConversation));

        com.youtube.research.entity.Message msg1 = new com.youtube.research.entity.Message();
        msg1.setId(1L);
        msg1.setRole("user");
        msg1.setContent("{\"type\":\"text\",\"text\":\"Hello\"}");

        com.youtube.research.entity.Message msg2 = new com.youtube.research.entity.Message();
        msg2.setId(2L);
        msg2.setRole("assistant");
        msg2.setContent("{\"type\":\"text\",\"text\":\"Hi there\"}");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(java.util.List.of(msg1, msg2));

        // Act & Assert
        webTestClient.get()
                .uri("/api/conversations/{conversationId}/messages", conversationId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(1L)
                .jsonPath("$[0].role").isEqualTo("user")
                .jsonPath("$[1].id").isEqualTo(2L)
                .jsonPath("$[1].role").isEqualTo("assistant");
    }

    @Test
    void shouldDeleteMessageAndReturn204() {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        Long userId = 1L;
        Long conversationId = 1L;
        Long messageId = 1L;

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername(username);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(mockUser));

        Conversation mockConversation = new Conversation();
        mockConversation.setId(conversationId);
        mockConversation.setUser(mockUser);
        mockConversation.setTitle("Machine Learning");

        when(conversationService.getConversationsByUser(userId))
                .thenReturn(java.util.List.of(mockConversation));

        com.youtube.research.entity.Message mockMessage = new com.youtube.research.entity.Message();
        mockMessage.setId(messageId);
        mockMessage.setConversation(mockConversation);
        mockMessage.setRole("user");
        mockMessage.setContent("{\"type\":\"text\",\"text\":\"To delete\"}");

        when(messageService.getMessageById(messageId))
                .thenReturn(Optional.of(mockMessage));

        doNothing().when(messageService).deleteMessage(messageId);

        // Act & Assert
        webTestClient.delete()
                .uri("/api/conversations/{conversationId}/messages/{messageId}", conversationId, messageId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNoContent();
    }
}
