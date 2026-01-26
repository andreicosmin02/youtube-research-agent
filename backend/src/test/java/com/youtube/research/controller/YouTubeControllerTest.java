package com.youtube.research.controller;

import com.google.api.services.youtube.model.Video;
import com.youtube.research.dto.youtube.YouTubeVideoListResponse;
import com.youtube.research.security.JwtTokenProvider;
import com.youtube.research.service.CommentService;
import com.youtube.research.service.YouTubeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class YouTubeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private YouTubeService youtubeService;

    @MockitoBean
    private CommentService commentService;

    // ==================== Search Tests ====================

    @Test
    void shouldSearchYouTubeAndReturn200() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String query = "machine learning";
        int maxResults = 5;

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setKind("youtube#videoListResponse");
        mockResponse.setItems(new ArrayList<>());

        when(youtubeService.searchVideos(eq(query), eq(maxResults)))
                .thenReturn(mockResponse);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults)
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.kind").isEqualTo("youtube#videoListResponse")
                .jsonPath("$.items").isArray();
    }

    @Test
    void shouldSearchYouTubeWithDefaultMaxResults() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String query = "deep learning";

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setKind("youtube#videoListResponse");
        mockResponse.setItems(new ArrayList<>());

        when(youtubeService.searchVideos(eq(query), eq(5)))
                .thenReturn(mockResponse);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search")
                        .queryParam("q", query)
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.kind").isEqualTo("youtube#videoListResponse");
    }

    @Test
    void shouldReturnBadRequestWhenSearchQueryIsEmpty() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);

        when(youtubeService.searchVideos(eq(""), anyInt()))
                .thenThrow(new IllegalArgumentException("Search query cannot be empty"));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search")
                        .queryParam("q", "")
                        .queryParam("maxResults", 5)
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Search query cannot be empty");
    }

    @Test
    void shouldReturnUnauthorizedWhenSearchWithoutToken() {
        // Act & Assert - JWT error from security layer
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search")
                        .queryParam("q", "machine learning")
                        .queryParam("maxResults", 5)
                        .build())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturnInternalServerErrorWhenYouTubeApiError() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String query = "machine learning";

        when(youtubeService.searchVideos(eq(query), anyInt()))
                .thenThrow(new IOException("YouTube API error"));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search")
                        .queryParam("q", query)
                        .queryParam("maxResults", 5)
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").value(s -> s.toString().contains("Error searching YouTube"));
    }

    // ==================== Get Video Details Tests ====================

    @Test
    void shouldGetVideoDetailsAndReturn200() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String videoId = "dQw4w9WgXcQ";

        Video mockVideo = new Video();
        mockVideo.setId(videoId);
        mockVideo.setKind("youtube#video");

        when(youtubeService.getVideoById(eq(videoId)))
                .thenReturn(mockVideo);

        // Act & Assert
        webTestClient.get()
                .uri("/api/youtube/videos/{videoId}", videoId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(videoId)
                .jsonPath("$.kind").isEqualTo("youtube#video");
    }

    @Test
    void shouldReturnBadRequestWhenVideoIdIsInvalid() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String invalidVideoId = "invalid";

        when(youtubeService.getVideoById(eq(invalidVideoId)))
                .thenThrow(new IllegalArgumentException("Video not found"));

        // Act & Assert
        webTestClient.get()
                .uri("/api/youtube/videos/{videoId}", invalidVideoId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Video not found");
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingVideoDetailsWithoutToken() {
        // Act & Assert - JWT error from security layer
        webTestClient.get()
                .uri("/api/youtube/videos/{videoId}", "dQw4w9WgXcQ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturnInternalServerErrorWhenGettingVideoDetailsFails() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String videoId = "dQw4w9WgXcQ";

        when(youtubeService.getVideoById(eq(videoId)))
                .thenThrow(new IOException("YouTube API error"));

        // Act & Assert
        webTestClient.get()
                .uri("/api/youtube/videos/{videoId}", videoId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").value(s -> s.toString().contains("Error fetching video"));
    }

    @Test
    void shouldReturnUnexpectedErrorResponse() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String query = "machine learning";

        when(youtubeService.searchVideos(eq(query), anyInt()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search")
                        .queryParam("q", query)
                        .queryParam("maxResults", 5)
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").isEqualTo("An unexpected error occurred");
    }

    @Test
    void shouldSearchAdvancedAndReturn200() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String query = "machine learning";

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setKind("youtube#videoListResponse");
        mockResponse.setItems(new ArrayList<>());

        when(youtubeService.searchVideosAdvanced(eq(query), anyInt(), eq("viewCount"), eq("video"), eq("long")))
                .thenReturn(mockResponse);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search/advanced")
                        .queryParam("q", query)
                        .queryParam("order", "viewCount")
                        .queryParam("videoType", "video")
                        .queryParam("videoDuration", "long")
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.kind").isEqualTo("youtube#videoListResponse");
    }

    @Test
    void shouldReturnBadRequestWhenOrderIsInvalid() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);

        when(youtubeService.searchVideosAdvanced(eq("machine learning"), anyInt(), eq("invalid"), eq("any"), eq("any")))
                .thenThrow(new IllegalArgumentException("Invalid order parameter"));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search/advanced")
                        .queryParam("q", "machine learning")
                        .queryParam("order", "invalid")
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid order parameter");
    }

    @Test
    void shouldUseDefaultFiltersWhenNotProvided() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String query = "python";

        YouTubeVideoListResponse mockResponse = new YouTubeVideoListResponse();
        mockResponse.setKind("youtube#videoListResponse");
        mockResponse.setItems(new ArrayList<>());

        when(youtubeService.searchVideosAdvanced(eq(query), eq(5), eq("relevance"), eq("any"), eq("any")))
                .thenReturn(mockResponse);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/search/advanced")
                        .queryParam("q", query)
                        .build())
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk();
    }

    // ==================== Get Comments Tests ====================

    @Test
    void shouldGetVideoCommentsAndReturn200() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String videoId = "dQw4w9WgXcQ";

        com.youtube.research.dto.youtube.YouTubeCommentListResponse mockResponse =
                new com.youtube.research.dto.youtube.YouTubeCommentListResponse();
        mockResponse.setVideoId(videoId);
        mockResponse.setItems(new ArrayList<>());

        when(commentService.getVideoComments(eq(videoId), eq(20)))
                .thenReturn(mockResponse);

        // Act & Assert
        webTestClient.get()
                .uri("/api/youtube/videos/{videoId}/comments", videoId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.video_id").isEqualTo(videoId)
                .jsonPath("$.items").isArray();
    }

    @Test
    void shouldGetVideoCommentsWithCustomMaxResults() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String videoId = "dQw4w9WgXcQ";
        int maxResults = 50;

        com.youtube.research.dto.youtube.YouTubeCommentListResponse mockResponse =
                new com.youtube.research.dto.youtube.YouTubeCommentListResponse();
        mockResponse.setVideoId(videoId);
        mockResponse.setItems(new ArrayList<>());

        when(commentService.getVideoComments(eq(videoId), eq(maxResults)))
                .thenReturn(mockResponse);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/videos/{videoId}/comments")
                        .queryParam("maxResults", maxResults)
                        .build(videoId))
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.video_id").isEqualTo(videoId);
    }

    @Test
    void shouldReturnBadRequestWhenMaxResultsInvalid() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String videoId = "dQw4w9WgXcQ";

        when(commentService.getVideoComments(eq(videoId), eq(101)))
                .thenThrow(new IllegalArgumentException("maxResults must be between 1 and 100"));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/youtube/videos/{videoId}/comments")
                        .queryParam("maxResults", 101)
                        .build(videoId))
                .header("Authorization", token)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("maxResults must be between 1 and 100");
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingCommentsWithoutToken() {
        // Act & Assert
        webTestClient.get()
                .uri("/api/youtube/videos/{videoId}/comments", "dQw4w9WgXcQ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturnInternalServerErrorWhenFetchingCommentsFails() throws IOException {
        // Arrange
        String username = "testuser";
        String token = "Bearer " + jwtTokenProvider.generateToken(username);
        String videoId = "dQw4w9WgXcQ";

        when(commentService.getVideoComments(eq(videoId), anyInt()))
                .thenThrow(new IOException("YouTube API error"));

        // Act & Assert
        webTestClient.get()
                .uri("/api/youtube/videos/{videoId}/comments", videoId)
                .header("Authorization", token)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").value(s -> s.toString().contains("Error fetching comments"));
    }
}