package com.goalstack.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The model's structured answer to one chat message.
 *
 * @param reply           the text shown to the user (a question or a short answer)
 * @param readyToGenerate true when the assistant thinks it knows enough to write a plan
 * @param facts           everything learned so far (may be null)
 */
public record AssistantTurn(
        @NotBlank @Size(max = 4000) String reply,
        boolean readyToGenerate,
        @Valid PlanningFacts facts) {
}
