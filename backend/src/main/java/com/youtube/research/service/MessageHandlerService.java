package com.youtube.research.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtube.research.dto.youtube.YouTubeCommentListResponse;
import com.youtube.research.dto.youtube.YouTubeCommentThread;
import com.youtube.research.dto.youtube.YouTubeVideo;
import com.youtube.research.dto.youtube.YouTubeVideoListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageHandlerService {

    private final OllamaService ollamaService;
    private final MessageService messageService;
    private final YouTubeService youtubeService;
    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public MessageHandlerService(OllamaService ollamaService,
                                 MessageService messageService,
                                 YouTubeService youtubeService,
                                 CommentService commentService,
                                 ObjectMapper objectMapper) {
        this.ollamaService = ollamaService;
        this.messageService = messageService;
        this.youtubeService = youtubeService;
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle message with streaming response (token by token)
     *
     * @param conversationId Conversation ID
     * @param userMessage User's message
     * @return Flux of response tokens
     */
    public Flux<String> handleMessageStreaming(Long conversationId, String userMessage) {
        log.debug("Handling streaming message for conversation {}: {}", conversationId, userMessage);

        // 1. Validate input
        if (conversationId == null || userMessage == null || userMessage.isBlank()) {
            return Flux.error(new IllegalArgumentException("Conversation ID and User message cannot be null or blank"));
        }

        // 2. Save the user's message (blocking operation wrapped in Mono)
        return Mono.fromCallable(() -> {
                    messageService.saveMessage(conversationId, "user", userMessage);
                    return userMessage;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(msg -> processMessageStreaming(conversationId, userMessage));
    }

    /**
     * Process the message and return streaming response
     */
    private Flux<String> processMessageStreaming(Long conversationId, String userMessage) {
        return Mono.fromCallable(() -> {
                    // Build conversation context
                    String conversationContext = buildConversationContext(conversationId);

                    // Decide what action to take
                    OllamaService.AgentDecision decision = ollamaService.decideAction(userMessage, conversationContext);
                    log.debug("Agent decision: {}", decision.action);

                    // Fetch YouTube results if needed
                    Object youtubeResults = null;
                    if (requiresYouTubeAction(decision.action)) {
                        youtubeResults = fetchYouTubeResults(decision);
                    }

                    // Return context for streaming
                    return new StreamingContext(decision, youtubeResults, conversationContext, userMessage);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(ctx -> generateStreamingResponse(conversationId, ctx));
    }

    /**
     * Generate streaming response and save final result
     */
    private Flux<String> generateStreamingResponse(Long conversationId, StreamingContext ctx) {
        String prompt = buildPromptForAction(ctx.userMessage, ctx.decision, ctx.youtubeResults);

        // Accumulate the full response for saving
        StringBuilder fullResponse = new StringBuilder();

        // If there are YouTube results, first send metadata, then stream the analysis
        Flux<String> metadataFlux = Flux.empty();
        if (ctx.youtubeResults != null) {
            try {
                String metadata = buildMetadataJson(ctx.decision, ctx.youtubeResults);
                metadataFlux = Flux.just("__METADATA__" + metadata + "__END_METADATA__");
            } catch (JsonProcessingException e) {
                log.error("Error building metadata JSON", e);
            }
        }

        // Stream the LLM response
        Flux<String> responseFlux = ollamaService.generateStreamingResponse(prompt, ctx.conversationContext)
                .doOnNext(token -> fullResponse.append(token))
                .doOnComplete(() -> {
                    // Save the complete response when streaming is done
                    try {
                        String assistantMessage = buildStructuredResponse(ctx.decision, fullResponse.toString(), ctx.youtubeResults);
                        messageService.saveMessage(conversationId, "assistant", assistantMessage);
                        log.debug("Saved complete streaming response for conversation {}", conversationId);
                    } catch (Exception e) {
                        log.error("Error saving streaming response: {}", e.getMessage());
                    }
                })
                .doOnError(e -> log.error("Streaming error: {}", e.getMessage()));

        return Flux.concat(metadataFlux, responseFlux);
    }

    /**
     * Build metadata JSON for YouTube results
     */
    private String buildMetadataJson(OllamaService.AgentDecision decision, Object youtubeResults) throws JsonProcessingException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", mapActionToType(decision.action));

        if (youtubeResults instanceof YouTubeVideoListResponse) {
            YouTubeVideoListResponse videoRes = (YouTubeVideoListResponse) youtubeResults;
            metadata.put("query", decision.query);
            metadata.put("videos", cleanVideoList(videoRes.getItems()));
        } else if (youtubeResults instanceof YouTubeCommentListResponse) {
            YouTubeCommentListResponse commentRes = (YouTubeCommentListResponse) youtubeResults;
            metadata.put("video_id", decision.videoId);
            metadata.put("comments", cleanCommentList(commentRes.getItems()));
        }

        return objectMapper.writeValueAsString(metadata);
    }

    /**
     * Build prompt based on action type
     */
    private String buildPromptForAction(String userMessage, OllamaService.AgentDecision decision, Object youtubeResults) {
        return switch (decision.action) {
            case "search", "search_advanced" -> buildSearchPrompt(userMessage, decision, youtubeResults);
            case "get_comments" -> buildCommentAnalysisPrompt(userMessage, youtubeResults);
            case "get_video" -> buildVideoDetailsPrompt(userMessage, youtubeResults);
            default -> userMessage;
        };
    }

    /**
     * Original non-streaming handleMessage method (kept for backward compatibility)
     */
    public String handleMessage(Long conversationId, String userMessage) throws IOException {
        log.debug("Handling message for conversation {}: {}", conversationId, userMessage);

        // 1. Validate input
        if (conversationId == null || userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Conversation ID and User message cannot be null or blank");
        }

        // 2. Save the user's message
        messageService.saveMessage(conversationId, "user", userMessage);

        // 3. Build conversation context
        String conversationContext = buildConversationContext(conversationId);

        // 4. Decide what action to take
        OllamaService.AgentDecision decision = ollamaService.decideAction(userMessage, conversationContext);
        log.debug("Agent decision: {}", decision.action);

        // 5. Fetch YouTube results IF NEEDED
        Object youtubeResults = null;
        if (requiresYouTubeAction(decision.action)) {
            youtubeResults = fetchYouTubeResults(decision);
        }

        // 6. Generate LLM response BASED ON ACTUAL YOUTUBE DATA
        String llmResponse = generateResponseFromYouTubeData(userMessage, decision, youtubeResults, conversationContext);

        // 7. Build structured response
        String assistantMessage = buildStructuredResponse(decision, llmResponse, youtubeResults);
        messageService.saveMessage(conversationId, "assistant", assistantMessage);

        log.debug("Generated and saved structured assistant response");
        return assistantMessage;
    }

    private boolean requiresYouTubeAction(String action) {
        return "search".equals(action) || "search_advanced".equals(action) ||
                "get_video".equals(action) || "get_comments".equals(action);
    }

    private Object fetchYouTubeResults(OllamaService.AgentDecision decision) throws IOException {
        return switch (decision.action) {
            case "search" -> youtubeService.searchVideos(decision.query, 5);
            case "search_advanced" -> youtubeService.searchVideosAdvanced(
                    decision.query, 5, decision.order, decision.videoType, decision.videoDuration);
            case "get_comments" -> commentService.getVideoComments(decision.videoId, 20);
            case "get_video" -> youtubeService.getVideoById(decision.videoId);
            default -> null;
        };
    }

    private String generateResponseFromYouTubeData(String userMessage, OllamaService.AgentDecision decision,
                                                   Object youtubeResults, String conversationContext) throws IOException {
        String prompt = switch (decision.action) {
            case "search", "search_advanced" -> buildSearchPrompt(userMessage, decision, youtubeResults);
            case "get_comments" -> buildCommentAnalysisPrompt(userMessage, youtubeResults);
            case "get_video" -> buildVideoDetailsPrompt(userMessage, youtubeResults);
            default -> userMessage;
        };

        return ollamaService.generateResponse(prompt, conversationContext);
    }

    private String buildSearchPrompt(String userMessage, OllamaService.AgentDecision decision, Object youtubeResults) {
        if (!(youtubeResults instanceof YouTubeVideoListResponse)) {
            return userMessage;
        }

        YouTubeVideoListResponse response = (YouTubeVideoListResponse) youtubeResults;
        List<YouTubeVideo> videos = response.getItems();

        if (videos == null || videos.isEmpty()) {
            return String.format("The user asked: '%s' but no videos were found. Please inform them.", userMessage);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("The user asked: \"").append(userMessage).append("\"\n\n");
        prompt.append("I found ").append(videos.size()).append(" relevant videos on YouTube:\n\n");

        for (int i = 0; i < videos.size(); i++) {
            YouTubeVideo video = videos.get(i);
            prompt.append(i + 1).append(". ");

            if (video.getSnippet() != null) {
                prompt.append("Title: ").append(video.getSnippet().getTitle()).append("\n");
                prompt.append("   Channel: ").append(video.getSnippet().getChannelTitle()).append("\n");
                String desc = video.getSnippet().getDescription();
                if (desc != null && desc.length() > 150) {
                    desc = desc.substring(0, 150) + "...";
                }
                prompt.append("   Description: ").append(desc).append("\n");
            }

            if (video.getStatistics() != null) {
                prompt.append("   Views: ").append(video.getStatistics().getViewCount()).append("\n");
                prompt.append("   Likes: ").append(video.getStatistics().getLikeCount()).append("\n");
                prompt.append("   Comments: ").append(video.getStatistics().getCommentCount()).append("\n");
            }

            prompt.append("   Video ID: ").append(video.getId()).append("\n\n");
        }

        prompt.append("Based on these search results, provide a helpful analysis and recommendations for the user.");

        return prompt.toString();
    }

    private String buildCommentAnalysisPrompt(String userMessage, Object youtubeResults) {
        if (!(youtubeResults instanceof YouTubeCommentListResponse)) {
            return userMessage;
        }

        YouTubeCommentListResponse response = (YouTubeCommentListResponse) youtubeResults;
        List<YouTubeCommentThread> comments = response.getItems();

        if (comments == null || comments.isEmpty()) {
            return String.format("The user asked about comments for video %s but no comments were found.", response.getVideoId());
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("The user asked: \"").append(userMessage).append("\"\n\n");
        prompt.append("I retrieved ").append(comments.size()).append(" top comments from the video:\n\n");

        for (int i = 0; i < comments.size(); i++) {
            YouTubeCommentThread thread = comments.get(i);
            if (thread.getTopLevelComment() != null) {
                YouTubeCommentThread.CommentData comment = thread.getTopLevelComment();
                prompt.append(i + 1).append(". ");
                prompt.append("Author: ").append(comment.getAuthorDisplayName()).append("\n");
                prompt.append("   Likes: ").append(comment.getLikeCount()).append("\n");
                prompt.append("   Comment: \"").append(comment.getTextDisplay()).append("\"\n\n");
            }
        }

        prompt.append("Based on these comments, provide insights about what people think about this video. ");
        prompt.append("Identify common themes, sentiment, and key discussion points.");

        return prompt.toString();
    }

    private String buildVideoDetailsPrompt(String userMessage, Object youtubeResults) {
        if (youtubeResults == null) {
            return userMessage;
        }

        YouTubeVideo video = null;

        if (youtubeResults instanceof YouTubeVideo) {
            video = (YouTubeVideo) youtubeResults;
        } else if (youtubeResults instanceof YouTubeVideoListResponse) {
            YouTubeVideoListResponse response = (YouTubeVideoListResponse) youtubeResults;
            if (response.getItems() != null && !response.getItems().isEmpty()) {
                video = response.getItems().get(0);
            }
        }

        if (video == null) {
            return userMessage;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("The user asked: \"").append(userMessage).append("\"\n\n");

        if (video.getSnippet() != null) {
            YouTubeVideo.Snippet snippet = video.getSnippet();
            prompt.append("Video: ").append(snippet.getTitle()).append("\n");
            prompt.append("Channel: ").append(snippet.getChannelTitle()).append("\n");
            prompt.append("Published: ").append(snippet.getPublishedAt()).append("\n");
            prompt.append("Description: ").append(snippet.getDescription()).append("\n\n");
        }

        if (video.getStatistics() != null) {
            prompt.append("Statistics:\n");
            prompt.append("- Views: ").append(video.getStatistics().getViewCount()).append("\n");
            prompt.append("- Likes: ").append(video.getStatistics().getLikeCount()).append("\n");
            prompt.append("- Comments: ").append(video.getStatistics().getCommentCount()).append("\n\n");
        }

        if (video.getContentDetails() != null) {
            prompt.append("Duration: ").append(video.getContentDetails().getDuration()).append("\n");
            prompt.append("Definition: ").append(video.getContentDetails().getDefinition()).append("\n\n");
        }

        prompt.append("Provide a detailed analysis of this video based on the information above.");

        return prompt.toString();
    }

    private String buildStructuredResponse(OllamaService.AgentDecision decision, String llmResponse,
                                           Object youtubeResults) throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("type", mapActionToType(decision.action));
        response.put("text", llmResponse);

        if (youtubeResults instanceof YouTubeVideoListResponse) {
            YouTubeVideoListResponse videoRes = (YouTubeVideoListResponse) youtubeResults;
            response.put("query", decision.query);
            response.put("videos", cleanVideoList(videoRes.getItems()));
        } else if (youtubeResults instanceof YouTubeCommentListResponse) {
            YouTubeCommentListResponse commentRes = (YouTubeCommentListResponse) youtubeResults;
            response.put("video_id", decision.videoId);
            response.put("comments", cleanCommentList(commentRes.getItems()));
        }

        return objectMapper.writeValueAsString(response);
    }

    private List<Map<String, Object>> cleanVideoList(List<YouTubeVideo> videos) {
        if (videos == null) return java.util.Collections.emptyList();

        return videos.stream()
                .map(this::cleanSingleVideo)
                .collect(Collectors.toList());
    }

    private Map<String, Object> cleanSingleVideo(YouTubeVideo video) {
        Map<String, Object> clean = new HashMap<>();

        clean.put("id", video.getId());

        if (video.getSnippet() != null) {
            YouTubeVideo.Snippet s = video.getSnippet();
            clean.put("title", s.getTitle());
            clean.put("channel", s.getChannelTitle());

            String desc = s.getDescription();
            if (desc != null && desc.length() > 200) {
                desc = desc.substring(0, 200) + "...";
            }
            clean.put("description", desc);

            if (s.getThumbnails() != null && s.getThumbnails().getMedium() != null) {
                clean.put("thumbnail", s.getThumbnails().getMedium().getUrl());
            }
        }

        if (video.getStatistics() != null) {
            Map<String, String> stats = new HashMap<>();
            stats.put("views", video.getStatistics().getViewCount());
            stats.put("likes", video.getStatistics().getLikeCount());
            stats.put("comments", video.getStatistics().getCommentCount());
            clean.put("stats", stats);
        }

        return clean;
    }

    private List<Map<String, Object>> cleanCommentList(List<YouTubeCommentThread> items) {
        if (items == null) return java.util.Collections.emptyList();

        return items.stream()
                .map(this::cleanComment)
                .collect(Collectors.toList());
    }

    private Map<String, Object> cleanComment(YouTubeCommentThread thread) {
        Map<String, Object> clean = new HashMap<>();

        if (thread.getTopLevelComment() != null) {
            YouTubeCommentThread.CommentData comment = thread.getTopLevelComment();
            clean.put("author", comment.getAuthorDisplayName());
            clean.put("text", comment.getTextDisplay());
            clean.put("likes", comment.getLikeCount());
            clean.put("published", comment.getPublishedAt());
        }

        return clean;
    }

    private String mapActionToType(String action) {
        return switch (action) {
            case "search", "search_advanced" -> "search_results";
            case "get_comments" -> "comments";
            case "get_video" -> "video_details";
            case "chat" -> "text";
            default -> "text";
        };
    }

    private String buildConversationContext(Long conversationId) {
        var messages = messageService.getConversationMessages(conversationId);
        StringBuilder context = new StringBuilder();
        int tokenCount = 0;
        final int MAX_TOKENS = 2000;

        for (int i = messages.size() - 1; i >= 0; i--) {
            var msg = messages.get(i);
            String text = extractTextFromMessage(msg.getContent());
            String line = msg.getRole() + ": " + text + "\n";
            int tokens = line.length() / 4;

            if (tokenCount + tokens > MAX_TOKENS) break;

            context.insert(0, line);
            tokenCount += tokens;
        }

        return context.toString();
    }

    private String extractTextFromMessage(String jsonContent) {
        try {
            Map<String, Object> content = objectMapper.readValue(jsonContent, Map.class);
            if (content.containsKey("text")) {
                return content.get("text").toString();
            }
            return jsonContent;
        } catch (IOException e) {
            return jsonContent;
        }
    }

    /**
     * Context holder for streaming operations
     */
    private static class StreamingContext {
        final OllamaService.AgentDecision decision;
        final Object youtubeResults;
        final String conversationContext;
        final String userMessage;

        StreamingContext(OllamaService.AgentDecision decision, Object youtubeResults,
                         String conversationContext, String userMessage) {
            this.decision = decision;
            this.youtubeResults = youtubeResults;
            this.conversationContext = conversationContext;
            this.userMessage = userMessage;
        }
    }
}