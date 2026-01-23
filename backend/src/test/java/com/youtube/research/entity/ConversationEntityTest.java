package com.youtube.research.entity;

import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ConversationEntityTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void shouldCreateAndSaveConversation() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        Conversation conversation = new Conversation();
        conversation.setUser(savedUser);
        conversation.setTitle("Machine Learning");

        Conversation saved = conversationRepository.save(conversation);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Machine Learning");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindConversationById() {
        User user = new User();
        user.setUsername("bob");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        Conversation conversation = new Conversation();
        conversation.setUser(savedUser);
        conversation.setTitle("Python Tips");
        Conversation saved = conversationRepository.save(conversation);

        var found = conversationRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Python Tips");
    }

    @Test
    void shouldFindConversationsByUser() {
        // Arrange - create user and multiple conversations
        User user = new User();
        user.setUsername("charlie");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        Conversation conv1 = new Conversation();
        conv1.setUser(savedUser);
        conv1.setTitle("AI");
        conversationRepository.save(conv1);

        Conversation conv2 = new Conversation();
        conv2.setUser(savedUser);
        conv2.setTitle("Database");
        conversationRepository.save(conv2);

        // Act
        List<Conversation> found = conversationRepository.findByUser(savedUser);

        // Assert
        assertThat(found.size()).isEqualTo(2);
        assertThat(found).extracting("title").containsExactlyInAnyOrder("AI", "Database");
    }

    @Test
    void shouldUpdateConversation() {
        // Arrange
        User user = new User();
        user.setUsername("diana");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        Conversation conversation = new Conversation();
        conversation.setUser(savedUser);
        conversation.setTitle("Old Title");
        Conversation saved = conversationRepository.save(conversation);

        // Act
        saved.setTitle("New Title");
        Conversation updated = conversationRepository.save(saved);

        // Assert
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    void shouldDeleteConversation() {
        // Arrange
        User user = new User();
        user.setUsername("eve");
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        Conversation conversation = new Conversation();
        conversation.setUser(savedUser);
        conversation.setTitle("To Delete");
        Conversation saved = conversationRepository.save(conversation);
        Long conversationId = saved.getId();

        // Act
        conversationRepository.deleteById(conversationId);

        // Assert
        var found = conversationRepository.findById(conversationId);
        assertThat(found).isEmpty();
    }
}
