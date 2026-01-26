package com.youtube.research.dto.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeCommentListResponse {

    @JsonProperty("video_id")
    private String videoId;

    @JsonProperty("page_info")
    private PageInfo pageInfo;

    @JsonProperty("next_page_token")
    private String nextPageToken;

    private List<YouTubeCommentThread> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {

        @JsonProperty("total_results")
        private Integer totalResults;

        @JsonProperty("results_per_page")
        private Integer resultsPerPage;
    }
}