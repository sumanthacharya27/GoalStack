package com.goalstack.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** One subgoal proposed by the model. The limits are the same as for manual subgoals. */
public record PlanSubgoal(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.25") @DecimalMax("1000.00") @Digits(integer = 4, fraction = 2) BigDecimal estimatedHours) {
}
