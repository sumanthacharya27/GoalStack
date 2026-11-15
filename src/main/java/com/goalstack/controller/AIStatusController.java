package com.goalstack.controller;

import com.goalstack.ai.AIProvider;
import com.goalstack.dto.AIStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the app (and you, while setting up) check whether the AI assistant is usable. Login is required. */
@RestController
@RequestMapping("/api/ai")
public class AIStatusController {

    private final AIProvider aiProvider;

    public AIStatusController(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @GetMapping("/status")
    public AIStatusResponse status() {
        return AIStatusResponse.from(aiProvider.checkStatus());
    }
}
