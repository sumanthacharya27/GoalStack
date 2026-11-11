package com.goalstack.service;

import com.goalstack.dto.GoalRequest;
import com.goalstack.dto.GoalView;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import com.goalstack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All goal business logic lives here: CRUD, progress, completion and overdue rules.
 * Every method takes the id of the logged-in user and only touches that user's goals.
 */
@Service
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final SubgoalRepository subgoalRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository, SubgoalRepository subgoalRepository,
                       UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.subgoalRepository = subgoalRepository;
        this.userRepository = userRepository;
    }

    // ---------- reading ----------

    /** Dashboard list: two queries in total (this user's goals + one grouped count). */
    public List<GoalView> getAllGoalViews(Long userId) {
        // goalId -> {total subgoals, completed subgoals}
        Map<Long, int[]> counts = new HashMap<>();
        for (Object[] row : subgoalRepository.countByGoalAndStatusForUser(userId)) {
            Long goalId = (Long) row[0];
            SubgoalStatus status = (SubgoalStatus) row[1];
            int count = ((Number) row[2]).intValue();
            int[] c = counts.computeIfAbsent(goalId, key -> new int[2]);
            c[0] += count;
            if (status == SubgoalStatus.COMPLETED) {
                c[1] += count;
            }
        }
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(goal -> {
                    int[] c = counts.getOrDefault(goal.getId(), new int[2]);
                    return buildView(goal, List.of(), c[0], c[1]);
                })
                .toList();
    }

    public GoalView getGoalView(Long userId, Long id) {
        Goal goal = findGoal(userId, id);
        List<Subgoal> subgoals = subgoalRepository.findByGoalIdOrderByIdAsc(id);
        int completed = (int) subgoals.stream()
                .filter(s -> s.getStatus() == SubgoalStatus.COMPLETED)
                .count();
        return buildView(goal, subgoals, subgoals.size(), completed);
    }

    /**
     * Finds a goal owned by this user. A goal that exists but belongs to someone else gives the
     * same "not found" error, so nobody can discover which ids exist.
     */
    public Goal findGoal(Long userId, Long id) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal with id " + id + " was not found."));
    }

    // ---------- writing ----------

    @Transactional
    public Goal createGoal(Long userId, GoalRequest request) {
        Goal goal = new Goal();
        goal.setUser(userRepository.getReferenceById(userId));
        copyRequestToGoal(request, goal);
        return goalRepository.save(goal);
    }

    @Transactional
    public Goal updateGoal(Long userId, Long id, GoalRequest request) {
        Goal goal = findGoal(userId, id);
        copyRequestToGoal(request, goal);
        return goalRepository.save(goal);
    }

    /** Deleting a goal also deletes its subgoals (JPA cascade + ON DELETE CASCADE in MySQL). */
    @Transactional
    public void deleteGoal(Long userId, Long id) {
        goalRepository.delete(findGoal(userId, id));
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

    private GoalView buildView(Goal goal, List<Subgoal> subgoals, int total, int completed) {
        boolean complete = isComplete(completed, total);
        boolean overdue = isOverdue(goal.getDeadline(), complete, LocalDate.now());
        return new GoalView(goal, subgoals, completed, total, calculateProgress(completed, total), complete, overdue);
    }

    private void copyRequestToGoal(GoalRequest request, Goal goal) {
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(blankToNull(request.getDescription()));
        goal.setDeadline(request.getDeadline());
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
