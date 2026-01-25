package com.youtube.research.service;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.Message;
import com.youtube.research.entity.User;
import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.MessageRepository;
import com.youtube.research.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               UserRepository userRepository,
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    public Conversation createConversation(Long userId, String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        if (title.length() > 255) {
            throw new IllegalArgumentException("Title cannot exceed 255 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);

        return conversationRepository.save(conversation);
    }

    public List<Conversation> getConversationsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return conversationRepository.findByUser(user);
    }

    public void deleteConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conversationRepository.deleteById(conversationId);
    }

    public Conversation updateConversation(Long conversationId, String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        conversation.setTitle(newTitle);
        return conversationRepository.save(conversation);
    }

    public List<Message> getConversationMessages(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }
}