package com.youtube.research.service;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.youtube.research.config.YouTubeConfig;
import com.youtube.research.dto.youtube.YouTubeCommentListResponse;
import com.youtube.research.dto.youtube.YouTubeCommentThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentService {

    private final YouTubeConfig youtubeConfig;
    private YouTube youtube;

    public CommentService(YouTubeConfig youtubeConfig) {
        this.youtubeConfig = youtubeConfig;
        this.youtube = youtubeConfig.getYouTube();
    }

    /**
     * Get comments for a video
     *
     * @param videoId YouTube video ID
     * @param maxResults Maximum results (1-100)
     * @return YouTubeCommentListResponse with comments
     * @throws IOException If API call fails
     */
    public YouTubeCommentListResponse getVideoComments(String videoId, int maxResults) throws IOException {
        validateVideoId(videoId);
        validateMaxResults(maxResults);

        log.debug("Fetching comments for video: {}", videoId);

        try {
            YouTube.CommentThreads.List commentRequest = youtube.commentThreads().list(Collections.singletonList("snippet"))
                    .setVideoId(videoId)
                    .setKey(youtubeConfig.getApiKey())
                    .setMaxResults((long) maxResults)
                    .setTextFormat("plainText")
                    .setFields("items(snippet(videoId,canReply,totalReplyCount,isPublic,topLevelComment(snippet(authorDisplayName,authorProfileImageUrl,likeCount,publishedAt,updatedAt,textDisplay)))),pageInfo,nextPageToken");

            CommentThreadListResponse commentResponse = commentRequest.execute();

            if (commentResponse == null || commentResponse.getItems() == null || commentResponse.getItems().isEmpty()) {
                log.info("No comments found for video: {}", videoId);
                return createEmptyResponse(commentResponse);
            }

            List<CommentThread> commentThreads = commentResponse.getItems();
            return convertToResponse(commentThreads, videoId, commentResponse);

        } catch (IOException e) {
            log.error("Error fetching comments for video: {}", videoId, e);
            throw e;
        }
    }

    /**
     * Get replies to a specific comment
     *
     * @param parentCommentId Parent comment ID
     * @param maxResults Maximum results (1-100)
     * @return List of reply comments
     * @throws IOException If API call fails
     */
    public List<Comment> getCommentReplies(String parentCommentId, int maxResults) throws IOException {
        if (parentCommentId == null || parentCommentId.isBlank()) {
            throw new IllegalArgumentException("Parent comment ID cannot be empty");
        }

        validateMaxResults(maxResults);

        log.debug("Fetching replies for comment: {}", parentCommentId);

        try {
            YouTube.Comments.List repliesRequest = youtube.comments().list(Collections.singletonList("snippet"))
                    .setParentId(parentCommentId)
                    .setKey(youtubeConfig.getApiKey())
                    .setMaxResults((long) maxResults)
                    .setTextFormat("plainText")
                    .setFields("items(id,snippet(authorDisplayName,authorProfileImageUrl,likeCount,publishedAt,textDisplay))");

            CommentListResponse repliesResponse = repliesRequest.execute();

            if (repliesResponse == null || repliesResponse.getItems() == null) {
                return new ArrayList<>();
            }

            return repliesResponse.getItems();

        } catch (IOException e) {
            log.error("Error fetching replies for comment: {}", parentCommentId, e);
            throw e;
        }
    }

    /**
     * Convert CommentThread list to DTO
     *
     * @param commentThreads List of comment threads from Google API
     * @param videoId Video ID
     * @param response Original API response
     * @return YouTubeCommentListResponse DTO
     */
    private YouTubeCommentListResponse convertToResponse(
            List<CommentThread> commentThreads,
            String videoId,
            CommentThreadListResponse response) {

        List<YouTubeCommentThread> dtoThreads = commentThreads.stream()
                .map(this::convertCommentThread)
                .collect(Collectors.toList());

        YouTubeCommentListResponse dto = new YouTubeCommentListResponse();
        dto.setVideoId(videoId);
        dto.setItems(dtoThreads);

        if (response != null && response.getPageInfo() != null) {
            YouTubeCommentListResponse.PageInfo pageInfo = new YouTubeCommentListResponse.PageInfo();
            pageInfo.setTotalResults(response.getPageInfo().getTotalResults());
            pageInfo.setResultsPerPage(response.getPageInfo().getResultsPerPage());
            dto.setPageInfo(pageInfo);
        }

        if (response != null) {
            dto.setNextPageToken(response.getNextPageToken());
        }

        return dto;
    }

    /**
     * Convert a single CommentThread to DTO
     *
     * @param thread CommentThread from Google API
     * @return YouTubeCommentThread DTO
     */
    private YouTubeCommentThread convertCommentThread(CommentThread thread) {
        YouTubeCommentThread dto = new YouTubeCommentThread();

        if (thread.getSnippet() != null) {
            var snippet = thread.getSnippet();

            dto.setVideoId(snippet.getVideoId());
            dto.setCanReply(snippet.getCanReply());
            dto.setTotalReplyCount(snippet.getTotalReplyCount());
            dto.setIsPublic(snippet.getIsPublic());

            // Convert top-level comment
            if (snippet.getTopLevelComment() != null) {
                var topComment = snippet.getTopLevelComment();
                YouTubeCommentThread.CommentData commentData = new YouTubeCommentThread.CommentData();

                if (topComment.getSnippet() != null) {
                    var commentSnippet = topComment.getSnippet();
                    commentData.setAuthorDisplayName(commentSnippet.getAuthorDisplayName());
                    commentData.setAuthorProfileImageUrl(commentSnippet.getAuthorProfileImageUrl());
                    commentData.setLikeCount(commentSnippet.getLikeCount());
                    commentData.setPublishedAt(commentSnippet.getPublishedAt() != null ?
                            commentSnippet.getPublishedAt().toString() : null);
                    commentData.setUpdatedAt(commentSnippet.getUpdatedAt() != null ?
                            commentSnippet.getUpdatedAt().toString() : null);
                    commentData.setTextDisplay(commentSnippet.getTextDisplay());
                }

                dto.setTopLevelComment(commentData);
            }
        }

        return dto;
    }

    /**
     * Create empty response
     *
     * @param response Original response
     * @return Empty YouTubeCommentListResponse
     */
    private YouTubeCommentListResponse createEmptyResponse(CommentThreadListResponse response) {
        YouTubeCommentListResponse dto = new YouTubeCommentListResponse();
        dto.setItems(new ArrayList<>());

        if (response != null && response.getPageInfo() != null) {
            YouTubeCommentListResponse.PageInfo pageInfo = new YouTubeCommentListResponse.PageInfo();
            pageInfo.setTotalResults(0);
            pageInfo.setResultsPerPage(0);
            dto.setPageInfo(pageInfo);
        }

        return dto;
    }

    /**
     * Validate video ID
     *
     * @param videoId Video ID to validate
     */
    private void validateVideoId(String videoId) {
        if (videoId == null || videoId.isBlank()) {
            throw new IllegalArgumentException("Video ID cannot be empty");
        }

        if (videoId.length() > 100) {
            throw new IllegalArgumentException("Video ID cannot exceed 100 characters");
        }
    }

    /**
     * Validate max results
     *
     * @param maxResults Number to validate
     */
    private void validateMaxResults(int maxResults) {
        if (maxResults < 1 || maxResults > 100) {
            throw new IllegalArgumentException("maxResults must be between 1 and 100");
        }
    }
}