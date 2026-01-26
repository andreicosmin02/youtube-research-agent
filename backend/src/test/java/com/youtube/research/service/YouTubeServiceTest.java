package com.youtube.research.service;

import com.youtube.research.config.YouTubeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class YouTubeServiceTest {

    @Mock
    private YouTubeConfig youtubeConfig;

    @InjectMocks
    private YouTubeService youtubeService;

    // Search Query Validation Tests

    @Test
    void shouldThrowExceptionWhenSearchQueryIsEmpty() {
        assertThatThrownBy(() -> youtubeService.searchVideos("", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search query cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenSearchQueryIsNull() {
        assertThatThrownBy(() -> youtubeService.searchVideos(null, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search query cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenSearchQueryExceeds256Characters() {
        String longQuery = "a".repeat(257);
        assertThatThrownBy(() -> youtubeService.searchVideos(longQuery, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search query cannot exceed 256 characters");
    }

    // Max Results Validation Tests

    @Test
    void shouldThrowExceptionWhenMaxResultsIsZero() {
        assertThatThrownBy(() -> youtubeService.searchVideos("machine learning", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxResults must be between 1 and 50");
    }

    @Test
    void shouldThrowExceptionWhenMaxResultsIsNegative() {
        assertThatThrownBy(() -> youtubeService.searchVideos("machine learning", -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxResults must be between 1 and 50");
    }

    @Test
    void shouldThrowExceptionWhenMaxResultsExceeds50() {
        assertThatThrownBy(() -> youtubeService.searchVideos("machine learning", 51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxResults must be between 1 and 50");
    }

    // Video ID Validation Tests

    @Test
    void shouldThrowExceptionWhenVideoIdIsEmpty() {
        assertThatThrownBy(() -> youtubeService.getVideoById(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Video ID cannot be empty");
    }

    @Test
    void shouldThrowExceptionWhenVideoIdIsNull() {
        assertThatThrownBy(() -> youtubeService.getVideoById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Video ID cannot be empty");
    }

    // Advanced Search Validation Tests

    @Test
    void shouldThrowExceptionWhenOrderIsInvalid() {
        assertThatThrownBy(() -> youtubeService.searchVideosAdvanced("machine learning", 5, "invalid", "any", "any"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid order parameter");
    }

    @Test
    void shouldThrowExceptionWhenVideoTypeIsInvalid() {
        assertThatThrownBy(() -> youtubeService.searchVideosAdvanced("machine learning", 5, "relevance", "invalid", "any"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid videoType parameter");
    }

    @Test
    void shouldThrowExceptionWhenVideoDurationIsInvalid() {
        assertThatThrownBy(() -> youtubeService.searchVideosAdvanced("machine learning", 5, "relevance", "any", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid videoDuration parameter");
    }

    @Test
    void shouldAcceptValidOrderParameter() {
        assertThatThrownBy(() -> youtubeService.searchVideosAdvanced("machine learning", 5, "viewCount", "any", "any"))
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptValidVideoTypeParameter() {
        assertThatThrownBy(() -> youtubeService.searchVideosAdvanced("machine learning", 5, "relevance", "video", "any"))
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptValidVideoDurationParameter() {
        assertThatThrownBy(() -> youtubeService.searchVideosAdvanced("machine learning", 5, "relevance", "any", "long"))
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(IllegalArgumentException.class);
    }
}