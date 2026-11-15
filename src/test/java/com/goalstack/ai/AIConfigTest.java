package com.goalstack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AIConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static AIProperties properties(String provider) {
        return new AIProperties(provider,
                new AIProperties.Http(Duration.ofSeconds(5), Duration.ofSeconds(120)),
                new AIProperties.Retry(1, Duration.ofMillis(500)),
                new AIProperties.Ollama("http://localhost:11434", "qwen2.5:7b", 8192, "5m", null),
                new AIProperties.Fake(AIProperties.Fake.FailureMode.NONE, Duration.ZERO));
    }

    // ----- choosing a provider -----

    @Test
    void theFakeProviderIsChosenByName() {
        AIProvider provider = new AIConfig().aiProvider(properties("fake"), objectMapper);

        assertThat(provider.name()).isEqualTo("fake");
    }

    @Test
    void theOllamaProviderIsChosenByNameAndUsesTheConfiguredModel() {
        AIProvider provider = new AIConfig().aiProvider(properties("ollama"), objectMapper);

        assertThat(provider.name()).isEqualTo("ollama");
        assertThat(provider.modelName()).isEqualTo("qwen2.5:7b");
    }

    @Test
    void theNameIgnoresCaseAndSpaces() {
        assertThat(new AIConfig().aiProvider(properties("  OLLAMA "), objectMapper).name()).isEqualTo("ollama");
    }

    @Test
    void anUnknownProviderStopsTheAppWithAClearMessage() {
        assertThatThrownBy(() -> new AIConfig().aiProvider(properties("bogus"), objectMapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bogus")
                .hasMessageContaining("'fake' or 'ollama'");
    }

    // ----- reading the settings -----

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AIProperties.class)
    static class PropertiesOnly {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(PropertiesOnly.class);

    @Test
    void settingsHaveSafeDefaults() {
        runner.run(context -> {
            AIProperties p = context.getBean(AIProperties.class);

            assertThat(p.provider()).isEqualTo("fake");
            assertThat(p.http().connectTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(p.http().readTimeout()).isEqualTo(Duration.ofSeconds(120));
            assertThat(p.retry().maxRetries()).isEqualTo(1);
            assertThat(p.retry().backoff()).isEqualTo(Duration.ofMillis(500));
            assertThat(p.ollama().baseUrl()).isEqualTo("http://localhost:11434");
            assertThat(p.ollama().model()).isEqualTo("llama3.1:8b");
            assertThat(p.ollama().numCtx()).isEqualTo(8192);
            assertThat(p.ollama().keepAlive()).isEqualTo("5m");
            assertThat(p.fake().failureMode()).isEqualTo(AIProperties.Fake.FailureMode.NONE);
        });
    }

    @Test
    void settingsCanBeOverridden() {
        runner.withPropertyValues(
                        "app.ai.provider=ollama",
                        "app.ai.ollama.model=qwen2.5:7b",
                        "app.ai.ollama.num-ctx=4096",
                        "app.ai.http.read-timeout=30s",
                        "app.ai.retry.max-retries=0",
                        "app.ai.fake.failure-mode=TIMEOUT")
                .run(context -> {
                    AIProperties p = context.getBean(AIProperties.class);

                    assertThat(p.provider()).isEqualTo("ollama");
                    assertThat(p.ollama().model()).isEqualTo("qwen2.5:7b");
                    assertThat(p.ollama().numCtx()).isEqualTo(4096);
                    assertThat(p.http().readTimeout()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(p.retry().maxRetries()).isZero();
                    assertThat(p.fake().failureMode()).isEqualTo(AIProperties.Fake.FailureMode.TIMEOUT);
                });
    }
}
