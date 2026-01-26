package com.youtube.research.service;

import com.youtube.research.dto.youtube.YouTubeVideoListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class MessageHandlerService {

    private final OllamaService ollamaService;
    private final MessageService messageService;
    private final YouTubeService youtubeService;
    private final CommentService commentService;

    public MessageHandlerService(OllamaService ollamaService,
                                 MessageService messageService,
                                 YouTubeService youtubeService,
                                 CommentService commentService) {
        this.ollamaService = ollamaService;
        this.messageService = messageService;
        this.youtubeService = youtubeService;
        this.commentService = commentService;
    }

    public String handleMessage(Long conversationId, String userMessage) throws IOException {
        // Validate input
        if (conversationId == null) {
            throw new IllegalArgumentException("Conversation ID cannot be null");
        }

        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("User message cannot be empty");
        }

        log.debug("Handling message for conversation {}: {}", conversationId, userMessage);

        // Save the user's message
        messageService.saveMessage(conversationId, "user", userMessage);

        // Build conversation context from previous messages
        String conversationContext = buildConversationContext(conversationId);

        // Decide what action to take
        OllamaService.AgentDecision decision = ollamaService.decideAction(userMessage, conversationContext);

        log.debug("Agent decision: {}", decision.action);

        // Route to appropriate YouTube endpoint and store results
        String youtubeResults = null;

        if ("search".equals(decision.action)) {
            var results = youtubeService.searchVideos(decision.query, 5);
            youtubeResults = results.toString();
        } else if ("search_advanced".equals(decision.action)) {
            var results = youtubeService.searchVideosAdvanced(decision.query, 5,
                    decision.order, decision.videoType, decision.videoDuration);
            youtubeResults = results.toString();
        } else if ("get_comments".equals(decision.action)) {
            var results = commentService.getVideoComments(decision.videoId, 20);
            youtubeResults = results.toString();
        } else if ("get_video".equals(decision.action)) {
            var results = youtubeService.getVideoById(decision.videoId);
            youtubeResults = results.toString();
        }

        // Get response from LLM
        String response = ollamaService.generateResponse(userMessage);

        // Save the assistant's response
        messageService.saveMessage(conversationId, "assistant", response);

        log.debug("Generated and saved response");

        return response;
    }

    private String buildConversationContext(Long conversationId) {
        // Get all previous messages for this conversation
        var messages = messageService.getConversationMessages(conversationId);

        StringBuilder context = new StringBuilder();
        for (var message : messages) {
            context.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }

        return context.toString();
    }
}