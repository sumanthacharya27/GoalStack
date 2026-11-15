package com.goalstack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goalstack.ai.dto.AssistantTurn;
import com.goalstack.ai.dto.GeneratedPlan;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredOutputParserTest {

    private final StructuredOutputParser parser = new StructuredOutputParser(
            new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

    private static final String VALID_PLAN = """
            {"goalTitle":"Learn Java","goalDescription":"Become job-ready.",
             "subgoals":[
               {"title":"Basics","description":"Syntax and types","estimatedHours":10},
               {"title":"OOP","estimatedHours":25.5}],
             "assumptions":["6 hours per week"]}
            """;

    // ----- good answers -----

    @Test
    void aValidPlanIsParsed() {
        GeneratedPlan plan = parser.parse(VALID_PLAN, GeneratedPlan.class);

        assertThat(plan.goalTitle()).isEqualTo("Learn Java");
        assertThat(plan.subgoals()).hasSize(2);
        assertThat(plan.subgoals().get(1).estimatedHours()).isEqualByComparingTo("25.5");
        assertThat(plan.assumptions()).containsExactly("6 hours per week");
    }

    @Test
    void aTurnWithFactsIsParsed() {
        AssistantTurn turn = parser.parse("""
                {"reply":"How many hours per week?","readyToGenerate":false,
                 "facts":{"desiredOutcome":"Learn Java","hoursPerWeek":6,"constraints":["exams in May"]}}
                """, AssistantTurn.class);

        assertThat(turn.reply()).isEqualTo("How many hours per week?");
        assertThat(turn.readyToGenerate()).isFalse();
        assertThat(turn.facts().hoursPerWeek()).isEqualByComparingTo("6");
        assertThat(turn.facts().constraints()).containsExactly("exams in May");
    }

    @Test
    void factsMayBeMissing() {
        AssistantTurn turn = parser.parse("{\"reply\":\"Hi\",\"readyToGenerate\":true}", AssistantTurn.class);

        assertThat(turn.facts()).isNull();
        assertThat(turn.readyToGenerate()).isTrue();
    }

    @Test
    void codeFencesAreRemoved() {
        GeneratedPlan plan = parser.parse("```json\n" + VALID_PLAN + "\n```", GeneratedPlan.class);

        assertThat(plan.subgoals()).hasSize(2);
    }

    @Test
    void chatterAroundTheJsonIsRemoved() {
        GeneratedPlan plan = parser.parse("Sure! Here is the plan:\n" + VALID_PLAN + "\nHope this helps!", GeneratedPlan.class);

        assertThat(plan.goalTitle()).isEqualTo("Learn Java");
    }

    @Test
    void unknownFieldsAreIgnored() {
        AssistantTurn turn = parser.parse("{\"reply\":\"Hi\",\"readyToGenerate\":false,\"mood\":\"happy\"}", AssistantTurn.class);

        assertThat(turn.reply()).isEqualTo("Hi");
    }

    @Test
    void extractJsonLeavesCleanJsonAlone() {
        assertThat(StructuredOutputParser.extractJson("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    // ----- bad answers: all must be BAD_RESPONSE -----

    @Test
    void textThatIsNotJsonIsRejected() {
        assertBad("I think you should start with the basics.", GeneratedPlan.class);
        assertBad("{\"goalTitle\": \"Learn Java\", \"subgoals\": [", GeneratedPlan.class);
    }

    @Test
    void emptyOrHugeAnswersAreRejected() {
        assertBad(null, GeneratedPlan.class);
        assertBad("   ", GeneratedPlan.class);
        assertBad("{\"reply\":\"" + "a".repeat(100_001) + "\",\"readyToGenerate\":false}", AssistantTurn.class);
    }

    @Test
    void aPlanWithoutSubgoalsIsRejected() {
        assertBad("{\"goalTitle\":\"Learn Java\",\"subgoals\":[]}", GeneratedPlan.class);
        assertBad("{\"goalTitle\":\"Learn Java\"}", GeneratedPlan.class);
    }

    @Test
    void hoursMustBePositiveAndInRange() {
        assertBad(plan("{\"title\":\"A\",\"estimatedHours\":0}"), GeneratedPlan.class);
        assertBad(plan("{\"title\":\"A\",\"estimatedHours\":-5}"), GeneratedPlan.class);
        assertBad(plan("{\"title\":\"A\",\"estimatedHours\":0.1}"), GeneratedPlan.class);
        assertBad(plan("{\"title\":\"A\",\"estimatedHours\":1000.01}"), GeneratedPlan.class);
        assertBad(plan("{\"title\":\"A\",\"estimatedHours\":1.234}"), GeneratedPlan.class);
        assertBad(plan("{\"title\":\"A\"}"), GeneratedPlan.class);
    }

    @Test
    void titlesMustFitTheLimits() {
        assertBad(plan("{\"title\":\"  \",\"estimatedHours\":5}"), GeneratedPlan.class);
        assertBad(plan("{\"title\":\"" + "a".repeat(121) + "\",\"estimatedHours\":5}"), GeneratedPlan.class);
        assertBad("{\"goalTitle\":\"" + "a".repeat(121) + "\",\"subgoals\":[{\"title\":\"A\",\"estimatedHours\":5}]}",
                GeneratedPlan.class);
    }

    @Test
    void moreThanFiftySubgoalsAreRejected() {
        String one = "{\"title\":\"A\",\"estimatedHours\":1}";
        String fiftyOne = String.join(",", java.util.Collections.nCopies(51, one));

        assertBad("{\"goalTitle\":\"Big\",\"subgoals\":[" + fiftyOne + "]}", GeneratedPlan.class);
    }

    @Test
    void aBlankReplyIsRejected() {
        assertBad("{\"reply\":\"  \",\"readyToGenerate\":false}", AssistantTurn.class);
    }

    @Test
    void absurdAvailabilityIsRejected() {
        assertBad("{\"reply\":\"Hi\",\"readyToGenerate\":false,\"facts\":{\"hoursPerWeek\":200}}", AssistantTurn.class);
    }

    private static String plan(String subgoalJson) {
        return "{\"goalTitle\":\"Learn Java\",\"subgoals\":[" + subgoalJson + "]}";
    }

    private void assertBad(String raw, Class<?> type) {
        assertThatThrownBy(() -> parser.parse(raw, type))
                .isInstanceOfSatisfying(AIProviderException.class,
                        ex -> assertThat(ex.getType()).isEqualTo(AIErrorType.BAD_RESPONSE));
    }
}
