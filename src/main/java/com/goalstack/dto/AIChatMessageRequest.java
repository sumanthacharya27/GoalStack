package com.goalstack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One message supplied by the authenticated planner UI. System messages are never accepted from clients. */
public record AIChatMessageRequest(
        @NotNull ChatRole role,
        @NotBlank @Size(max = 2000) String content) {

    public enum ChatRole {
        USER, ASSISTANT
    }
}
