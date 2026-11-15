package com.goalstack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/** Chooses the AI provider from the settings. The rest of the app only sees the {@link AIProvider} interface. */
@Configuration
@EnableConfigurationProperties(AIProperties.class)
public class AIConfig {

    @Bean
    public AIProvider aiProvider(AIProperties properties, ObjectMapper objectMapper) {
        String chosen = properties.provider() == null ? "" : properties.provider().trim().toLowerCase(Locale.ROOT);
        AIProvider provider = switch (chosen) {
            case "fake" -> new FakeAIProvider(properties.fake(), objectMapper);
            case "ollama" -> new OllamaProvider(properties, objectMapper);
            default -> throw new IllegalStateException(
                    "Unknown app.ai.provider '" + properties.provider() + "'. Use 'fake' or 'ollama'.");
        };
        return new ResilientAIProvider(provider, properties.retry().maxRetries(), properties.retry().backoff());
    }
}
