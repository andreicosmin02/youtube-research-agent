package com.youtube.research.dto.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeVideoListResponse {

    private String kind;
    private String etag;

    @JsonProperty("nextPageToken")
    private String nextPageToken;

    @JsonProperty("prevPageToken")
    private String prevPageToken;

    @JsonProperty("pageInfo")
    private PageInfo pageInfo;

    private List<YouTubeVideo> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        @JsonProperty("totalResults")
        private Integer totalResults;

        @JsonProperty("resultsPerPage")
        private Integer resultsPerPage;
    }
}