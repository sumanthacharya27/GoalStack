package com.goalstack.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Asks the provider for a structured answer and returns it as a checked object.
 * If the answer cannot be used, it asks the model ONCE to fix it. Provider problems (busy, down, slow) are not
 * repeated here: the provider wrapper already handles those.
 */
@Service
public class StructuredAIClient {

    private static final Logger log = LoggerFactory.getLogger(StructuredAIClient.class);

    private final AIProvider provider;
    private final StructuredOutputParser parser;

    public StructuredAIClient(AIProvider provider, StructuredOutputParser parser) {
        this.provider = provider;
        this.parser = parser;
    }

    public <T> T generate(AIProviderRequest request, Class<T> type) {
        AIProviderResponse first = provider.generate(request);
        try {
            return parser.parse(first.content(), type);
        } catch (AIProviderException problem) {
            if (problem.getType() != AIErrorType.BAD_RESPONSE) {
                throw problem;
            }
            log.warn("AI answer was not usable ({}). Asking the model once more.", problem.getMessage());
            AIProviderResponse second = provider.generate(repairRequest(request, first.content(), problem.getMessage()));
            return parser.parse(second.content(), type);
        }
    }

    private AIProviderRequest repairRequest(AIProviderRequest original, String badAnswer, String reason) {
        List<AIMessage> messages = new ArrayList<>(original.messages());
        messages.add(AIMessage.assistant(shorten(badAnswer, 2000)));
        messages.add(AIMessage.user("Your previous reply could not be used (" + shorten(reason, 500) + "). "
                + "Reply again with ONLY one valid JSON object that follows the required format. "
                + "No explanation and no code fences."));
        return original.withMessages(messages);
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
