package com.hospital.controller;

import com.hospital.service.SiriAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/siri")
public class SiriAgentController {

    private final SiriAgentService siriAgentService;

    public SiriAgentController(SiriAgentService siriAgentService) {
        this.siriAgentService = siriAgentService;
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> query(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please provide a non-empty 'text' field."));
        }

        String reply = siriAgentService.handleNaturalLanguage(text.trim());
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
