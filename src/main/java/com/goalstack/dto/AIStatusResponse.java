package com.goalstack.dto;

import com.goalstack.ai.AIStatus;

/** What the React app is told about the AI assistant. No addresses, keys or other internal details. */
public record AIStatusResponse(String provider, String model, boolean available, String availability, String message) {

    public static AIStatusResponse from(AIStatus status) {
        String message = switch (status.availability()) {
            case AVAILABLE -> "The AI assistant is ready.";
            case DEMO -> "Demo mode: answers come from a built-in fake AI, not a real model.";
            case UNREACHABLE -> "The AI assistant cannot be reached right now. You can still create goals manually.";
            case MODEL_MISSING -> "The configured AI model is not installed yet. You can still create goals manually.";
        };
        return new AIStatusResponse(status.provider(), status.model(), status.available(),
                status.availability().name(), message);
    }
}
