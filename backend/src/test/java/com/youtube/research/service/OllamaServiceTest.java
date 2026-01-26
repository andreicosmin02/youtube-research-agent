package com.youtube.research.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtube.research.service.OllamaService.AgentDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OllamaServiceTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private OllamaService ollamaService;

    // ==================== Generate Response Validation Tests ====================

    @Test
    void shouldThrowExceptionWhenPromptIsEmpty() {
        assertThatThrownBy(() -> ollamaService.generateResponse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenPromptIsNull() {
        assertThatThrownBy(() -> ollamaService.generateResponse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenPromptIsOnlyWhitespace() {
        assertThatThrownBy(() -> ollamaService.generateResponse("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt cannot be empty");
    }

    // ==================== Decide Action Validation Tests ====================

    @Test
    void shouldThrowExceptionWhenUserMessageIsEmpty() {
        assertThatThrownBy(() -> ollamaService.decideAction("", "context"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenUserMessageIsNull() {
        assertThatThrownBy(() -> ollamaService.decideAction(null, "context"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenUserMessageIsOnlyWhitespace() {
        assertThatThrownBy(() -> ollamaService.decideAction("   ", "context"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }

    // ==================== AgentDecision Object Tests ====================

    @Test
    void shouldCreateAgentDecisionWithAllParameters() {
        // Arrange
        AgentDecision decision = new AgentDecision(
                "search",
                "machine learning",
                null,
                "viewCount",
                "video",
                "long",
                "User asked to search for videos"
        );

        // Assert
        assert decision.action.equals("search");
        assert decision.query.equals("machine learning");
        assert decision.order.equals("viewCount");
        assert decision.videoType.equals("video");
        assert decision.videoDuration.equals("long");
        assert decision.reasoning.equals("User asked to search for videos");
    }

    @Test
    void shouldCreateAgentDecisionForChatAction() {
        // Arrange
        AgentDecision decision = new AgentDecision(
                "chat",
                null,
                null,
                null,
                null,
                null,
                "General conversation"
        );

        // Assert
        assert decision.action.equals("chat");
        assert decision.query == null;
    }

    @Test
    void shouldCreateAgentDecisionForGetCommentsAction() {
        // Arrange
        AgentDecision decision = new AgentDecision(
                "get_comments",
                null,
                "dQw4w9WgXcQ",
                null,
                null,
                null,
                "User asked about comments"
        );

        // Assert
        assert decision.action.equals("get_comments");
        assert decision.videoId.equals("dQw4w9WgXcQ");
    }

    @Test
    void shouldCreateEmptyAgentDecision() {
        // Arrange & Act
        AgentDecision decision = new AgentDecision();

        // Assert
        assert decision.action == null;
        assert decision.query == null;
        assert decision.videoId == null;
    }
}