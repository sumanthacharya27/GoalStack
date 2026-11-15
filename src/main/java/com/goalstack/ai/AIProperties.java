package com.goalstack.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings under "app.ai" in application.properties. Secrets (the optional API key) are read from
 * environment variables and are only ever used by the backend.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AIProperties(
        /** "fake" (built-in demo, no model needed) or "ollama". */
        @DefaultValue("fake") String provider,
        @DefaultValue Http http,
        @DefaultValue Retry retry,
        @DefaultValue Ollama ollama,
        @DefaultValue Fake fake) {

    public record Http(
            @DefaultValue("5s") Duration connectTimeout,
            /** Local models can take a while, especially on the first call while the model loads. */
            @DefaultValue("120s") Duration readTimeout) {
    }

    public record Retry(
            /** Extra attempts after a busy or unavailable answer. 0 turns retries off. */
            @DefaultValue("1") int maxRetries,
            /** Wait before retry 1; retry 2 waits twice as long, and so on. */
            @DefaultValue("500ms") Duration backoff) {
    }

    public record Ollama(
            @DefaultValue("http://localhost:11434") String baseUrl,
            @DefaultValue("llama3.1:8b") String model,
            /** Context window in tokens. Sent on every call so long chats are not silently cut. */
            @DefaultValue("8192") int numCtx,
            /** How long Ollama keeps the model in memory after a call. */
            @DefaultValue("5m") String keepAlive,
            /** Optional bearer token, for an Ollama behind a gateway. Empty for a normal local install. */
            String apiKey) {
    }

    public record Fake(
            @DefaultValue("NONE") FailureMode failureMode,
            /** Artificial waiting time, to see loading states in the UI. */
            @DefaultValue("0ms") Duration delay) {

        /** Makes the fake provider fail on purpose, to test error handling in the UI. */
        public enum FailureMode {
            NONE, UNAVAILABLE, RATE_LIMITED, TIMEOUT, BAD_JSON
        }
    }
}
