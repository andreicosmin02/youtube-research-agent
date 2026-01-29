package com.youtube.research.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtube.research.dto.youtube.YouTubeVideoListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageHandlerServiceStreamingTest {

    @Mock
    private OllamaService ollamaService;

    @Mock
    private MessageService messageService;

    @Mock
    private YouTubeService youtubeService;

    @Mock
    private CommentService commentService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageHandlerService messageHandlerService;

    @Test
    void shouldReturnErrorFluxWhenConversationIdIsNull() {
        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(null, "test message");

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Conversation ID and User message cannot be null or blank"))
                .verify();
    }

    @Test
    void shouldReturnErrorFluxWhenMessageIsEmpty() {
        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(1L, "");

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Conversation ID and User message cannot be null or blank"))
                .verify();
    }

    @Test
    void shouldReturnErrorFluxWhenMessageIsNull() {
        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(1L, null);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Conversation ID and User message cannot be null or blank"))
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

        // Use block to ensure the flux completes
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
    void shouldIncludeMetadataForSearchAction() throws Exception {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Find videos about Java";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "search", "Java", null, null, null, null, "Search request"
        );

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setItems(new ArrayList<>());

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.searchVideos("Java", 5))
                .thenReturn(mockResponse);

        when(ollamaService.generateStreamingResponse(anyString(), anyString()))
                .thenReturn(Flux.just("Here ", "are ", "results"));

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"type\":\"search_results\",\"videos\":[]}");

        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(conversationId, userMessage);

        // Assert - First element should be metadata
        StepVerifier.create(result)
                .expectNextMatches(s -> s.startsWith("__METADATA__"))
                .expectNext("Here ")
                .expectNext("are ")
                .expectNext("results")
                .verifyComplete();
    }

    @Test
    void shouldCallYouTubeServiceForSearchAction() throws IOException {
        // Arrange
        Long conversationId = 1L;
        String userMessage = "Search for Python tutorials";
        String query = "Python tutorials";

        OllamaService.AgentDecision mockDecision = new OllamaService.AgentDecision(
                "search", query, null, null, null, null, "Search"
        );

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setItems(new ArrayList<>());

        when(ollamaService.decideAction(eq(userMessage), anyString()))
                .thenReturn(mockDecision);

        when(youtubeService.searchVideos(query, 5))
                .thenReturn(mockResponse);

        when(ollamaService.generateStreamingResponse(anyString(), anyString()))
                .thenReturn(Flux.just("Found videos"));

        when(messageService.getConversationMessages(conversationId))
                .thenReturn(new ArrayList<>());

        // Act
        Flux<String> result = messageHandlerService.handleMessageStreaming(conversationId, userMessage);
        result.collectList().block();

        // Assert
        verify(youtubeService).searchVideos(query, 5);
    }

    @Test
    void shouldNotCallYouTubeServiceForChatAction() throws IOException {
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