//package com.youtube.research.controller;
//
//import com.youtube.research.service.ConversationService;
//import com.youtube.research.service.MessageHandlerService;
//import com.youtube.research.service.UserService;
//import com.youtube.research.entity.User;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/conversations")
//public class MessageHandlerController {
//
//    private final MessageHandlerService messageHandlerService;
//    private final UserService userService;
//    private final ConversationService conversationService;
//
//    public MessageHandlerController(MessageHandlerService messageHandlerService,
//                                    UserService userService,
//                                    ConversationService conversationService) {
//        this.messageHandlerService = messageHandlerService;
//        this.userService = userService;
//        this.conversationService = conversationService;
//    }
//
//    @PostMapping("/{conversationId}/send-message")
//    public ResponseEntity<SendMessageResponse> sendMessage(
//            @PathVariable Long conversationId,
//            @RequestBody SendMessageRequest request,
//            Authentication authentication) throws IOException {
//
//        if (authentication == null) {
//            log.warn("Attempted to send message with no authentication");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        String username = authentication.getName();
//        log.debug("User {} sending message to conversation {}", username, conversationId);
//
//        Long userId = userService.getUserByUsername(username)
//                .map(User::getId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//        // Verify user owns this conversation
//        List<com.youtube.research.entity.Conversation> userConversations =
//                conversationService.getConversationsByUser(userId);
//
//        boolean userOwnsConversation = userConversations.stream()
//                .anyMatch(conv -> conv.getId().equals(conversationId));
//
//        if (!userOwnsConversation) {
//            log.warn("User {} attempted to message conversation {} they don't own", username, conversationId);
//            return ResponseEntity.notFound().build();
//        }
//
//        try {
//            // Handle the message and get response
//            String response = messageHandlerService.handleMessage(conversationId, request.message());
//            return ResponseEntity.ok(new SendMessageResponse(response));
//        } catch (IllegalArgumentException e) {
//            log.warn("Validation error: {}", e.getMessage());
//            throw e;  // Let GlobalExceptionHandler catch it
//        }
//    }
//
//    record SendMessageRequest(String message) {}
//    record SendMessageResponse(String response) {}
//}

package com.youtube.research.controller;

import com.youtube.research.service.ConversationService;
import com.youtube.research.service.MessageHandlerService;
import com.youtube.research.service.UserService;
import com.youtube.research.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/conversations")
public class MessageHandlerController {

    private final MessageHandlerService messageHandlerService;
    private final UserService userService;
    private final ConversationService conversationService;

    public MessageHandlerController(MessageHandlerService messageHandlerService,
                                    UserService userService,
                                    ConversationService conversationService) {
        this.messageHandlerService = messageHandlerService;
        this.userService = userService;
        this.conversationService = conversationService;
    }

    @PostMapping("/{conversationId}/send-message")
    public Mono<ResponseEntity<SendMessageResponse>> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to send message with no authentication");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String username = authentication.getName();
        log.debug("User {} sending message to conversation {}", username, conversationId);

        Long userId = userService.getUserByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify user owns this conversation
        List<com.youtube.research.entity.Conversation> userConversations =
                conversationService.getConversationsByUser(userId);

        boolean userOwnsConversation = userConversations.stream()
                .anyMatch(conv -> conv.getId().equals(conversationId));

        if (!userOwnsConversation) {
            log.warn("User {} attempted to message conversation {} they don't own", username, conversationId);
            return Mono.just(ResponseEntity.notFound().build());
        }

        try {
            // Run blocking service call on a separate thread pool
            return Mono.fromCallable(() -> messageHandlerService.handleMessage(conversationId, request.message()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(response -> ResponseEntity.ok(new SendMessageResponse(response)))
                    .onErrorResume(IllegalArgumentException.class, e -> {
                        log.warn("Validation error: {}", e.getMessage());
                        return Mono.error(e);
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    record SendMessageRequest(String message) {}
    record SendMessageResponse(String response) {}
}