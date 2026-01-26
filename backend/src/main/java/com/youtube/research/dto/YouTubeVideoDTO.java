package com.youtube.research.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class YouTubeVideoDTO {

    @JsonProperty("youtube_id")
    private String youtubeId;

    private String title;

    private String description;

    private String url;

    private String channel;

    @JsonProperty("channel_id")
    private String channelId;

    @JsonProperty("duration_seconds")
    private Long durationSeconds;

    private Long views;

    @JsonProperty("published_at")
    private String publishedAt;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
}