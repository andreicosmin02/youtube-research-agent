package com.youtube.research.controller;

import com.youtube.research.dto.ConversationDTO;
import com.youtube.research.dto.ConversationDetailDTO;
import com.youtube.research.dto.MessageDTO;
import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.Message;
import com.youtube.research.entity.User;
import com.youtube.research.service.ConversationService;
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
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;  // ADD THIS LINE

    public ConversationController(ConversationService conversationService, UserService userService) {
        this.conversationService = conversationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ConversationDTO> createConversation(
            @RequestBody CreateConversationRequest request,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to create conversation with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} creating conversation with title: {}", username, request.title());

        Long userId = userService.getUserByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Conversation conversation = conversationService.createConversation(userId, request.title());

        ConversationDTO dto = new ConversationDTO(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    record CreateConversationRequest(String title) {}

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getAllConversations(Authentication authentication) {
        if (authentication == null) {
            log.warn("Attempted to get conversations with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} retrieving conversations", username);

        Long userId = userService.getUserByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Conversation> conversations = conversationService.getConversationsByUser(userId);

        List<ConversationDTO> dtos = conversations.stream()
                .map(conv -> new ConversationDTO(
                        conv.getId(),
                        conv.getTitle(),
                        conv.getCreatedAt(),
                        conv.getUpdatedAt()
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConversationById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeMessages,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to get conversation {} with no authentication", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} retrieving conversation {}", username, id);

        Long userId = userService.getUserByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Conversation> userConversations = conversationService.getConversationsByUser(userId);

        Conversation conversation = userConversations.stream()
                .filter(conv -> conv.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (conversation == null) {
            log.warn("User {} attempted to access conversation {} they don't own", username, id);
            return ResponseEntity.notFound().build();
        }

        if (includeMessages) {
            List<Message> messages = conversationService.getConversationMessages(id);
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(msg -> new MessageDTO(
                            msg.getId(),
                            msg.getRole(),
                            msg.getContent(),
                            msg.getCreatedAt()
                    ))
                    .toList();

            ConversationDetailDTO dto = new ConversationDetailDTO(
                    conversation.getId(),
                    conversation.getTitle(),
                    conversation.getCreatedAt(),
                    conversation.getUpdatedAt(),
                    messageDTOs
            );
            return ResponseEntity.ok(dto);
        } else {
            ConversationDTO dto = new ConversationDTO(
                    conversation.getId(),
                    conversation.getTitle(),
                    conversation.getCreatedAt(),
                    conversation.getUpdatedAt()
            );
            return ResponseEntity.ok(dto);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConversationDTO> updateConversation(
            @PathVariable Long id,
            @RequestBody UpdateConversationRequest request,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to update conversation {} with no authentication", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} updating conversation {}", username, id);

        Long userId = userService.getUserByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Conversation> userConversations = conversationService.getConversationsByUser(userId);

        boolean userOwnsConversation = userConversations.stream()
                .anyMatch(conv -> conv.getId().equals(id));

        if (!userOwnsConversation) {
            log.warn("User {} attempted to update conversation {} they don't own", username, id);
            return ResponseEntity.notFound().build();
        }

        Conversation updatedConversation = conversationService.updateConversation(id, request.title());

        ConversationDTO dto = new ConversationDTO(
                updatedConversation.getId(),
                updatedConversation.getTitle(),
                updatedConversation.getCreatedAt(),
                updatedConversation.getUpdatedAt()
        );

        return ResponseEntity.ok(dto);
    }

    record UpdateConversationRequest(String title) {}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to delete conversation {} with no authentication", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        log.debug("User {} deleting conversation {}", username, id);

        Long userId = userService.getUserByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Conversation> userConversations = conversationService.getConversationsByUser(userId);

        boolean userOwnsConversation = userConversations.stream()
                .anyMatch(conv -> conv.getId().equals(id));

        if (!userOwnsConversation) {
            log.warn("User {} attempted to delete conversation {} they don't own", username, id);
            return ResponseEntity.notFound().build();
        }

        conversationService.deleteConversation(id);
        log.info("User {} deleted conversation {}", username, id);

        return ResponseEntity.noContent().build();
    }
}
