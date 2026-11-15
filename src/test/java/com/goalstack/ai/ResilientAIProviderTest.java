package com.goalstack.ai;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilientAIProviderTest {

    private final List<Duration> pauses = new ArrayList<>();

    private ResilientAIProvider wrap(ScriptedAIProvider provider, int maxRetries) {
        return new ResilientAIProvider(provider, maxRetries, Duration.ofMillis(500), pauses::add);
    }

    @Test
    void aBriefOutageIsRetriedOnceAndThenSucceeds() {
        ScriptedAIProvider provider = new ScriptedAIProvider().fail(AIErrorType.UNAVAILABLE).reply("ok");

        AIProviderResponse response = wrap(provider, 1).generate(ScriptedAIProvider.simpleRequest());

        assertThat(response.content()).isEqualTo("ok");
        assertThat(provider.requests).hasSize(2);
        assertThat(pauses).containsExactly(Duration.ofMillis(500));
    }

    @Test
    void aBusyServerIsRetriedToo() {
        ScriptedAIProvider provider = new ScriptedAIProvider().fail(AIErrorType.RATE_LIMITED).reply("ok");

        assertThat(wrap(provider, 1).generate(ScriptedAIProvider.simpleRequest()).content()).isEqualTo("ok");
    }

    @Test
    void itGivesUpAfterTheConfiguredRetriesAndWaitsLongerEachTime() {
        ScriptedAIProvider provider = new ScriptedAIProvider()
                .fail(AIErrorType.UNAVAILABLE).fail(AIErrorType.UNAVAILABLE).fail(AIErrorType.UNAVAILABLE).reply("late");

        assertThatThrownBy(() -> wrap(provider, 2).generate(ScriptedAIProvider.simpleRequest()))
                .isInstanceOfSatisfying(AIProviderException.class,
                        ex -> assertThat(ex.getType()).isEqualTo(AIErrorType.UNAVAILABLE));
        assertThat(provider.requests).hasSize(3);
        assertThat(pauses).containsExactly(Duration.ofMillis(500), Duration.ofMillis(1000));
    }

    @Test
    void problemsThatRepeatingCannotFixAreNotRetried() {
        for (AIErrorType type : List.of(AIErrorType.NOT_CONFIGURED, AIErrorType.BAD_RESPONSE,
                AIErrorType.TIMEOUT, AIErrorType.REQUEST_REJECTED)) {
            ScriptedAIProvider provider = new ScriptedAIProvider().fail(type).reply("never reached");

            assertThatThrownBy(() -> wrap(provider, 3).generate(ScriptedAIProvider.simpleRequest()))
                    .isInstanceOfSatisfying(AIProviderException.class, ex -> assertThat(ex.getType()).isEqualTo(type));
            assertThat(provider.requests).as("calls for %s", type).hasSize(1);
        }
        assertThat(pauses).isEmpty();
    }

    @Test
    void zeroRetriesMeansNoRetry() {
        ScriptedAIProvider provider = new ScriptedAIProvider().fail(AIErrorType.UNAVAILABLE).reply("ok");

        assertThatThrownBy(() -> wrap(provider, 0).generate(ScriptedAIProvider.simpleRequest()))
                .isInstanceOf(AIProviderException.class);
        assertThat(provider.requests).hasSize(1);
    }

    @Test
    void statusAndNamesComeFromTheWrappedProvider() {
        ResilientAIProvider resilient = wrap(new ScriptedAIProvider(), 1);

        assertThat(resilient.name()).isEqualTo("scripted");
        assertThat(resilient.modelName()).isEqualTo("scripted-model");
        assertThat(resilient.checkStatus().available()).isTrue();
    }
}
