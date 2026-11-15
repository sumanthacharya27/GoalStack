package com.goalstack.ai;

/**
 * What a provider returned.
 *
 * @param content          the model's text (JSON text for structured requests); never blank
 * @param model            the model that answered
 * @param promptTokens     tokens read, when the provider reports it (otherwise null)
 * @param completionTokens tokens written, when the provider reports it (otherwise null)
 * @param finishReason     for example "stop" or "length" (cut off); may be null
 */
public record AIProviderResponse(String content, String model, Integer promptTokens, Integer completionTokens,
                                 String finishReason) {
}
