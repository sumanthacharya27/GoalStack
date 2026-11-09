package com.goalstack.dto;

import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;

import java.time.LocalDateTime;

/** JSON returned to the React app for one subgoal. */
public record SubgoalResponse(Long id, Long goalId, String title, String description,
                              SubgoalStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static SubgoalResponse from(Subgoal s) {
        return new SubgoalResponse(s.getId(), s.getGoal().getId(), s.getTitle(), s.getDescription(),
                s.getStatus(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
