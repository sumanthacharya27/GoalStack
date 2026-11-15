package com.goalstack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goalstack.ai.AIProperties;
import com.goalstack.ai.FakeAIProvider;
import com.goalstack.ai.StructuredAIClient;
import com.goalstack.ai.StructuredOutputParser;
import com.goalstack.ai.dto.AssistantTurn;
import com.goalstack.ai.dto.GeneratedPlan;
import com.goalstack.dto.AIChatMessageRequest;
import com.goalstack.dto.AIChatRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AIPlanningServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final StructuredOutputParser parser = new StructuredOutputParser(
            mapper, Validation.buildDefaultValidatorFactory().getValidator());
    private final AIPlanningService service = new AIPlanningService(new StructuredAIClient(
            new FakeAIProvider(new AIProperties.Fake(
                    AIProperties.Fake.FailureMode.NONE, Duration.ZERO), mapper), parser));

    @Test
    void chatAsksAClarifyingQuestionForTheFirstUserMessage() {
        AssistantTurn turn = service.chat(new AIChatRequest(List.of(
                new AIChatMessageRequest(AIChatMessageRequest.ChatRole.USER, "I want to learn Java"))));

        assertThat(turn.reply()).contains("hours per week");
        assertThat(turn.readyToGenerate()).isFalse();
    }

    @Test
    void chatBecomesReadyAfterTheUserProvidesAnotherDetail() {
        AssistantTurn turn = service.chat(new AIChatRequest(List.of(
                new AIChatMessageRequest(AIChatMessageRequest.ChatRole.USER, "I want to learn Java"),
                new AIChatMessageRequest(AIChatMessageRequest.ChatRole.ASSISTANT, "How much time do you have?"),
                new AIChatMessageRequest(AIChatMessageRequest.ChatRole.USER, "I can study 6 hours per week"))));

        assertThat(turn.readyToGenerate()).isTrue();
    }

    @Test
    void quickGoalGenerationReturnsAValidatedDraft() {
        GeneratedPlan plan = service.generatePlan(new AIChatRequest(List.of(
                new AIChatMessageRequest(AIChatMessageRequest.ChatRole.USER, "I want to learn Spring Boot"))));

        assertThat(plan.goalTitle()).contains("Spring Boot");
        assertThat(plan.subgoals()).hasSize(5);
    }
}
