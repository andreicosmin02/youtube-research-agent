package com.youtube.research.service;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.youtube.research.config.YouTubeConfig;
import com.youtube.research.dto.youtube.YouTubeVideo;
import com.youtube.research.dto.youtube.YouTubeVideoListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YouTubeService {

    private final YouTubeConfig youtubeConfig;
    private final YouTube youtube;

    public YouTubeService(YouTubeConfig youtubeConfig) {
        this.youtubeConfig = youtubeConfig;
        this.youtube = youtubeConfig.getYouTube();
    }
    /**
     * Search for videos on YouTube
     *
     * @param query Search query string
     * @param maxResults Maximum number of results (1-50)
     * @return YouTubeVideoListResponse with video details
     * @throws IOException If API call fails
     * @throws IllegalArgumentException If parameters are invalid
     */
    public YouTubeVideoListResponse searchVideos(String query, int maxResults) throws IOException {
        validateSearchQuery(query);
        validateMaxResults(maxResults);

        log.debug("Searching YouTube for: {}", query);

        try {
            // Step 1: Search for videos
            YouTube.Search.List searchRequest = youtube.search().list(Collections.singletonList("snippet"))
                    .setQ(query)
                    .setKey(youtubeConfig.getApiKey())
                    .setMaxResults((long) maxResults)
                    .setType(Collections.singletonList("video"))
                    .setFields("items(snippet(channelId,channelTitle,description,publishedAt,thumbnails,title),id),pageInfo,nextPageToken,prevPageToken");

            SearchListResponse searchResponse = searchRequest.execute();

            if (searchResponse == null || searchResponse.getItems() == null || searchResponse.getItems().isEmpty()) {
                log.info("No videos found for query: {}", query);
                return createEmptyResponse(searchResponse);
            }

            List<SearchResult> searchResults = searchResponse.getItems();

            // Step 2: Extract video IDs
            List<String> videoIds = searchResults.stream()
                    .map(result -> result.getId().getVideoId())
                    .collect(Collectors.toList());

            log.debug("Found {} search results, fetching full details", videoIds.size());

            // Step 3: Get full video details
            List<Video> videoDetails = getVideoDetails(videoIds);

            // Step 4: Convert to DTO
            return convertToResponse(videoDetails, query, searchResponse);

        } catch (IOException e) {
            log.error("Error searching YouTube for: {}", query, e);
            throw e;
        }
    }

    /**
     * Get detailed information for specific videos
     *
     * @param videoIds List of YouTube video IDs
     * @return List of Video objects with full details
     * @throws IOException If API call fails
     */
    private List<Video> getVideoDetails(List<String> videoIds) throws IOException {
        if (videoIds == null || videoIds.isEmpty()) {
            return new ArrayList<>();
        }

        String ids = String.join(",", videoIds);
        log.debug("Fetching details for {} videos", videoIds.size());

        try {
            YouTube.Videos.List videosRequest = youtube.videos().list(Collections.singletonList("snippet,statistics,contentDetails"))
                    .setId(Collections.singletonList(ids))
                    .setKey(youtubeConfig.getApiKey())
                    .setFields("items(id,kind,etag,snippet,statistics,contentDetails),pageInfo");

            VideoListResponse videosResponse = videosRequest.execute();

            if (videosResponse == null || videosResponse.getItems() == null) {
                log.warn("No video details returned for ids: {}", ids);
                return new ArrayList<>();
            }

            log.debug("Successfully retrieved details for {} videos", videosResponse.getItems().size());
            return videosResponse.getItems();

        } catch (IOException e) {
            log.error("Error fetching video details for ids: {}", ids, e);
            throw e;
        }
    }

    /**
     * Get details for a single video
     *
     * @param videoId YouTube video ID
     * @return Video object with full details
     * @throws IOException If API call fails
     * @throws IllegalArgumentException If video not found
     */
    public Video getVideoById(String videoId) throws IOException {
        if (videoId == null || videoId.isBlank()) {
            throw new IllegalArgumentException("Video ID cannot be empty");
        }

        log.debug("Fetching video details for ID: {}", videoId);

        try {
            YouTube.Videos.List videosRequest = youtube.videos().list(Collections.singletonList("snippet,statistics,contentDetails"))
                    .setId(Collections.singletonList(videoId))
                    .setKey(youtubeConfig.getApiKey())
                    .setFields("items(id,kind,etag,snippet,statistics,contentDetails)");

            VideoListResponse videosResponse = videosRequest.execute();

            if (videosResponse == null || videosResponse.getItems() == null || videosResponse.getItems().isEmpty()) {
                throw new IllegalArgumentException("Video not found with ID: " + videoId);
            }

            return videosResponse.getItems().get(0);

        } catch (IOException e) {
            log.error("Error fetching video by ID: {}", videoId, e);
            throw e;
        }
    }

    /**
     * Convert Google API Video objects to DTO
     *
     * @param videos List of Video objects from Google API
     * @param query Original search query
     * @param searchResponse Original search response (for pagination info)
     * @return YouTubeVideoListResponse DTO
     */
    private YouTubeVideoListResponse convertToResponse(
            List<Video> videos,
            String query,
            SearchListResponse searchResponse) {

        List<YouTubeVideo> dtoVideos = videos.stream()
                .map(this::convertVideo)
                .collect(Collectors.toList());

        YouTubeVideoListResponse response = new YouTubeVideoListResponse();
        response.setKind("youtube#videoListResponse");
        response.setItems(dtoVideos);

        if (searchResponse != null) {
            response.setNextPageToken(searchResponse.getNextPageToken());
            response.setPrevPageToken(searchResponse.getPrevPageToken());

            if (searchResponse.getPageInfo() != null) {
                YouTubeVideoListResponse.PageInfo pageInfo = new YouTubeVideoListResponse.PageInfo();
                pageInfo.setTotalResults(searchResponse.getPageInfo().getTotalResults());
                pageInfo.setResultsPerPage(searchResponse.getPageInfo().getResultsPerPage());
                response.setPageInfo(pageInfo);
            }
        }

        return response;
    }

    /**
     * Convert a single Google API Video to DTO
     *
     * @param video Video object from Google API
     * @return YouTubeVideo DTO
     */
    private YouTubeVideo convertVideo(Video video) {
        YouTubeVideo dto = new YouTubeVideo();
        dto.setKind(video.getKind());
        dto.setEtag(video.getEtag());
        dto.setId(video.getId());

        // Convert snippet
        if (video.getSnippet() != null) {
            YouTubeVideo.Snippet snippetDto = new YouTubeVideo.Snippet();
            var snippet = video.getSnippet();

            snippetDto.setPublishedAt(snippet.getPublishedAt() != null ? snippet.getPublishedAt().toString() : null);
            snippetDto.setChannelId(snippet.getChannelId());
            snippetDto.setTitle(snippet.getTitle());
            snippetDto.setDescription(snippet.getDescription());
            snippetDto.setChannelTitle(snippet.getChannelTitle());
            snippetDto.setTags(snippet.getTags());
            snippetDto.setCategoryId(snippet.getCategoryId());
            snippetDto.setLiveBroadcastContent(snippet.getLiveBroadcastContent());
            snippetDto.setDefaultLanguage(snippet.getDefaultLanguage());

            // Convert thumbnails
            if (snippet.getThumbnails() != null) {
                YouTubeVideo.Thumbnails thumbnailsDto = new YouTubeVideo.Thumbnails();
                var thumbnails = snippet.getThumbnails();

                if (thumbnails.getDefault() != null) {
                    thumbnailsDto.setDefaultThumbnail(convertThumbnail(thumbnails.getDefault()));
                }
                if (thumbnails.getMedium() != null) {
                    thumbnailsDto.setMedium(convertThumbnail(thumbnails.getMedium()));
                }
                if (thumbnails.getHigh() != null) {
                    thumbnailsDto.setHigh(convertThumbnail(thumbnails.getHigh()));
                }
                if (thumbnails.getStandard() != null) {
                    thumbnailsDto.setStandard(convertThumbnail(thumbnails.getStandard()));
                }
                if (thumbnails.getMaxres() != null) {
                    thumbnailsDto.setMaxres(convertThumbnail(thumbnails.getMaxres()));
                }

                snippetDto.setThumbnails(thumbnailsDto);
            }

            // Convert localized
            if (snippet.getLocalized() != null) {
                YouTubeVideo.Localized localizedDto = new YouTubeVideo.Localized();
                localizedDto.setTitle(snippet.getLocalized().getTitle());
                localizedDto.setDescription(snippet.getLocalized().getDescription());
                snippetDto.setLocalized(localizedDto);
            }

            dto.setSnippet(snippetDto);
        }

        // Convert statistics
        if (video.getStatistics() != null) {
            YouTubeVideo.Statistics statsDto = new YouTubeVideo.Statistics();
            var stats = video.getStatistics();

            statsDto.setViewCount(stats.getViewCount() != null ? stats.getViewCount().toString() : null);
            statsDto.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount().toString() : null);
            statsDto.setCommentCount(stats.getCommentCount() != null ? stats.getCommentCount().toString() : null);

            dto.setStatistics(statsDto);
        }

        // Convert content details
        if (video.getContentDetails() != null) {
            YouTubeVideo.ContentDetails contentDto = new YouTubeVideo.ContentDetails();
            var content = video.getContentDetails();

            contentDto.setDuration(content.getDuration());
            contentDto.setDimension(content.getDimension());
            contentDto.setDefinition(content.getDefinition());
            contentDto.setCaption(content.getCaption());
            contentDto.setLicensedContent(content.getLicensedContent());
            contentDto.setProjection(content.getProjection());

            // Note: ContentRating conversion omitted for brevity (too many fields)
            // You can add it if needed

            dto.setContentDetails(contentDto);
        }

        return dto;
    }

    /**
     * Convert a single thumbnail
     *
     * @param thumbnail Thumbnail from Google API
     * @return YouTubeVideo.Thumbnail DTO
     */
    private YouTubeVideo.Thumbnail convertThumbnail(com.google.api.services.youtube.model.Thumbnail thumbnail) {
        return new YouTubeVideo.Thumbnail(
                thumbnail.getUrl(),
                Math.toIntExact(thumbnail.getWidth()),
                Math.toIntExact(thumbnail.getHeight())
        );
    }

    /**
     * Create an empty response for when no videos are found
     *
     * @param searchResponse Original search response (for pagination info)
     * @return Empty YouTubeVideoListResponse
     */
    private YouTubeVideoListResponse createEmptyResponse(SearchListResponse searchResponse) {
        YouTubeVideoListResponse response = new YouTubeVideoListResponse();
        response.setKind("youtube#videoListResponse");
        response.setItems(new ArrayList<>());

        if (searchResponse != null && searchResponse.getPageInfo() != null) {
            YouTubeVideoListResponse.PageInfo pageInfo = new YouTubeVideoListResponse.PageInfo();
            pageInfo.setTotalResults(0);
            pageInfo.setResultsPerPage(0);
            response.setPageInfo(pageInfo);
        }

        return response;
    }

    /**
     * Validate search query
     *
     * @param query Search query string
     * @throws IllegalArgumentException If query is invalid
     */
    private void validateSearchQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        if (query.length() > 256) {
            throw new IllegalArgumentException("Search query cannot exceed 256 characters");
        }
    }

    /**
     * Validate max results parameter
     *
     * @param maxResults Maximum number of results
     * @throws IllegalArgumentException If maxResults is invalid
     */
    private void validateMaxResults(int maxResults) {
        if (maxResults < 1 || maxResults > 50) {
            throw new IllegalArgumentException("maxResults must be between 1 and 50");
        }
    }

    /**
     * Search for videos with advanced filtering
     *
     * @param query Search query
     * @param maxResults Max results (1-50)
     * @param order Sort order (relevance, date, viewCount, rating, title)
     * @param videoType video, episode, movie, or any
     * @param videoDuration short, medium, long, or any
     * @return YouTubeVideoListResponse with filtered results
     * @throws IOException If API call fails
     */
    public YouTubeVideoListResponse searchVideosAdvanced(
            String query,
            int maxResults,
            String order,
            String videoType,
            String videoDuration) throws IOException {

        validateSearchQuery(query);
        validateMaxResults(maxResults);
        validateSearchFilters(order, videoType, videoDuration);

        log.debug("Advanced search: query={}, order={}, videoType={}, duration={}",
                query, order, videoType, videoDuration);

        try {
            YouTube.Search.List searchRequest = youtube.search().list(Collections.singletonList("snippet"))
                    .setQ(query)
                    .setKey(youtubeConfig.getApiKey())
                    .setMaxResults((long) maxResults)
                    .setType(Collections.singletonList("video"))
                    .setOrder(order)
                    .setVideoType(videoType)
                    .setVideoDuration(videoDuration)
                    .setFields("items(snippet(channelId,channelTitle,description,publishedAt,thumbnails,title),id),pageInfo,nextPageToken,prevPageToken");

            SearchListResponse searchResponse = searchRequest.execute();

            if (searchResponse == null || searchResponse.getItems() == null || searchResponse.getItems().isEmpty()) {
                log.info("No videos found for query: {} with filters", query);
                return createEmptyResponse(searchResponse);
            }

            List<SearchResult> searchResults = searchResponse.getItems();
            List<String> videoIds = searchResults.stream()
                    .map(result -> result.getId().getVideoId())
                    .collect(Collectors.toList());

            List<Video> videoDetails = getVideoDetails(videoIds);
            return convertToResponse(videoDetails, query, searchResponse);

        } catch (IOException e) {
            log.error("Error searching YouTube", e);
            throw e;
        }
    }

    private void validateSearchFilters(String order, String videoType, String videoDuration) {
        if (order != null && !order.matches("^(relevance|date|rating|title|videoCount|viewCount)$")) {
            throw new IllegalArgumentException("Invalid order parameter");
        }

        if (videoType != null && !videoType.matches("^(any|video|episode|movie)$")) {
            throw new IllegalArgumentException("Invalid videoType parameter");
        }

        if (videoDuration != null && !videoDuration.matches("^(any|short|medium|long)$")) {
            throw new IllegalArgumentException("Invalid videoDuration parameter");
        }
    }
}