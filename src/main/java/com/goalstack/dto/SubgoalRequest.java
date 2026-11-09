package com.goalstack.dto;

import com.goalstack.entity.SubgoalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** JSON body for creating or updating a subgoal. Status is ignored on creation (always PENDING). */
public class SubgoalRequest {

    @NotBlank(message = "Title is required.")
    @Size(max = 120, message = "Title must be 120 characters or fewer.")
    private String title;

    @Size(max = 5000, message = "Description must be 5000 characters or fewer.")
    private String description;

    @NotNull(message = "Status is required.")
    private SubgoalStatus status = SubgoalStatus.PENDING;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SubgoalStatus getStatus() { return status; }
    public void setStatus(SubgoalStatus status) { this.status = status; }
}
