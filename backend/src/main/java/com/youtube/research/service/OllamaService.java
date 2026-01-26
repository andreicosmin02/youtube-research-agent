package com.youtube.research.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OllamaService {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:mistral}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OllamaService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Get a response from Ollama LLM
     *
     * @param prompt The prompt/message to send
     * @return LLM response text
     * @throws IOException If API call fails
     */
    public String generateResponse(String prompt) throws IOException {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        log.debug("Generating response from Ollama for prompt: {}", prompt.substring(0, Math.min(100, prompt.length())));

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);

            String url = ollamaBaseUrl + "/api/generate";

            String response = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                throw new IOException("Empty response from Ollama");
            }

            JsonNode jsonNode = objectMapper.readTree(response);
            String generatedText = jsonNode.get("response").asText();

            log.debug("Generated response length: {}", generatedText.length());
            return generatedText;

        } catch (IOException e) {
            log.error("Error calling Ollama API", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating response", e);
            throw new IOException("Error generating response from LLM", e);
        }
    }

    /**
     * Decide what action to take based on user message and context
     *
     * @param userMessage User's message
     * @param conversationContext Previous messages for context
     * @return Decision object with action type and parameters
     * @throws IOException If API call fails
     */
    public AgentDecision decideAction(String userMessage, String conversationContext) throws IOException {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("User message cannot be empty");
        }

        log.debug("Making agent decision for message: {}", userMessage);

        String systemPrompt = """
You are a YouTube research assistant. Analyze the user message and decide which action to take.

Available actions:
1. "search" - Search YouTube videos. Use when user asks to find/search videos. Return with "query" parameter.
2. "search_advanced" - Advanced search with filters. Use when user specifies video type, duration, or sort order. Return with "query" and optional "order", "videoType", "videoDuration".
3. "get_video" - Get video details. Use when user asks about a specific video they mentioned. Return with "videoId" parameter.
4. "get_comments" - Get video comments. Use when user asks about comments/opinions on a video. Return with "videoId" parameter.
5. "chat" - Just answer from context. Use when user asks general questions or needs clarification.

Respond ONLY with a JSON object in this format:
{
  "action": "search|search_advanced|get_video|get_comments|chat",
  "query": "search query (if applicable)",
  "videoId": "video id (if applicable)",
  "order": "relevance|date|viewCount|rating|title (optional)",
  "videoType": "any|video|episode|movie (optional)",
  "videoDuration": "any|short|medium|long (optional)",
  "reasoning": "brief explanation"
}
""";

        String prompt = systemPrompt + "\n\nConversation context:\n" + conversationContext +
                "\n\nUser message: " + userMessage;

        String response = generateResponse(prompt);

        try {
            return objectMapper.readValue(response, AgentDecision.class);
        } catch (IOException e) {
            log.error("Failed to parse agent decision JSON", e);
            // Fallback to chat if decision parsing fails
            return new AgentDecision("chat", null, null, null, null, null, "Parse error, defaulting to chat");
        }
    }

    /**
     * Agent decision object
     */
    public static class AgentDecision {
        public String action;
        public String query;
        public String videoId;
        public String order;
        public String videoType;
        public String videoDuration;
        public String reasoning;

        public AgentDecision() {}

        public AgentDecision(String action, String query, String videoId, String order, String videoType, String videoDuration, String reasoning) {
            this.action = action;
            this.query = query;
            this.videoId = videoId;
            this.order = order;
            this.videoType = videoType;
            this.videoDuration = videoDuration;
            this.reasoning = reasoning;
        }
    }
}