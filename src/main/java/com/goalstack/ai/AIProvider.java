package com.goalstack.ai;

/**
 * The only thing the rest of GoalStack knows about an AI vendor. Planning code depends on this interface,
 * never on a vendor library. A provider turns a vendor-neutral request into a vendor call, and maps every
 * vendor problem to an {@link AIProviderException}. A provider never touches the database.
 */
public interface AIProvider {

    /** Sends the conversation and returns the model's answer. */
    AIProviderResponse generate(AIProviderRequest request);

    /** A quick check that the provider is usable. Must not throw; returns an unavailable status instead. */
    AIStatus checkStatus();

    /** Short provider name, for example "ollama" or "fake". */
    String name();

    /** The configured model name. */
    String modelName();
}
