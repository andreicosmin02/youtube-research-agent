package com.youtube.research.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtube.research.dto.youtube.YouTubeVideoListResponse;
import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.Message;
import com.youtube.research.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageHandlerServiceTest {

    @Mock
    private OllamaService ollamaService;

    @Mock
    private MessageService messageService;

    @Mock
    private YouTubeService youtubeService;

    @Mock
    private CommentService commentService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MessageHandlerService messageHandlerService;

    @Test
    void shouldSaveUserMessageWhenHandlingMessage() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat", null, null, null, null, null, "General question"
        );

        when(ollamaService.decideAction(anyString(), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Response");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(messageService).saveMessage(conversationId, "user", userMessage);
    }

    @Test
    void shouldCallOllamaServiceToGenerateResponse() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";
        String expectedResponse = "I'll search for machine learning videos for you";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat", null, null, null, null, null, "General question"
        );

        when(ollamaService.decideAction(anyString(), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn(expectedResponse);

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        String result = messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        assertThat(result).contains(expectedResponse);
        verify(ollamaService).generateResponse(anyString(), anyString());
    }

    @Test
    void shouldSaveAssistantResponseMessage() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";
        String assistantResponse = "I'll search for machine learning videos for you";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat", null, null, null, null, null, "General question"
        );

        when(ollamaService.decideAction(anyString(), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn(assistantResponse);

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        String result = messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        assertThat(result).contains(assistantResponse);
        verify(messageService).saveMessage(eq(conversationId), eq("assistant"), anyString());
    }

    @Test
    void shouldThrowExceptionWhenUserMessageIsEmpty() {
        // Arrange
        Long conversationId = 1L;
        String emptyMessage = "";

        // Act & Assert
        assertThatThrownBy(() -> messageHandlerService.handleMessage(conversationId, emptyMessage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    void shouldThrowExceptionWhenConversationIdIsNull() {
        // Arrange
        String userMessage = "Find me videos about machine learning";

        // Act & Assert
        assertThatThrownBy(() -> messageHandlerService.handleMessage(null, userMessage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    void shouldCallDecideActionToGetAgentDecision() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat",
                null,
                null,
                null,
                null,
                null,
                "General question"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Here are some videos");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(ollamaService).decideAction(eq(userMessage), anyString());
    }

    @Test
    void shouldCallYouTubeSearchWhenDecisionIsSearch() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";
        String searchQuery = "machine learning";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "search",
                searchQuery,
                null,
                null,
                null,
                null,
                "User wants to search"
        );

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setItems(new ArrayList<>());

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.searchVideos(eq(searchQuery), eq(5)))
                .thenReturn(mockResponse);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Here are some videos");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(youtubeService).searchVideos(eq(searchQuery), eq(5));
    }

    @Test
    void shouldCallYouTubeSearchAdvancedWhenDecisionIsSearchAdvanced() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find long videos about machine learning sorted by views";
        String searchQuery = "machine learning";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "search_advanced",
                searchQuery,
                null,
                "viewCount",
                "video",
                "long",
                "User wants advanced search with filters"
        );

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setItems(new ArrayList<>());

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.searchVideosAdvanced(eq(searchQuery), eq(5),
                eq("viewCount"), eq("video"), eq("long")))
                .thenReturn(mockResponse);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Here are long videos");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(youtubeService).searchVideosAdvanced(eq(searchQuery), eq(5),
                eq("viewCount"), eq("video"), eq("long"));
    }

    @Test
    void shouldCallCommentServiceWhenDecisionIsGetComments() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "What are people saying about this video?";
        String videoId = "dQw4w9WgXcQ";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "get_comments",
                null,
                videoId,
                null,
                null,
                null,
                "User wants to see comments"
        );

        com.youtube.research.dto.youtube.YouTubeCommentListResponse mockResponse =
                new com.youtube.research.dto.youtube.YouTubeCommentListResponse();
        mockResponse.setItems(new ArrayList<>());

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(commentService.getVideoComments(eq(videoId), eq(20)))
                .thenReturn(mockResponse);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Here's what people are saying");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(commentService).getVideoComments(eq(videoId), eq(20));
    }

    @Test
    void shouldCallYouTubeGetVideoWhenDecisionIsGetVideo() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Tell me more about that video";
        String videoId = "dQw4w9WgXcQ";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "get_video",
                null,
                videoId,
                null,
                null,
                null,
                "User wants video details"
        );

        com.google.api.services.youtube.model.Video mockVideo =
                new com.google.api.services.youtube.model.Video();
        mockVideo.setId(videoId);

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.getVideoById(eq(videoId)))
                .thenReturn(mockVideo);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Here are the video details");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(youtubeService).getVideoById(eq(videoId));
    }

    @Test
    void shouldNotCallYouTubeWhenDecisionIsChat() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "What is machine learning?";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat",
                null,
                null,
                null,
                null,
                null,
                "General knowledge question"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Machine learning is a subset of AI...");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(youtubeService, never()).searchVideos(anyString(), anyInt());
        verify(youtubeService, never()).searchVideosAdvanced(anyString(), anyInt(), anyString(), anyString(), anyString());
        verify(youtubeService, never()).getVideoById(anyString());
        verify(commentService, never()).getVideoComments(anyString(), anyInt());
    }

    @Test
    void shouldStoreYouTubeResultsWhenSearching() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";
        String searchQuery = "machine learning";

        YouTubeVideoListResponse mockSearchResults = new YouTubeVideoListResponse();
        mockSearchResults.setKind("youtube#videoListResponse");
        mockSearchResults.setItems(new ArrayList<>());

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "search",
                searchQuery,
                null,
                null,
                null,
                null,
                "User wants to search"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.searchVideos(eq(searchQuery), eq(5)))
                .thenReturn(mockSearchResults);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Here are some videos");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(youtubeService).searchVideos(eq(searchQuery), eq(5));
        verify(messageService).saveMessage(eq(conversationId), eq("assistant"), anyString());
    }

    @Test
    void shouldBuildContextFromPreviousMessages() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat",
                null,
                null,
                null,
                null,
                null,
                "User question"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateResponse(anyString(), anyString()))
                .thenReturn("Response");

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        verify(ollamaService).decideAction(eq(userMessage), anyString());
    }

    // ==================== Streaming Tests ====================

    @Test
    void shouldReturnErrorFluxWhenConversationIdIsNullForStreaming() {
        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(null, "test message");

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("cannot be null or blank"))
                .verify();
    }

    @Test
    void shouldReturnErrorFluxWhenMessageIsEmptyForStreaming() {
        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(1L, "");

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("cannot be null or blank"))
                .verify();
    }

    @Test
    void shouldReturnErrorFluxWhenMessageIsNullForStreaming() {
        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(1L, null);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("cannot be null or blank"))
                .verify();
    }

    @Test
    void shouldSaveUserMessageBeforeStreaming() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "What is machine learning?";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat", null, null, null, null, null, "General question"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateStreamingResponse(anyString(), anyString()))
                .thenReturn(Flux.just("Machine ", "learning ", "is..."));

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(conversationId, userMessage);

        // Collect all tokens to complete the stream
        result.collectList().block();

        // Assert
        verify(messageService).saveMessage(conversationId, "user", userMessage);
    }

    @Test
    void shouldStreamTokensForChatAction() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Hello!";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat", null, null, null, null, null, "Greeting"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateStreamingResponse(anyString(), anyString()))
                .thenReturn(Flux.just("Hi", " there", "!"));

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(conversationId, userMessage);

        // Assert
        StepVerifier.create(result)
                .expectNext("Hi")
                .expectNext(" there")
                .expectNext("!")
                .verifyComplete();
    }

    @Test
    void shouldNotCallYouTubeServiceForChatActionInStreaming() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "What is the weather?";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat", null, null, null, null, null, "General question"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateStreamingResponse(anyString(), anyString()))
                .thenReturn(Flux.just("I don't know"));

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(conversationId, userMessage);
        result.collectList().block();

        // Assert
        verify(youtubeService, never()).searchVideos(anyString(), anyInt());
        verify(commentService, never()).getVideoComments(anyString(), anyInt());
    }
}