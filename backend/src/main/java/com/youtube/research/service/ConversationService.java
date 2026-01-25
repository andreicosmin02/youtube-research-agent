package com.youtube.research.service;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.User;
import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository, UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    public Conversation createConversation(Long userId, String title) {
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
}