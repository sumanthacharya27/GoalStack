package com.goalstack.dto;

import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;

import java.util.List;

/** A goal plus its calculated values (progress, counts, complete, overdue) for the templates. */
public class GoalView {

    private final Goal goal;
    private final List<Subgoal> subgoals;
    private final int completedCount;
    private final int totalCount;
    private final int progress;
    private final boolean complete;
    private final boolean overdue;

    public GoalView(Goal goal, List<Subgoal> subgoals, int completedCount, int totalCount,
                    int progress, boolean complete, boolean overdue) {
        this.goal = goal;
        this.subgoals = subgoals;
        this.completedCount = completedCount;
        this.totalCount = totalCount;
        this.progress = progress;
        this.complete = complete;
        this.overdue = overdue;
    }

    public Goal getGoal() { return goal; }
    public List<Subgoal> getSubgoals() { return subgoals; }
    public int getCompletedCount() { return completedCount; }
    public int getTotalCount() { return totalCount; }
    public int getProgress() { return progress; }
    public boolean isComplete() { return complete; }
    public boolean isOverdue() { return overdue; }
}
