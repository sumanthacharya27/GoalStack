package com.goalstack.dto;
import com.goalstack.entity.Goal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** JSON body for creating or updating a goal. Dates use the format yyyy-MM-dd. */
public class GoalRequest {

    @NotBlank(message = "Title is required.")
    @Size(max = 120, message = "Title must be 120 characters or fewer.")
    private String title;

    @Size(max = 5000, message = "Description must be 5000 characters or fewer.")
    private String description;

    private LocalDate deadline;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public static GoalRequest from(Goal goal) {
        GoalRequest request = new GoalRequest();

        request.setTitle(goal.getTitle());
        request.setDescription(goal.getDescription());
        request.setDeadline(goal.getDeadline());

        return request;
    }
}
