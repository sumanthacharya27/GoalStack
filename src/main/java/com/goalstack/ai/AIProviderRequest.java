package com.goalstack.ai;

import java.util.List;
import java.util.Map;

/**
 * Everything a provider needs for one call, in a vendor-neutral form.
 *
 * @param messages        the conversation so far (system prompt first)
 * @param schemaName      a short name for the expected JSON shape (see {@link AISchemas}); null for free text
 * @param responseSchema  a JSON Schema the answer must follow; null for free text
 * @param temperature     0.0 = most predictable; null = provider default
 * @param maxOutputTokens upper limit for the answer length; null = provider default
 */
public record AIProviderRequest(List<AIMessage> messages, String schemaName, Map<String, Object> responseSchema,
                                Double temperature, Integer maxOutputTokens) {

    public AIProviderRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("An AI request needs at least one message.");
        }
        messages = List.copyOf(messages);
    }

    /** A structured request: the answer must be JSON that follows the schema. */
    public static AIProviderRequest structured(List<AIMessage> messages, String schemaName,
                                               Map<String, Object> schema, double temperature, int maxOutputTokens) {
        return new AIProviderRequest(messages, schemaName, schema, temperature, maxOutputTokens);
    }

    /** The same request with a different list of messages (used when asking the model to fix its answer). */
    public AIProviderRequest withMessages(List<AIMessage> newMessages) {
        return new AIProviderRequest(newMessages, schemaName, responseSchema, temperature, maxOutputTokens);
    }
}
