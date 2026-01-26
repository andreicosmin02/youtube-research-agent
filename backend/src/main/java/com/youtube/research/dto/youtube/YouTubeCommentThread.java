package com.youtube.research.dto.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeCommentThread {

    @JsonProperty("video_id")
    private String videoId;

    @JsonProperty("can_reply")
    private Boolean canReply;

    @JsonProperty("total_reply_count")
    private Long totalReplyCount;

    @JsonProperty("is_public")
    private Boolean isPublic;

    @JsonProperty("top_level_comment")
    private CommentData topLevelComment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentData {

        @JsonProperty("author_display_name")
        private String authorDisplayName;

        @JsonProperty("author_profile_image_url")
        private String authorProfileImageUrl;

        @JsonProperty("like_count")
        private Long likeCount;

        @JsonProperty("published_at")
        private String publishedAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        @JsonProperty("text_display")
        private String textDisplay;
    }
}