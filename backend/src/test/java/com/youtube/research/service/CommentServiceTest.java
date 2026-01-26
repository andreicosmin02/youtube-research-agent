package com.youtube.research.service;

import com.youtube.research.config.YouTubeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private YouTubeConfig youtubeConfig;

    @InjectMocks
    private CommentService commentService;

    // ==================== Video ID Validation Tests ====================

    @Test
    void shouldThrowExceptionWhenVideoIdIsEmpty() {
        assertThatThrownBy(() -> commentService.getVideoComments("", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Video ID cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenVideoIdIsNull() {
        assertThatThrownBy(() -> commentService.getVideoComments(null, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Video ID cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenVideoIdExceeds100Characters() {
        String longVideoId = "a".repeat(101);
        assertThatThrownBy(() -> commentService.getVideoComments(longVideoId, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Video ID cannot exceed 100 characters");
    }

    // ==================== Max Results Validation Tests ====================

    @Test
    void shouldThrowExceptionWhenMaxResultsIsZero() {
        assertThatThrownBy(() -> commentService.getVideoComments("dQw4w9WgXcQ", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxResults must be between 1 and 100");
    }

    @Test
    void shouldThrowExceptionWhenMaxResultsIsNegative() {
        assertThatThrownBy(() -> commentService.getVideoComments("dQw4w9WgXcQ", -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxResults must be between 1 and 100");
    }

    @Test
    void shouldThrowExceptionWhenMaxResultsExceeds100() {
        assertThatThrownBy(() -> commentService.getVideoComments("dQw4w9WgXcQ", 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxResults must be between 1 and 100");
    }

    @Test
    void shouldAcceptMaxResultsOf1() {
        assertThatThrownBy(() -> commentService.getVideoComments("dQw4w9WgXcQ", 1))
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptMaxResultsOf100() {
        assertThatThrownBy(() -> commentService.getVideoComments("dQw4w9WgXcQ", 100))
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(IllegalArgumentException.class);
    }

    // ==================== Parent Comment ID Validation Tests ====================

    @Test
    void shouldThrowExceptionWhenParentCommentIdIsEmpty() {
        assertThatThrownBy(() -> commentService.getCommentReplies("", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent comment ID cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenParentCommentIdIsNull() {
        assertThatThrownBy(() -> commentService.getCommentReplies(null, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent comment ID cannot be empty");
    }
}