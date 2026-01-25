package com.youtube.research.service;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.Message;
import com.youtube.research.repository.ConversationRepository;
import com.youtube.research.repository.MessageRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public MessageService(MessageRepository messageRepository, ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    public Message saveMessage(Long conversationId, String role, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);

        return messageRepository.save(message);
    }

    public List<Message> getConversationMessages(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    public void deleteMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        messageRepository.deleteById(messageId);
    }

    public Optional<Message> getMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }
}