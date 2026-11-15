package com.goalstack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goalstack.ai.dto.AssistantTurn;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredAIClientTest {

    private static final String GOOD = "{\"reply\":\"Hello\",\"readyToGenerate\":false}";
    private static final String BAD = "Sorry, no JSON today.";

    private final StructuredOutputParser parser = new StructuredOutputParser(
            new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void aGoodAnswerNeedsOneCall() {
        ScriptedAIProvider provider = new ScriptedAIProvider().reply(GOOD);

        AssistantTurn turn = new StructuredAIClient(provider, parser)
                .generate(ScriptedAIProvider.simpleRequest(), AssistantTurn.class);

        assertThat(turn.reply()).isEqualTo("Hello");
        assertThat(provider.requests).hasSize(1);
    }

    @Test
    void aBadAnswerIsRepairedOnceAndTheModelIsToldWhatWentWrong() {
        ScriptedAIProvider provider = new ScriptedAIProvider().reply(BAD).reply(GOOD);

        AssistantTurn turn = new StructuredAIClient(provider, parser)
                .generate(ScriptedAIProvider.simpleRequest(), AssistantTurn.class);

        assertThat(turn.reply()).isEqualTo("Hello");
        assertThat(provider.requests).hasSize(2);
        List<AIMessage> second = provider.requests.get(1).messages();
        assertThat(second).hasSize(provider.requests.get(0).messages().size() + 2);
        assertThat(second.get(second.size() - 2).role()).isEqualTo(AIRole.ASSISTANT);
        assertThat(second.get(second.size() - 2).content()).isEqualTo(BAD);
        assertThat(second.get(second.size() - 1).role()).isEqualTo(AIRole.USER);
        assertThat(second.get(second.size() - 1).content()).contains("ONLY one valid JSON object");
    }

    @Test
    void ifTheRepairFailsToo_itGivesUpAfterExactlyTwoCalls() {
        ScriptedAIProvider provider = new ScriptedAIProvider().reply(BAD).reply(BAD).reply(GOOD);

        assertThatThrownBy(() -> new StructuredAIClient(provider, parser)
                .generate(ScriptedAIProvider.simpleRequest(), AssistantTurn.class))
                .isInstanceOfSatisfying(AIProviderException.class,
                        ex -> assertThat(ex.getType()).isEqualTo(AIErrorType.BAD_RESPONSE));
        assertThat(provider.requests).hasSize(2);
    }

    @Test
    void providerProblemsAreNotRepeatedHere() {
        ScriptedAIProvider provider = new ScriptedAIProvider().fail(AIErrorType.UNAVAILABLE).reply(GOOD);

        assertThatThrownBy(() -> new StructuredAIClient(provider, parser)
                .generate(ScriptedAIProvider.simpleRequest(), AssistantTurn.class))
                .isInstanceOfSatisfying(AIProviderException.class,
                        ex -> assertThat(ex.getType()).isEqualTo(AIErrorType.UNAVAILABLE));
        assertThat(provider.requests).hasSize(1);
    }
}
