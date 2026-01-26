package com.youtube.research.config;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.youtube.YouTube;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class YouTubeConfig {

    @Getter
    @Value("${youtube.api.key:}")
    private String apiKey;

    private YouTube youtube;

    public YouTube getYouTube() {
        if (youtube == null) {
            HttpTransport httpTransport = new NetHttpTransport();
            youtube = new YouTube.Builder(httpTransport, new GsonFactory(), null)
                    .setApplicationName("youtube-research-agent")
                    .build();
        }
        return youtube;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}