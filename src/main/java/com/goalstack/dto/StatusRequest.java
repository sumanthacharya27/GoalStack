package com.goalstack.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.goalstack.entity.Goal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JSON returned to the React app for one goal, including the calculated values.
 * "subgoals" is only present on the single-goal endpoints, not in the dashboard list.
 */
public record GoalResponse(Long id, String title, String description, LocalDate deadline,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           int completedCount, int totalCount, int progress,
                           boolean complete, boolean overdue,
                           @JsonInclude(JsonInclude.Include.NON_NULL) List<SubgoalResponse> subgoals) {

    public static GoalResponse from(GoalView view, boolean includeSubgoals) {
        Goal g = view.getGoal();
        List<SubgoalResponse> subgoals = includeSubgoals
                ? view.getSubgoals().stream().map(SubgoalResponse::from).toList()
                : null;
        return new GoalResponse(g.getId(), g.getTitle(), g.getDescription(), g.getDeadline(),
                g.getCreatedAt(), g.getUpdatedAt(), view.getCompletedCount(), view.getTotalCount(),
                view.getProgress(), view.isComplete(), view.isOverdue(), subgoals);
    }
}
