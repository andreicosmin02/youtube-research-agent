package com.youtube.research.service;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.Message;
import com.youtube.research.entity.User;
import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.MessageRepository;
import com.youtube.research.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void shouldCreateConversationForUser() {
        // Arrange
        Long userId = 1L;
        String title = "Machine Learning Discussion";

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Conversation savedConversation = new Conversation();
        savedConversation.setId(1L);
        savedConversation.setUser(user);
        savedConversation.setTitle(title);

        when(userRepository.findById(eq(userId))).thenReturn(java.util.Optional.of(user));

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation conv = invocation.getArgument(0);
                    if (conv.getId() == null) {
                        conv.setId(1L);
                    }
                    return conv;
                });

        // Act
        Conversation result = conversationService.createConversation(userId, title);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getUser().getId()).isEqualTo(userId);

        verify(userRepository).findById(eq(userId));
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingConversationWithEmptyTitle() {
        // Arrange
        Long userId = 1L;

        // Act & Assert
        assertThatThrownBy(() -> conversationService.createConversation(userId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenCreatingConversationWithNullTitle() {
        // Arrange
        Long userId = 1L;

        // Act & Assert
        assertThatThrownBy(() -> conversationService.createConversation(userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenCreatingConversationWithTitleTooLong() {
        // Arrange
        Long userId = 1L;
        String tooLongTitle = "a".repeat(256);

        // Act & Assert
        assertThatThrownBy(() -> conversationService.createConversation(userId, tooLongTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title cannot exceed 255 characters");
    }

    @Test
    void shouldThrowExceptionWhenCreatingConversationForNonExistentUser() {
        // Arrange
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> conversationService.createConversation(userId, "Title"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldGetConversationsByUser() {
        // Arrange
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Conversation conv1 = new Conversation();
        conv1.setId(1L);
        conv1.setUser(user);
        conv1.setTitle("First conversation");

        Conversation conv2 = new Conversation();
        conv2.setId(2L);
        conv2.setUser(user);
        conv2.setTitle("Second conversation");

        List<Conversation> conversations = List.of(conv1, conv2);

        when(conversationRepository.findByUser(user)).thenReturn(conversations);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // Act
        List<Conversation> result = conversationService.getConversationsByUser(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting("title").containsExactly("First conversation", "Second conversation");
    }

    @Test
    void shouldUpdateConversationTitle() {
        // Arrange
        Long conversationId = 1L;
        String newTitle = "Updated Title";

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setTitle("Old Title");

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.of(conversation));

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Conversation result = conversationService.updateConversation(conversationId, newTitle);

        // Assert
        assertThat(result.getTitle()).isEqualTo(newTitle);
        verify(conversationRepository).findById(conversationId);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithEmptyTitle() {
        // Arrange
        Long conversationId = 1L;

        // Act & Assert
        assertThatThrownBy(() -> conversationService.updateConversation(conversationId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentConversation() {
        // Arrange
        Long conversationId = 999L;

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> conversationService.updateConversation(conversationId, "New Title"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation not found");
    }

    @Test
    void shouldDeleteConversation() {
        // Arrange
        Long conversationId = 1L;

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setUser(user);
        conversation.setTitle("Conversation to Delete");

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.of(conversation));

        // Act
        conversationService.deleteConversation(conversationId);

        // Assert
        verify(conversationRepository).deleteById(conversationId);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentConversation() {
        // Arrange
        Long conversationId = 999L;

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> conversationService.deleteConversation(conversationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation not found");
    }

    @Test
    void shouldGetConversationMessages() {
        // Arrange
        Long conversationId = 1L;
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setUser(user);
        conversation.setTitle("Test Conversation");

        Message msg1 = new Message();
        msg1.setId(1L);
        msg1.setConversation(conversation);
        msg1.setRole("user");
        msg1.setContent("{\"type\":\"text\",\"text\":\"First\"}");

        Message msg2 = new Message();
        msg2.setId(2L);
        msg2.setConversation(conversation);
        msg2.setRole("assistant");
        msg2.setContent("{\"type\":\"text\",\"text\":\"Second\"}");

        List<Message> messages = List.of(msg1, msg2);

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.of(conversation));

        when(messageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                .thenReturn(messages);

        // Act
        List<Message> result = conversationService.getConversationMessages(conversationId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting("role").containsExactly("user", "assistant");

        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Test
    void shouldThrowExceptionWhenGettingMessagesForNonExistentConversation() {
        // Arrange
        Long conversationId = 999L;

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> conversationService.getConversationMessages(conversationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation not found");
    }
}