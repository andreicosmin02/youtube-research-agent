package com.youtube.research.service;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.User;
import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void shouldCreateConversationForUser() {
        // Arrange
        Long userId = 1L;
        String title = "Machine Learning Discussion";

        // Create the user that should be returned by the userRepository mock
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        // Create the conversation that should be returned by the conversationRepository mock
        // after the save operation, including the ID assigned by the 'database'.
        Conversation savedConversation = new Conversation();
        savedConversation.setId(1L);
        savedConversation.setUser(user);
        savedConversation.setTitle(title);

        // Configure the userRepository mock: findById should return the user
        when(userRepository.findById(eq(userId))).thenReturn(java.util.Optional.of(user));

        // Configure the conversationRepository mock: save should return the saved conversation
        // Using ArgumentCaptor to capture the conversation passed to save (before ID assignment by 'DB')
        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        when(conversationRepository.save(conversationCaptor.capture()))
                .thenAnswer(invocation -> {
                    Conversation conv = invocation.getArgument(0);
                    // Simulate the DB assigning an ID. In this case, we just set it directly
                    // as the mock returns the modified object.
                    // Alternatively, you could return 'savedConversation' directly if its state is fully known.
                    // Returning the captured object with the ID set simulates the DB operation more closely.
                    if (conv.getId() == null) {
                        conv.setId(1L); // Assign ID like a DB would
                    }
                    return conv; // Return the same object, now with ID
                });

        // Act
        Conversation result = conversationService.createConversation(userId, title);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getUser().getId()).isEqualTo(userId);

        // Optional Verification: Check interactions
        verify(userRepository).findById(eq(userId));
        verify(conversationRepository).save(any(Conversation.class)); // Or verify with captor if needed

        // Optional Verification: Check the conversation passed to save
        Conversation capturedConversation = conversationCaptor.getValue();
        assertThat(capturedConversation.getUser()).isEqualTo(user);
        assertThat(capturedConversation.getTitle()).isEqualTo(title);
        // The ID might be null here if the service creates the object without ID initially
        // assertThat(capturedConversation.getId()).isNull(); // Uncomment if applicable
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
        conv2.setId(1L);
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
}