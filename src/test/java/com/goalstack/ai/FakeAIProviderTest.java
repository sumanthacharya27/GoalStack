package com.goalstack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goalstack.ai.dto.AssistantTurn;
import com.goalstack.ai.dto.GeneratedPlan;
import com.goalstack.ai.dto.PlanSubgoal;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeAIProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StructuredOutputParser parser = new StructuredOutputParser(
            objectMapper, Validation.buildDefaultValidatorFactory().getValidator());

    private FakeAIProvider fake(AIProperties.Fake.FailureMode mode) {
        return new FakeAIProvider(new AIProperties.Fake(mode, Duration.ZERO), objectMapper);
    }

    private AIProviderRequest turnRequest(AIMessage... messages) {
        return AIProviderRequest.structured(List.of(messages), AISchemas.ASSISTANT_TURN, AISchemas.assistantTurn(), 0.2, 300);
    }

    private AIProviderRequest planRequest(AIMessage... messages) {
        return AIProviderRequest.structured(List.of(messages), AISchemas.GENERATED_PLAN, AISchemas.generatedPlan(), 0.2, 2000);
    }

    @Test
    void theFirstMessageGetsAClarifyingQuestion() {
        AIProviderResponse response = fake(AIProperties.Fake.FailureMode.NONE)
                .generate(turnRequest(AIMessage.system("Plan goals."), AIMessage.user("I want to learn Java")));

        AssistantTurn turn = parser.parse(response.content(), AssistantTurn.class);

        assertThat(turn.readyToGenerate()).isFalse();
        assertThat(turn.reply()).contains("hours per week");
        assertThat(turn.facts().desiredOutcome()).contains("Java");
    }

    @Test
    void theSecondMessageIsEnoughAndHoursPerWeekAreFound() {
        AIProviderResponse response = fake(AIProperties.Fake.FailureMode.NONE).generate(turnRequest(
                AIMessage.system("Plan goals."), AIMessage.user("I want to learn Java"),
                AIMessage.assistant("How many hours per week?"), AIMessage.user("About 6 hours a week, I know some C")));

        AssistantTurn turn = parser.parse(response.content(), AssistantTurn.class);

        assertThat(turn.readyToGenerate()).isTrue();
        assertThat(turn.facts().hoursPerWeek()).isEqualByComparingTo("6");
    }

    @Test
    void thePlanPassesTheSameValidationAsARealModelAnswer() {
        AIProviderResponse response = fake(AIProperties.Fake.FailureMode.NONE)
                .generate(planRequest(AIMessage.system("Plan goals."), AIMessage.user("I want to learn Java")));

        GeneratedPlan plan = parser.parse(response.content(), GeneratedPlan.class);

        assertThat(plan.subgoals()).hasSize(5);
        BigDecimal total = plan.subgoals().stream().map(PlanSubgoal::estimatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100");
        assertThat(plan.goalTitle()).isEqualTo("I want to learn Java");
    }

    @Test
    void aVeryLongFirstMessageStillGivesAValidTitle() {
        AIProviderResponse response = fake(AIProperties.Fake.FailureMode.NONE)
                .generate(planRequest(AIMessage.user("x".repeat(400))));

        assertThat(parser.parse(response.content(), GeneratedPlan.class).goalTitle()).hasSize(120);
    }

    @Test
    void aRequestWithoutASchemaGetsPlainText() {
        AIProviderResponse response = fake(AIProperties.Fake.FailureMode.NONE)
                .generate(new AIProviderRequest(List.of(AIMessage.user("hi")), null, null, null, null));

        assertThat(response.content()).contains("fake AI");
    }

    @Test
    void failureModesFailLikeARealProvider() {
        assertFails(AIProperties.Fake.FailureMode.UNAVAILABLE, AIErrorType.UNAVAILABLE);
        assertFails(AIProperties.Fake.FailureMode.RATE_LIMITED, AIErrorType.RATE_LIMITED);
        assertFails(AIProperties.Fake.FailureMode.TIMEOUT, AIErrorType.TIMEOUT);
    }

    @Test
    void badJsonModeGivesAnswersTheParserRejects() {
        AIProviderResponse response = fake(AIProperties.Fake.FailureMode.BAD_JSON)
                .generate(turnRequest(AIMessage.user("hello")));

        assertThatThrownBy(() -> parser.parse(response.content(), AssistantTurn.class))
                .isInstanceOfSatisfying(AIProviderException.class,
                        ex -> assertThat(ex.getType()).isEqualTo(AIErrorType.BAD_RESPONSE));
    }

    @Test
    void statusSaysDemoUnlessItIsSetToFail() {
        assertThat(fake(AIProperties.Fake.FailureMode.NONE).checkStatus().availability())
                .isEqualTo(AIStatus.Availability.DEMO);
        assertThat(fake(AIProperties.Fake.FailureMode.UNAVAILABLE).checkStatus().available()).isFalse();
    }

    private void assertFails(AIProperties.Fake.FailureMode mode, AIErrorType expected) {
        assertThatThrownBy(() -> fake(mode).generate(turnRequest(AIMessage.user("hello"))))
                .isInstanceOfSatisfying(AIProviderException.class, ex -> assertThat(ex.getType()).isEqualTo(expected));
    }
}
