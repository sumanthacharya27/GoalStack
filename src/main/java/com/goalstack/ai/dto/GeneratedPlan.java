package com.goalstack.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A plan proposed by the model. It has no weights and no deadline on purpose: the backend calculates weights from
 * the hours, and works out the deadline from the total hours and the user's available time. The model only supplies
 * the content, an effort estimate and the assumptions it made.
 */
public record GeneratedPlan(
        @NotBlank @Size(max = 120) String goalTitle,
        @Size(max = 5000) String goalDescription,
        @NotEmpty @Size(max = 50) List<@Valid PlanSubgoal> subgoals,
        @Size(max = 10) List<@Size(max = 300) String> assumptions) {
}
