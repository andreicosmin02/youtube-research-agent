package com.youtube.research.controller;

import com.youtube.research.dto.youtube.YouTubeVideoListResponse;
import com.youtube.research.service.CommentService;
import com.youtube.research.service.YouTubeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {

    private final YouTubeService youtubeService;
    private final CommentService commentService;

    public YouTubeController(YouTubeService youtubeService, CommentService commentService) {
        this.youtubeService = youtubeService;
        this.commentService = commentService;
    }

    /**
     * Search YouTube videos
     *
     * GET /api/youtube/search?q=machine+learning&maxResults=5
     *
     * @param query Search query (required)
     * @param maxResults Maximum results to return (1-50, default: 5)
     * @param authentication Authenticated user
     * @return YouTubeVideoListResponse with search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchYouTube(
            @RequestParam(name = "q", required = true) String query,
            @RequestParam(name = "maxResults", required = false, defaultValue = "5") int maxResults,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted YouTube search with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication required"));
        }

        String username = authentication.getName();
        log.debug("User {} searching YouTube for: {}", username, query);

        try {
            YouTubeVideoListResponse results = youtubeService.searchVideos(query, maxResults);
            log.info("User {} found {} videos for query: {}", username,
                    results.getItems() != null ? results.getItems().size() : 0, query);

            return ResponseEntity.ok(results);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error in YouTube search: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));

        } catch (IOException e) {
            log.error("Error searching YouTube for query: {}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error searching YouTube: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during YouTube search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Get details for a specific YouTube video
     *
     * GET /api/youtube/videos/{videoId}
     *
     * @param videoId YouTube video ID (required)
     * @param authentication Authenticated user
     * @return Video details
     */
    @GetMapping("/videos/{videoId}")
    public ResponseEntity<?> getVideoDetails(
            @PathVariable String videoId,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to get video details with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication required"));
        }

        String username = authentication.getName();
        log.debug("User {} fetching details for video: {}", username, videoId);

        try {
            var video = youtubeService.getVideoById(videoId);
            log.info("User {} retrieved details for video: {}", username, videoId);

            return ResponseEntity.ok(video);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error fetching video: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));

        } catch (IOException e) {
            log.error("Error fetching video details for ID: {}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching video: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error fetching video details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    record ErrorResponse(String message) {}

    /**
     * Advanced YouTube search with filters
     *
     * GET /api/youtube/search?q=query&order=viewCount&videoType=video&videoDuration=medium
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<?> searchAdvanced(
            @RequestParam(name = "q", required = true) String query,
            @RequestParam(name = "maxResults", required = false, defaultValue = "5") int maxResults,
            @RequestParam(name = "order", required = false, defaultValue = "relevance") String order,
            @RequestParam(name = "videoType", required = false, defaultValue = "any") String videoType,
            @RequestParam(name = "videoDuration", required = false, defaultValue = "any") String videoDuration,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted YouTube search with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication required"));
        }

        String username = authentication.getName();
        log.debug("User {} performing advanced search: query={}, order={}, type={}, duration={}",
                username, query, order, videoType, videoDuration);

        try {
            YouTubeVideoListResponse results = youtubeService.searchVideosAdvanced(
                    query, maxResults, order, videoType, videoDuration);

            log.info("User {} found {} videos", username,
                    results.getItems() != null ? results.getItems().size() : 0);

            return ResponseEntity.ok(results);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));

        } catch (IOException e) {
            log.error("Error searching YouTube", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error searching YouTube"));

        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Get comments for a YouTube video
     *
     * GET /api/youtube/videos/{videoId}/comments?maxResults=20
     *
     * @param videoId YouTube video ID (required)
     * @param maxResults Maximum results (1-100, default: 20)
     * @param authentication Authenticated user
     * @return Comments for the video
     */
    @GetMapping("/videos/{videoId}/comments")
    public ResponseEntity<?> getVideoComments(
            @PathVariable String videoId,
            @RequestParam(name = "maxResults", required = false, defaultValue = "20") int maxResults,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Attempted to get comments with no authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication required"));
        }

        String username = authentication.getName();
        log.debug("User {} fetching comments for video: {}", username, videoId);

        try {
            var comments = commentService.getVideoComments(videoId, maxResults);
            log.info("User {} retrieved {} comments for video: {}", username,
                    comments.getItems() != null ? comments.getItems().size() : 0, videoId);

            return ResponseEntity.ok(comments);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));

        } catch (IOException e) {
            log.error("Error fetching comments for video: {}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching comments"));

        } catch (Exception e) {
            log.error("Unexpected error fetching comments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }
}