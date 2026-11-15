package com.goalstack.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns the model's text into a checked Java object. Model output is untrusted: it may be wrapped in chatter or
 * code fences, may be invalid JSON, or may break the limits. Anything wrong becomes an AIProviderException(BAD_RESPONSE).
 */
@Component
public class StructuredOutputParser {

    private static final int MAX_CHARACTERS = 100_000;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public StructuredOutputParser(ObjectMapper objectMapper, Validator validator) {
        // a private copy, so unknown fields are ignored here without changing the app-wide setting
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.validator = validator;
    }

    public <T> T parse(String raw, Class<T> type) {
        if (raw == null || raw.isBlank()) {
            throw bad("The model returned nothing", null);
        }
        if (raw.length() > MAX_CHARACTERS) {
            throw bad("The model answer is too long (" + raw.length() + " characters)", null);
        }

        T value;
        try {
            value = objectMapper.readValue(extractJson(raw), type);
        } catch (JsonProcessingException ex) {
            throw bad("The answer is not valid JSON for " + type.getSimpleName() + ": " + shorten(ex.getOriginalMessage(), 200), ex);
        }
        if (value == null) {
            throw bad("The answer was empty JSON", null);
        }

        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            String summary = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .limit(5)
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw bad("The answer breaks the rules: " + summary, null);
        }
        return value;
    }

    /** Removes code fences and any text before the first "{" or after the last "}". */
    static String extractJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            text = firstNewline >= 0 ? text.substring(firstNewline + 1) : text.substring(3);
            if (text.trim().endsWith("```")) {
                text = text.trim();
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start && (start > 0 || end < text.length() - 1)) {
            text = text.substring(start, end + 1);
        }
        return text;
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() > max ? oneLine.substring(0, max) : oneLine;
    }

    private static AIProviderException bad(String detail, Throwable cause) {
        return cause == null
                ? new AIProviderException(AIErrorType.BAD_RESPONSE, detail)
                : new AIProviderException(AIErrorType.BAD_RESPONSE, detail, cause);
    }
}
