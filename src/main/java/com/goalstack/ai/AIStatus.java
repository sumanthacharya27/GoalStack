package com.goalstack.ai;

/** Whether the AI provider can be used right now. */
public record AIStatus(String provider, String model, boolean available, Availability availability) {

    public enum Availability {
        /** A real provider is reachable and the model is installed. */
        AVAILABLE,
        /** The built-in fake provider is active (no real model). */
        DEMO,
        /** The provider cannot be reached. */
        UNREACHABLE,
        /** The provider is reachable but the configured model is not installed. */
        MODEL_MISSING
    }
}
