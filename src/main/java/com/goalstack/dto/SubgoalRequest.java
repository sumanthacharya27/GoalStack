package com.goalstack.dto;

import com.goalstack.entity.SubgoalStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** JSON body for creating or updating a subgoal. Status is ignored on creation (always PENDING). */
public class SubgoalRequest {

    @NotBlank(message = "Title is required.")
    @Size(max = 120, message = "Title must be 120 characters or fewer.")
    private String title;

    @Size(max = 5000, message = "Description must be 5000 characters or fewer.")
    private String description;

    // Optional schedule date shown on the calendar.
    private LocalDate targetDate;

    // Required for goals made in Version 3 or later; not used for older goals. The service checks which applies.
    @DecimalMin(value = "0.25", message = "Estimated hours must be at least 0.25.")
    @DecimalMax(value = "1000.00", message = "Estimated hours must be 1000 or fewer.")
    @Digits(integer = 4, fraction = 2, message = "Use at most 2 decimal places.")
    private BigDecimal estimatedHours;

    @NotNull(message = "Status is required.")
    private SubgoalStatus status = SubgoalStatus.PENDING;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }

    public SubgoalStatus getStatus() { return status; }
    public void setStatus(SubgoalStatus status) { this.status = status; }
}
