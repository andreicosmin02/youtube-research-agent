package com.youtube.research.service;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.Message;
import com.youtube.research.entity.User;
import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private MessageService messageService;

    private User createTestUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        return user;
    }

    private Conversation createTestConversation(Long conversationId, User user) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setUser(user);
        conversation.setTitle("Test Conversation");
        return conversation;
    }

    @Test
    void shouldGetConversationMessages() {
        // Arrange
        Long conversationId = 1L;
        User user = createTestUser(1L);
        Conversation conversation = createTestConversation(conversationId, user);

        Message msg1 = new Message();
        msg1.setId(1L);
        msg1.setConversation(conversation);
        msg1.setRole("user");
        msg1.setContent("{\"type\":\"text\",\"text\":\"First message\"}");

        Message msg2 = new Message();
        msg2.setId(2L);
        msg2.setConversation(conversation);
        msg2.setRole("assistant");
        msg2.setContent("{\"type\":\"text\",\"text\":\"Assistant response\"}");

        List<Message> messages = List.of(msg1, msg2);

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.of(conversation));

        when(messageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                .thenReturn(messages);

        // Act
        List<Message> result = messageService.getConversationMessages(conversationId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting("role").containsExactly("user", "assistant");
        assertThat(result).extracting("id").containsExactly(1L, 2L);

        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Test
    void shouldDeleteMessage() {
        // Arrange
        Long messageId = 1L;
        User user = createTestUser(1L);
        Conversation conversation = createTestConversation(1L, user);

        Message message = new Message();
        message.setId(messageId);
        message.setConversation(conversation);
        message.setRole("user");
        message.setContent("{\"type\":\"text\",\"text\":\"Message to delete\"}");

        when(messageRepository.findById(messageId))
                .thenReturn(java.util.Optional.of(message));

        // Act
        messageService.deleteMessage(messageId);

        // Assert
        verify(messageRepository).findById(messageId);
        verify(messageRepository).deleteById(messageId);
    }

    @Test
    void shouldThrowExceptionWhenConversationNotFound() {
        // Arrange
        Long conversationId = 999L;
        String messageContent = "{\"type\":\"text\",\"text\":\"Some message\"}";

        when(conversationRepository.findById(conversationId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> messageService.saveMessage(conversationId, "user", messageContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation not found");
    }

    @Test
    void shouldThrowExceptionWhenMessageNotFound() {
        // Arrange
        Long messageId = 999L;

        when(messageRepository.findById(messageId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> messageService.deleteMessage(messageId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message not found");
    }
}