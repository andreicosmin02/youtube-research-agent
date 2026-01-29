package com.youtube.research.controller;

import com.youtube.research.service.ConversationService;
import com.youtube.research.service.MessageHandlerService;
import com.youtube.research.service.UserService;
import com.youtube.research.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
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

    /**
     * Send message with streaming response (Server-Sent Events)
     *
     * This endpoint returns tokens one by one as they are generated.
     * The response is in SSE format: data: {token}\n\n
     *
     * Special events:
     * - __METADATA__{json}__END_METADATA__ : Contains YouTube results metadata
     * - [DONE] : Indicates end of stream
     */
    @PostMapping(
            value = "/{conversationId}/send-message/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> sendMessageStreaming(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        // ---- Auth / validation ----
        if (authentication == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("Unauthorized")
                    .build());
        }

        final String username = authentication.getName();

        final String msg = (request == null) ? null : request.message();
        if (msg == null || msg.isBlank()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("Message cannot be empty")
                    .build());
        }

        final Long userId = userService.getUserByUsername(username)
                .map(User::getId)
                .orElse(null);

        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("User not found")
                    .build());
        }

        // Verify ownership
        final boolean ownsConversation = conversationService.getConversationsByUser(userId).stream()
                .anyMatch(c -> c.getId().equals(conversationId));

        if (!ownsConversation) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("Conversation not found")
                    .build());
        }

        // ---- Streaming ----
        final String META_PREFIX = "__METADATA__";
        final String META_SUFFIX = "__END_METADATA__";

        return messageHandlerService.handleMessageStreaming(conversationId, msg)
                // IMPORTANT: do NOT emit empty tokens (""), but KEEP " " tokens
                .filter(token -> token != null && !token.isEmpty())
                .map(token -> {
                    // Metadata event
                    if (token.startsWith(META_PREFIX)) {
                        int start = META_PREFIX.length();
                        int end = token.indexOf(META_SUFFIX, start);
                        String metadata = (end >= 0) ? token.substring(start, end) : token.substring(start);

                        return ServerSentEvent.<String>builder()
                                .event("metadata")
                                .data(metadata)
                                .build();
                    }

                    // Normal token event (leading spaces preserved)
                    return ServerSentEvent.<String>builder()
                            .event("token")
                            .data(token)
                            .build();
                })
                .concatWithValues(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build())
                .timeout(Duration.ofMinutes(5))
                .onErrorResume(e -> {
                    log.error("Streaming error: {}", e.getMessage(), e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(e.getMessage() == null ? "Streaming error" : e.getMessage())
                            .build());
                });
    }


    /**
     * Original non-streaming endpoint (kept for backward compatibility)
     */
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