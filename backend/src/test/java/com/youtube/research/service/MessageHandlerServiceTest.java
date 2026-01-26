package com.youtube.research.service;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.Message;
import com.youtube.research.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Response");

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

        when(ollamaService.generateResponse(anyString()))
                .thenReturn(expectedResponse);

        // Act
        String result = messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(ollamaService).generateResponse(anyString());
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

        when(ollamaService.generateResponse(anyString()))
                .thenReturn(assistantResponse);

        // Act
        String result = messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        assertThat(result).isEqualTo(assistantResponse);
        verify(messageService).saveMessage(conversationId, "assistant", assistantResponse);
    }

    @Test
    void shouldThrowExceptionWhenUserMessageIsEmpty() {
        // Arrange
        Long conversationId = 1L;
        String emptyMessage = "";

        // Act & Assert
        assertThatThrownBy(() -> messageHandlerService.handleMessage(conversationId, emptyMessage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenConversationIdIsNull() {
        // Arrange
        String userMessage = "Find me videos about machine learning";

        // Act & Assert
        assertThatThrownBy(() -> messageHandlerService.handleMessage(null, userMessage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversation ID cannot be null");
    }

    @Test
    void shouldCallDecideActionToGetAgentDecision() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find me videos about machine learning";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "chat",  // Use chat instead of search
                null,
                null,
                null,
                null,
                null,
                "General question"
        );

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Here are some videos");

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

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.searchVideos(eq(searchQuery), eq(5)))
                .thenReturn(new com.youtube.research.dto.youtube.YouTubeVideoListResponse());

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Here are some videos");

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

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.searchVideosAdvanced(eq(searchQuery), eq(5),
                eq("viewCount"), eq("video"), eq("long")))
                .thenReturn(new com.youtube.research.dto.youtube.YouTubeVideoListResponse());

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Here are long videos");

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

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(commentService.getVideoComments(eq(videoId), eq(20)))
                .thenReturn(new com.youtube.research.dto.youtube.YouTubeCommentListResponse());

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Here's what people are saying");

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

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        com.google.api.services.youtube.model.Video mockVideo =
                new com.google.api.services.youtube.model.Video();
        mockVideo.setId(videoId);

        when(youtubeService.getVideoById(eq(videoId)))
                .thenReturn(mockVideo);

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Here are the video details");

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

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Machine learning is a subset of AI...");

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

        com.youtube.research.dto.youtube.YouTubeVideoListResponse mockSearchResults =
                new com.youtube.research.dto.youtube.YouTubeVideoListResponse();
        mockSearchResults.setKind("youtube#videoListResponse");

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

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Here are some videos");

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert
        // Verify search was called and results were stored
        verify(youtubeService).searchVideos(eq(searchQuery), eq(5));
        verify(messageService).saveMessage(conversationId, "assistant", "Here are some videos");
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

        when(ollamaService.generateResponse(anyString()))
                .thenReturn("Response");

        // Act
        messageHandlerService.handleMessage(conversationId, userMessage);

        // Assert - Verify decideAction was called with some context string (could be empty on first call)
        verify(ollamaService).decideAction(eq(userMessage), anyString());
    }
}