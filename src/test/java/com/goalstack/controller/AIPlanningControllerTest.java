package com.goalstack.controller;

import com.goalstack.ai.dto.AssistantTurn;
import com.goalstack.ai.dto.GeneratedPlan;
import com.goalstack.ai.dto.PlanSubgoal;
import java.math.BigDecimal;
import com.goalstack.dto.AIChatMessageRequest;
import com.goalstack.dto.AIChatRequest;
import com.goalstack.service.AIPlanningService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIPlanningControllerTest {

    @Test
    void chatDelegatesToPlanningService() {
        AIPlanningService service = mock(AIPlanningService.class);
        AIPlanningController controller = new AIPlanningController(service);
        AIChatRequest request = new AIChatRequest(List.of(
                new AIChatMessageRequest(AIChatMessageRequest.ChatRole.USER, "Learn Spring Boot")));
        AssistantTurn expected = new AssistantTurn("How much time do you have?", false, null);
        when(service.chat(request)).thenReturn(expected);

        assertSame(expected, controller.chat(request));
        verify(service).chat(request);
    }

    @Test
    void generatePlanDelegatesToPlanningService() {
        AIPlanningService service = mock(AIPlanningService.class);
        AIPlanningController controller = new AIPlanningController(service);
        AIChatRequest request = new AIChatRequest(List.of(
                new AIChatMessageRequest(AIChatMessageRequest.ChatRole.USER, "Learn Spring Boot")));
        GeneratedPlan expected = new GeneratedPlan("Spring Boot", "Learn the framework",
                List.of(new PlanSubgoal("Learn basics", "Study core concepts", new BigDecimal("5.00"))),
                List.of());
        when(service.generatePlan(request)).thenReturn(expected);

        assertSame(expected, controller.generatePlan(request));
        verify(service).generatePlan(request);
    }
}
