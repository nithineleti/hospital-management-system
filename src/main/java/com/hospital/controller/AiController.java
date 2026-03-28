package com.hospital.controller;

import com.hospital.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        if (message.trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter(30000L); // 30s timeout
            try {
                emitter.send(SseEmitter.event().name("error").data("Empty message"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(120000L); // 2min timeout
        emitter.onCompletion(() -> System.out.println("AI Chat completed"));
        emitter.onTimeout(() -> {
            emitter.complete();
            System.out.println("AI Chat timeout");
        });
        emitter.onError((ex) -> {
            emitter.completeWithError(ex);
            System.out.println("AI Chat error: " + ex.getMessage());
        });

        aiService.streamResponse(emitter, message);
        return emitter;
    }
}
