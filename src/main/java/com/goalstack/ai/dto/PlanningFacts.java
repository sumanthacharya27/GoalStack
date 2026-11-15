package com.goalstack.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the assistant has learned about the user's goal so far. Every field may be missing until the user has told us.
 * The backend keeps these facts and sends them with every request, so details from early in a long chat are not lost.
 */
public record PlanningFacts(
        @Size(max = 1000) String desiredOutcome,
        @Size(max = 1000) String currentExperience,
        @Size(max = 1000) String existingKnowledge,
        @DecimalMin("0.5") @DecimalMax("168") BigDecimal hoursPerWeek,
        @Min(1) @Max(520) Integer timeframeWeeks,
        @Size(max = 10) List<@Size(max = 300) String> constraints) {
}
