package com.youtube.research.entity;

import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.MessageRepository;
import com.youtube.research.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

@DataJpaTest
class MessageEntityTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    // Helper method to create test data
    private Conversation createTestConversation(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash");
        User savedUser = userRepository.save(user);

        Conversation conversation = new Conversation();
        conversation.setUser(savedUser);
        conversation.setTitle("Test Conversation");
        return conversationRepository.save(conversation);
    }

    @Test
    void shouldCreateAndSaveMessage() {
        // Arrange
        Conversation conversation = createTestConversation("alice");

        String jsonContent = "{\"type\":\"text\",\"text\":\"Find me videos about machine learning\"}";

        Message message = new Message();
        message.setConversation(conversation);
        message.setRole("user");
        message.setContent(jsonContent);

        // Act
        Message saved = messageRepository.save(message);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo("user");
        assertThat(saved.getContent()).isEqualTo(jsonContent);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindMessageById() {
        // Arrange
        Conversation conversation = createTestConversation("bob");

        Message message = new Message();
        message.setConversation(conversation);
        message.setRole("assistant");
        message.setContent("{\"type\":\"text\",\"text\":\"Here are the results\"}");
        Message saved = messageRepository.save(message);

        // Act
        var found = messageRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo("assistant");
    }

    @Test
    void shouldFindMessagesByConversation() {
        // Arrange
        Conversation conversation = createTestConversation("charlie");

        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setRole("user");
        msg1.setContent("{\"type\":\"text\",\"text\":\"Question 1\"}");
        messageRepository.save(msg1);

        Message msg2 = new Message();
        msg2.setConversation(conversation);
        msg2.setRole("assistant");
        msg2.setContent("{\"type\":\"text\",\"text\":\"Answer 1\"}");
        messageRepository.save(msg2);

        Message msg3 = new Message();
        msg3.setConversation(conversation);
        msg3.setRole("user");
        msg3.setContent("{\"type\":\"text\",\"text\":\"Question 2\"}");
        messageRepository.save(msg3);

        // Act
        List<Message> found = messageRepository.findByConversation(conversation);

        // Assert
        assertThat(found).hasSize(3);
        assertThat(found)
                .extracting("role")
                .containsExactlyInAnyOrder("user", "assistant", "user");
    }

    @Test
    void shouldFindMessagesByConversationOrderedByCreatedAt() {
        // Arrange
        Conversation conversation = createTestConversation("diana");

        Message msg1 = new Message();
        msg1.setConversation(conversation);
        msg1.setRole("user");
        msg1.setContent("{\"type\":\"text\",\"text\":\"First\"}");
        messageRepository.save(msg1);

        Message msg2 = new Message();
        msg2.setConversation(conversation);
        msg2.setRole("assistant");
        msg2.setContent("{\"type\":\"text\",\"text\":\"Second\"}");
        messageRepository.save(msg2);

        // Act
        List<Message> found = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);

        // Assert
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getContent()).contains("First");
        assertThat(found.get(1).getContent()).contains("Second");
    }

    @Test
    void shouldUpdateMessage() {
        // Arrange
        Conversation conversation = createTestConversation("eve");

        Message message = new Message();
        message.setConversation(conversation);
        message.setRole("user");
        message.setContent("{\"type\":\"text\",\"text\":\"Original content\"}");
        Message saved = messageRepository.save(message);

        // Act
        saved.setContent("{\"type\":\"text\",\"text\":\"Updated content\"}");
        Message updated = messageRepository.save(saved);

        // Assert
        assertThat(updated.getContent()).contains("Updated content");
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }
}