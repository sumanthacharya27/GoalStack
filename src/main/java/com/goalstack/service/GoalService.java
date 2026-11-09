package com.goalstack.service;

import com.goalstack.entity.Goal;
import com.goalstack.dto.GoalRequest;
import com.goalstack.dto.GoalView;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** All goal business logic lives here: CRUD, progress, completion and overdue rules. */
@Service
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final SubgoalRepository subgoalRepository;

    public GoalService(GoalRepository goalRepository, SubgoalRepository subgoalRepository) {
        this.goalRepository = goalRepository;
        this.subgoalRepository = subgoalRepository;
    }

    // ---------- reading ----------

    public List<GoalView> getAllGoalViews() {
        return goalRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(goal -> buildView(goal, goal.getSubgoals()))
                .toList();
    }

    public GoalView getGoalView(Long id) {
        Goal goal = findGoal(id);
        return buildView(goal, subgoalRepository.findByGoalIdOrderByIdAsc(id));
    }

    public GoalRequest getGoalRequest(Long id) {
        return GoalRequest.from(findGoal(id));
    }

    public Goal findGoal(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal with id " + id + " was not found."));
    }

    // ---------- writing ----------

    @Transactional
    public Goal createGoal(GoalRequest form) {
        Goal goal = new Goal();
        copyFormToGoal(form, goal);
        return goalRepository.save(goal);
    }

    @Transactional
    public Goal updateGoal(Long id, GoalRequest form) {
        Goal goal = findGoal(id);
        copyFormToGoal(form, goal);
        return goalRepository.save(goal);
    }

    /** Deleting a goal also deletes its subgoals (JPA cascade + ON DELETE CASCADE in MySQL). */
    @Transactional
    public void deleteGoal(Long id) {
        goalRepository.delete(findGoal(id));
    }

    // ---------- business rules ----------

    /** Progress = completed / total * 100, rounded DOWN, always between 0 and 100. */
    public static int calculateProgress(int completedCount, int totalCount) {
        if (totalCount <= 0) {
            return 0;
        }
        int percent = (int) (completedCount * 100L / totalCount);
        return Math.max(0, Math.min(100, percent));
    }

    /** A goal is complete only when it has at least one subgoal and all of them are COMPLETED. */
    public static boolean isComplete(int completedCount, int totalCount) {
        return totalCount > 0 && completedCount == totalCount;
    }

    /** Overdue = deadline is before today AND the goal is not complete. */
    public static boolean isOverdue(LocalDate deadline, boolean complete, LocalDate today) {
        return deadline != null && deadline.isBefore(today) && !complete;
    }

    private GoalView buildView(Goal goal, List<Subgoal> subgoals) {
        int total = subgoals.size();
        int completed = (int) subgoals.stream()
                .filter(s -> s.getStatus() == SubgoalStatus.COMPLETED)
                .count();
        boolean complete = isComplete(completed, total);
        boolean overdue = isOverdue(goal.getDeadline(), complete, LocalDate.now());
        return new GoalView(goal, subgoals, completed, total, calculateProgress(completed, total), complete, overdue);
    }

    private void copyFormToGoal(GoalRequest form, Goal goal) {
        goal.setTitle(form.getTitle().trim());
        goal.setDescription(blankToNull(form.getDescription()));
        goal.setDeadline(form.getDeadline());
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

}
