package com.youtube.research.controller;

import com.youtube.research.dto.MessageDTO;
import com.youtube.research.entity.Message;
import com.youtube.research.service.ConversationService;
import com.youtube.research.service.MessageService;
import com.youtube.research.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/conversations")
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserService userService;

    public MessageController(MessageService messageService,
                             ConversationService conversationService,
                             UserService userService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.userService = userService;
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageDTO> createMessage(
            @PathVariable Long conversationId,
            @RequestBody CreateMessageRequest request,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to create message with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} creating message in conversation {}", username, conversationId);

        Long userId = userService.getUserByUsername(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify user owns this conversation
        List<com.youtube.research.entity.Conversation> userConversations = conversationService.getConversationsByUser(userId);
        boolean userOwnsConversation = userConversations.stream()
                .anyMatch(conv -> conv.getId().equals(conversationId));

        if (!userOwnsConversation) {
            log.warn("User {} attempted to message conversation {} they don't own", username, conversationId);
            return ResponseEntity.notFound().build();
        }

        Message message = messageService.saveMessage(conversationId, "user", request.content());

        MessageDTO dto = new MessageDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    record CreateMessageRequest(String content) {}

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getConversationMessages(
            @PathVariable Long conversationId,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to get messages with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} retrieving messages for conversation {}", username, conversationId);

        Long userId = userService.getUserByUsername(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify user owns this conversation
        List<com.youtube.research.entity.Conversation> userConversations = conversationService.getConversationsByUser(userId);
        boolean userOwnsConversation = userConversations.stream()
                .anyMatch(conv -> conv.getId().equals(conversationId));

        if (!userOwnsConversation) {
            log.warn("User {} attempted to access messages in conversation {} they don't own", username, conversationId);
            return ResponseEntity.notFound().build();
        }

        List<Message> messages = messageService.getConversationMessages(conversationId);

        List<MessageDTO> dtos = messages.stream()
                .map(msg -> new MessageDTO(
                        msg.getId(),
                        msg.getRole(),
                        msg.getContent(),
                        msg.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to delete message with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} deleting message {} in conversation {}", username, messageId, conversationId);

        Long userId = userService.getUserByUsername(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify user owns this conversation
        List<com.youtube.research.entity.Conversation> userConversations = conversationService.getConversationsByUser(userId);
        boolean userOwnsConversation = userConversations.stream()
                .anyMatch(conv -> conv.getId().equals(conversationId));

        if (!userOwnsConversation) {
            log.warn("User {} attempted to delete message in conversation {} they don't own", username, conversationId);
            return ResponseEntity.notFound().build();
        }

        // Verify the message belongs to this conversation
        Message message = messageService.getMessageById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            log.warn("User {} attempted to delete message {} not in conversation {}", username, messageId, conversationId);
            return ResponseEntity.notFound().build();
        }

        messageService.deleteMessage(messageId);
        log.info("User {} deleted message {}", username, messageId);

        return ResponseEntity.noContent().build();
    }
}