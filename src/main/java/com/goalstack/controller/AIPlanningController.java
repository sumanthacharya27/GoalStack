package com.goalstack.controller;

import com.goalstack.ai.dto.AssistantTurn;
import com.goalstack.ai.dto.GeneratedPlan;
import com.goalstack.dto.AIChatRequest;
import com.goalstack.service.AIPlanningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated endpoints used by the simple GoalStack AI planner demo. */
@RestController
@RequestMapping("/api/ai")
public class AIPlanningController {

    private final AIPlanningService planningService;

    public AIPlanningController(AIPlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/chat")
    public AssistantTurn chat(@Valid @RequestBody AIChatRequest request) {
        return planningService.chat(request);
    }

    @PostMapping("/generate-plan")
    public GeneratedPlan generatePlan(@Valid @RequestBody AIChatRequest request) {
        return planningService.generatePlan(request);
    }
}
