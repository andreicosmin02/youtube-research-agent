package com.youtube.research.dto.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeVideo {

    private String kind;
    private String etag;
    private String id;

    @JsonProperty("snippet")
    private Snippet snippet;

    @JsonProperty("statistics")
    private Statistics statistics;

    @JsonProperty("contentDetails")
    private ContentDetails contentDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Snippet {
        @JsonProperty("publishedAt")
        private String publishedAt;

        @JsonProperty("channelId")
        private String channelId;

        private String title;
        private String description;

        @JsonProperty("thumbnails")
        private Thumbnails thumbnails;

        @JsonProperty("channelTitle")
        private String channelTitle;

        private java.util.List<String> tags;

        @JsonProperty("categoryId")
        private String categoryId;

        @JsonProperty("liveBroadcastContent")
        private String liveBroadcastContent;

        @JsonProperty("defaultLanguage")
        private String defaultLanguage;

        private Localized localized;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Thumbnails {
        @JsonProperty("default")
        private Thumbnail defaultThumbnail;

        private Thumbnail medium;
        private Thumbnail high;
        private Thumbnail standard;
        private Thumbnail maxres;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Thumbnail {
        private String url;
        private Integer width;
        private Integer height;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Localized {
        private String title;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        @JsonProperty("viewCount")
        private String viewCount;

        @JsonProperty("likeCount")
        private String likeCount;

        @JsonProperty("commentCount")
        private String commentCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentDetails {
        private String duration;

        @JsonProperty("dimension")
        private String dimension;

        private String definition;

        @JsonProperty("caption")
        private String caption;

        @JsonProperty("licensedContent")
        private Boolean licensedContent;

        @JsonProperty("contentRating")
        private ContentRating contentRating;

        @JsonProperty("projection")
        private String projection;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentRating {
        @JsonProperty("acbRating")
        private String acbRating;

        @JsonProperty("agcomRating")
        private String agcomRating;

        @JsonProperty("anatelRating")
        private String anatelRating;

        @JsonProperty("bbfcRating")
        private String bbfcRating;

        @JsonProperty("bfvcRating")
        private String bfvcRating;

        @JsonProperty("bmukhRating")
        private String bmukhRating;

        @JsonProperty("catvRating")
        private String catvRating;

        @JsonProperty("catvfrRating")
        private String catvfrRating;

        @JsonProperty("cbfcRating")
        private String cbfcRating;

        @JsonProperty("cccRating")
        private String cccRating;

        @JsonProperty("cceRating")
        private String cceRating;

        @JsonProperty("chfilmRating")
        private String chfilmRating;

        @JsonProperty("chvrsRating")
        private String chvrsRating;

        @JsonProperty("cicfRating")
        private String cicfRating;

        @JsonProperty("classindRating")
        private String classindRating;

        @JsonProperty("djctqRating")
        private String djctqRating;

        @JsonProperty("eestbRating")
        private String eestbRating;

        @JsonProperty("egfilmRating")
        private String egfilmRating;

        @JsonProperty("eirinRating")
        private String eirinRating;

        @JsonProperty("fcoRating")
        private String fcoRating;

        @JsonProperty("fmocRating")
        private String fmocRating;

        @JsonProperty("fpbRating")
        private String fpbRating;

        @JsonProperty("fskRating")
        private String fskRating;

        @JsonProperty("grfilmRating")
        private String grfilmRating;

        @JsonProperty("ifcoRating")
        private String ifcoRating;

        @JsonProperty("ilfilmRating")
        private String ilfilmRating;

        @JsonProperty("incaaRating")
        private String incaaRating;

        @JsonProperty("kfcbRating")
        private String kfcbRating;

        @JsonProperty("kijuRating")
        private String kijuRating;

        @JsonProperty("kmrbRating")
        private String kmrbRating;

        @JsonProperty("lsf RatingRating")
        private String lsfRating;

        @JsonProperty("mccaaRating")
        private String mccaaRating;

        @JsonProperty("mccypRating")
        private String mccypRating;

        @JsonProperty("mdaRating")
        private String mdaRating;

        @JsonProperty("medietilsynetRating")
        private String medietilsynetRating;

        @JsonProperty("mekuRating")
        private String mekuRating;

        @JsonProperty("mibacRating")
        private String mibacRating;

        @JsonProperty("mocRating")
        private String mocRating;

        @JsonProperty("mpaRating")
        private String mpaRating;

        @JsonProperty("mtrcbRating")
        private String mtrcbRating;

        @JsonProperty("nbc tRating")
        private String nbcRating;

        @JsonProperty("nfvcbRating")
        private String nfvcbRating;

        @JsonProperty("nfwcRating")
        private String nfwcRating;

        @JsonProperty("nkclvRating")
        private String nkclvRating;

        @JsonProperty("oflcRating")
        private String oflcRating;

        @JsonProperty("pefilmRating")
        private String pefilmRating;

        @JsonProperty("rcnofRating")
        private String rcnofRating;

        @JsonProperty("resorteviolaoRating")
        private String resorteviolaoRating;

        @JsonProperty("rtcRating")
        private String rtcRating;

        @JsonProperty("rteRating")
        private String rteRating;

        @JsonProperty("russiaRating")
        private String russiaRating;

        @JsonProperty("skfilmRating")
        private String skfilmRating;

        @JsonProperty("smaisRating")
        private String smaisRating;

        @JsonProperty("smratRating")
        private String smratRating;

        @JsonProperty("tvpgRating")
        private String tvpgRating;

        @JsonProperty("ytRating")
        private String ytRating;
    }
}