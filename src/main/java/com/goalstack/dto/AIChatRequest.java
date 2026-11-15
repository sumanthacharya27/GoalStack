package com.goalstack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Short, client-held conversation history for the demo planner. */
public record AIChatRequest(
        @NotEmpty @Size(max = 20) List<@Valid AIChatMessageRequest> messages) {
}
