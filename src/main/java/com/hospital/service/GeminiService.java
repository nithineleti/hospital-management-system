package com.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeminiService {

    @Value("${gemini.api.url:}")
    private String geminiApiUrl;

    @Value("${gemini.api.model:gemini-1.5-mini}")
    private String geminiModel;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateResponse(String systemInstruction, String userPrompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.contains("YOUR_GEMINI_API_KEY") || geminiApiKey.contains("placeholder")) {
            return "SIRI: Gemini API key is not configured. Please set gemini.api.key or GEMINI_API_KEY and restart the app.";
        }

        String effectiveUrl = geminiApiUrl != null && !geminiApiUrl.isBlank()
                ? geminiApiUrl
                : String.format("https://generativeai.googleapis.com/v1/models/%s:generate", geminiModel);

        try {
            String instructionPayload = systemInstruction + "\nUser: " + userPrompt;
            com.fasterxml.jackson.databind.node.ObjectNode requestNode = objectMapper.createObjectNode();

            if (effectiveUrl.contains("/v1/")) {
                // New Gemini v1 endpoint expects an input field
                requestNode.put("input", instructionPayload);
                requestNode.put("temperature", 0.2);
                requestNode.put("maxOutputTokens", 320);
            } else {
                // v1beta2-style payload
                com.fasterxml.jackson.databind.node.ObjectNode promptNode = objectMapper.createObjectNode();
                promptNode.put("text", instructionPayload);
                requestNode.set("prompt", promptNode);
                requestNode.put("temperature", 0.2);
                requestNode.put("max_output_tokens", 320);
            }

            String requestBody = objectMapper.writeValueAsString(requestNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(effectiveUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + geminiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                JsonNode root = objectMapper.readTree(response.body());

                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode first = candidates.get(0);
                    JsonNode content = first.path("content");
                    if (content.isTextual()) {
                        return content.asText();
                    }
                }

                JsonNode output = root.path("output");
                if (output.isArray() && output.size() > 0) {
                    JsonNode first = output.get(0);
                    JsonNode content = first.path("content");
                    if (content.isTextual()) {
                        return content.asText();
                    }
                }

                JsonNode textNode = root.path("text");
                if (textNode.isTextual()) {
                    return textNode.asText();
                }

                return "SIRI: Received an unexpected response from Gemini. Please check the configured endpoint and payload format.";
            } else if (status == 404) {
                return String.format("SIRI: Gemini API endpoint not found (404). Confirm the endpoint is correct and uses a valid model URL. Current URL: %s", effectiveUrl);
            } else if (status == 401 || status == 403) {
                return String.format("SIRI: Gemini authentication failed (%d). Check your API key and permissions.", status);
            } else {
                return String.format("SIRI: Gemini API returned status %d. Please check API key and endpoint. URL used: %s", status, effectiveUrl);
            }
        } catch (Exception e) {
            return "SIRI: Unable to contact Gemini service right now. " + e.getMessage();
        }
    }
}
