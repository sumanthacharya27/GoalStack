package com.goalstack.dto;

import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.WeightingMode;

import java.math.BigDecimal;
import java.util.List;

/** A goal plus its calculated values (progress, counts, complete, overdue, total effort) for the API. */
public class GoalView {

    private final Goal goal;
    private final List<Subgoal> subgoals;
    private final int completedCount;
    private final int totalCount;
    private final BigDecimal progress;
    private final boolean complete;
    private final boolean overdue;
    private final BigDecimal totalEstimatedHours;

    public GoalView(Goal goal, List<Subgoal> subgoals, int completedCount, int totalCount,
                    BigDecimal progress, boolean complete, boolean overdue, BigDecimal totalEstimatedHours) {
        this.goal = goal;
        this.subgoals = subgoals;
        this.completedCount = completedCount;
        this.totalCount = totalCount;
        this.progress = progress;
        this.complete = complete;
        this.overdue = overdue;
        this.totalEstimatedHours = totalEstimatedHours;
    }

    public Goal getGoal() { return goal; }
    public List<Subgoal> getSubgoals() { return subgoals; }
    public int getCompletedCount() { return completedCount; }
    public int getTotalCount() { return totalCount; }
    /** Percent, 0.00 to 100.00 (sum of the weights of completed subgoals). */
    public BigDecimal getProgress() { return progress; }
    public boolean isComplete() { return complete; }
    public boolean isOverdue() { return overdue; }
    /** Sum of subgoal hours; null for goals that do not use effort weighting. */
    public BigDecimal getTotalEstimatedHours() { return totalEstimatedHours; }
    public WeightingMode getWeightingMode() { return goal.getWeightingMode(); }
}
