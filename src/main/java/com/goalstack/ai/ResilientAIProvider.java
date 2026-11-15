package com.goalstack.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Wraps a real provider and repeats a call a limited number of times when the problem is probably
 * short-lived (server busy or briefly unavailable). Other problems are passed on straight away.
 */
public class ResilientAIProvider implements AIProvider {

    /** Pauses between attempts. Replaced in tests so they do not wait. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private static final Logger log = LoggerFactory.getLogger(ResilientAIProvider.class);

    private final AIProvider delegate;
    private final int maxRetries;
    private final Duration backoff;
    private final Sleeper sleeper;

    public ResilientAIProvider(AIProvider delegate, int maxRetries, Duration backoff) {
        this(delegate, maxRetries, backoff, duration -> Thread.sleep(duration.toMillis()));
    }

    public ResilientAIProvider(AIProvider delegate, int maxRetries, Duration backoff, Sleeper sleeper) {
        this.delegate = delegate;
        this.maxRetries = Math.max(0, maxRetries);
        this.backoff = backoff;
        this.sleeper = sleeper;
    }

    @Override
    public AIProviderResponse generate(AIProviderRequest request) {
        int retriesUsed = 0;
        while (true) {
            try {
                return delegate.generate(request);
            } catch (AIProviderException ex) {
                if (!ex.getType().isRetryable() || retriesUsed >= maxRetries) {
                    throw ex;
                }
                retriesUsed++;
                Duration wait = backoff.multipliedBy(retriesUsed);
                log.warn("AI call failed ({}), retry {} of {} in {} ms", ex.getType(), retriesUsed, maxRetries,
                        wait.toMillis());
                pause(wait, ex);
            }
        }
    }

    private void pause(Duration wait, AIProviderException original) {
        try {
            sleeper.sleep(wait);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AIProviderException(AIErrorType.UNAVAILABLE, "Interrupted while waiting to retry", original);
        }
    }

    @Override
    public AIStatus checkStatus() {
        return delegate.checkStatus();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public String modelName() {
        return delegate.modelName();
    }
}
