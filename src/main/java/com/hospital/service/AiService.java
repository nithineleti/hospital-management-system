package com.hospital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${gemini.api.key:AIzaSyA6m235En4tldzSCIzx-Kkpcc5RrK4DkaY}")
    private String geminiApiKey;

    private final HttpClient httpClient;

    public AiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Async
    public CompletableFuture<String> generateResponse(String message) {
        try {
            String prompt = "You are a helpful medical assistant for Hospital Management System. User: " + message;
            String json = String.format("""
{
  "contents": [{
    "parts": [{
      "text": "%s"
    }]
  }]
}
""", prompt);

            logger.info("Sending to Gemini: {}", json.substring(0, 200));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("Gemini response status: {} preview: {}", response.statusCode(), response.body().substring(0, Math.min(300, response.body().length())));

            if (response.statusCode() == 200) {
                String content = extractText(response.body());
                logger.info("AI Response generated successfully: {}", content.substring(0, 100));
                return CompletableFuture.completedFuture(content);
            } else {
                logger.error("Gemini API error: {} body: {}", response.statusCode(), response.body());
                // Mock response for demo
                return CompletableFuture.completedFuture("🤖 Hi Doctor! I'm Siri AI. Gemini API key needs update (400 error). Demo responses:\\n\\n• Check patient #123 status\\n• Schedule Dr. Smith 2pm\\n• Inventory low on Paracetamol (15 left)\\n\\nFull AI ready when key fixed!");
            }
        } catch (Exception e) {
            logger.error("Error generating AI response", e);
            return CompletableFuture.completedFuture("🤖 Siri demo mode: Ask about patients, appointments, or inventory!");
        }
    }

    public void streamResponse(SseEmitter emitter, String message) {
        generateResponse(message).thenAccept(result -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(result));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }).exceptionally(throwable -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Connection error: " + throwable.getMessage()));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return null;
        });
    }

    private String extractText(String jsonResponse) {
        try {
            // Better extraction for Gemini response
            int candidatesStart = jsonResponse.indexOf("\"candidates\":[{");
            if (candidatesStart > 0) {
                String candidates = jsonResponse.substring(candidatesStart);
                int contentStart = candidates.indexOf("\"content\":{") + 10;
                int partsStart = candidates.indexOf("\"parts\":[", contentStart);
                int textStart = candidates.indexOf("\"text\":\"", partsStart) + 8;
                int textEnd = candidates.indexOf("\"", textStart);
                if (textStart > 7 && textEnd > textStart) {
                    String text = candidates.substring(textStart, textEnd);
                    return text.replace("\\n", "\n").replace("\\t", "  ");
                }
            }
            return "No response text found in JSON.";
        } catch (Exception e) {
            logger.error("Text extraction error", e);
            return "Response received but parsing failed.";
        }
    }
}
