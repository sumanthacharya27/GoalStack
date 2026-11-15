package com.goalstack.service;

import com.goalstack.dto.GoalRequest;
import com.goalstack.dto.GoalView;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.entity.WeightingMode;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import com.goalstack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All goal business logic lives here: CRUD, completion and overdue rules.
 * Progress maths is in WeightedProgressService. Every method takes the id of the logged-in user
 * and only touches that user's goals.
 */
@Service
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final SubgoalRepository subgoalRepository;
    private final UserRepository userRepository;
    private final WeightedProgressService weightedProgressService;

    public GoalService(GoalRepository goalRepository, SubgoalRepository subgoalRepository,
                       UserRepository userRepository, WeightedProgressService weightedProgressService) {
        this.goalRepository = goalRepository;
        this.subgoalRepository = subgoalRepository;
        this.userRepository = userRepository;
        this.weightedProgressService = weightedProgressService;
    }

    // ---------- reading ----------

    /** Dashboard list: two queries in total (this user's goals + one grouped query for counts, weights, hours). */
    public List<GoalView> getAllGoalViews(Long userId) {
        Map<Long, Totals> totalsByGoal = new HashMap<>();
        for (Object[] row : subgoalRepository.countByGoalAndStatusForUser(userId)) {
            Long goalId = (Long) row[0];
            SubgoalStatus status = (SubgoalStatus) row[1];
            int count = ((Number) row[2]).intValue();
            BigDecimal weightSum = asDecimal(row[3]);
            BigDecimal hoursSum = asDecimal(row[4]);

            Totals totals = totalsByGoal.computeIfAbsent(goalId, key -> new Totals());
            totals.total += count;
            if (status == SubgoalStatus.COMPLETED) {
                totals.completed += count;
                if (weightSum != null) {
                    totals.completedWeight = totals.completedWeight.add(weightSum);
                }
            }
            if (hoursSum != null) {
                totals.hours = totals.hours.add(hoursSum);
            }
        }

        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(goal -> {
                    Totals t = totalsByGoal.getOrDefault(goal.getId(), new Totals());
                    BigDecimal hours = goal.getWeightingMode() == WeightingMode.EFFORT ? t.hours : null;
                    return buildView(goal, List.of(), t.total, t.completed,
                            weightedProgressService.clampProgress(t.completedWeight), hours);
                })
                .toList();
    }

    public GoalView getGoalView(Long userId, Long id) {
        Goal goal = findGoal(userId, id);
        List<Subgoal> subgoals = subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(id);
        int completed = (int) subgoals.stream()
                .filter(s -> s.getStatus() == SubgoalStatus.COMPLETED)
                .count();
        BigDecimal hours = goal.getWeightingMode() == WeightingMode.EFFORT
                ? weightedProgressService.totalHours(subgoals) : null;
        return buildView(goal, subgoals, subgoals.size(), completed, weightedProgressService.progress(subgoals), hours);
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
        goal.setWeightingMode(WeightingMode.EFFORT); // every goal made now uses effort-based weights
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

    /** A goal is complete only when it has at least one subgoal and all of them are COMPLETED. */
    public static boolean isComplete(int completedCount, int totalCount) {
        return totalCount > 0 && completedCount == totalCount;
    }

    /** Overdue = deadline is before today AND the goal is not complete. */
    public static boolean isOverdue(LocalDate deadline, boolean complete, LocalDate today) {
        return deadline != null && deadline.isBefore(today) && !complete;
    }

    private GoalView buildView(Goal goal, List<Subgoal> subgoals, int total, int completed,
                               BigDecimal progress, BigDecimal totalHours) {
        boolean complete = isComplete(completed, total);
        boolean overdue = isOverdue(goal.getDeadline(), complete, LocalDate.now());
        return new GoalView(goal, subgoals, completed, total, progress, complete, overdue, totalHours);
    }

    private void copyRequestToGoal(GoalRequest request, Goal goal) {
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(blankToNull(request.getDescription()));
        goal.setDeadline(request.getDeadline());
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    /** Running totals for one goal while reading the grouped dashboard query. */
    private static class Totals {
        int total;
        int completed;
        BigDecimal completedWeight = BigDecimal.ZERO;
        BigDecimal hours = BigDecimal.ZERO.setScale(2);
    }
}
