package com.goalstack.dto;

import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

/** JSON returned to the React app for one subgoal. "estimatedHours" is null for subgoals of older goals. */
public record SubgoalResponse(Long id, Long goalId, String title, String description,
                              SubgoalStatus status, LocalDate targetDate, BigDecimal estimatedHours,
                              BigDecimal weight, int sortOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static SubgoalResponse from(Subgoal s) {
        return new SubgoalResponse(s.getId(), s.getGoal().getId(), s.getTitle(), s.getDescription(),
                s.getStatus(), s.getTargetDate(), s.getEstimatedHours(), s.getWeight(), s.getSortOrder(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
