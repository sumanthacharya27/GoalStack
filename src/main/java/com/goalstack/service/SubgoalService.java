package com.goalstack.service;

import com.goalstack.dto.SubgoalRequest;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.entity.WeightingMode;
import com.goalstack.exception.InvalidStatusException;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.exception.ValidationFailedException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Subgoal business logic. A subgoal is only reachable if its goal belongs to the logged-in user.
 * New subgoals always start as PENDING. Adding, deleting or changing the hours of a subgoal recalculates
 * the weights of the whole goal inside the same transaction, with the goal row locked.
 */
@Service
@Transactional(readOnly = true)
public class SubgoalService {

    private final GoalRepository goalRepository;
    private final SubgoalRepository subgoalRepository;
    private final WeightedProgressService weightedProgressService;

    public SubgoalService(GoalRepository goalRepository, SubgoalRepository subgoalRepository,
                          WeightedProgressService weightedProgressService) {
        this.goalRepository = goalRepository;
        this.subgoalRepository = subgoalRepository;
        this.weightedProgressService = weightedProgressService;
    }

    public Subgoal findSubgoal(Long userId, Long id) {
        return subgoalRepository.findOwnedById(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subgoal with id " + id + " was not found."));
    }

    @Transactional
    public Subgoal createSubgoal(Long userId, Long goalId, SubgoalRequest request) {
        Goal goal = lockGoal(userId, goalId);
        List<Subgoal> subgoals = new ArrayList<>(subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(goalId));
        if (subgoals.size() >= WeightedProgressService.MAX_SUBGOALS_PER_GOAL) {
            throw new ValidationFailedException(
                    "A goal can have at most " + WeightedProgressService.MAX_SUBGOALS_PER_GOAL + " subgoals.");
        }
        BigDecimal hours = checkHours(goal, request.getEstimatedHours());

        Subgoal subgoal = new Subgoal();
        subgoal.setGoal(goal);
        subgoal.setTitle(request.getTitle().trim());
        subgoal.setDescription(blankToNull(request.getDescription()));
        subgoal.setStatus(SubgoalStatus.PENDING);
        subgoal.setTargetDate(request.getTargetDate());
        subgoal.setEstimatedHours(hours);

        subgoals.add(subgoal); // the new subgoal goes last
        weightedProgressService.applyWeights(goal.getWeightingMode(), subgoals);
        goal.touch();
        return subgoalRepository.save(subgoal);
    }

    @Transactional
    public Subgoal updateSubgoal(Long userId, Long id, SubgoalRequest request) {
        Subgoal subgoal = findSubgoal(userId, id);
        Goal goal = lockGoal(userId, subgoal.getGoal().getId());
        BigDecimal hours = checkHours(goal, request.getEstimatedHours());

        subgoal.setTitle(request.getTitle().trim());
        subgoal.setDescription(blankToNull(request.getDescription()));
        subgoal.setStatus(request.getStatus());
        subgoal.setTargetDate(request.getTargetDate());
        subgoal.setEstimatedHours(hours);

        List<Subgoal> subgoals = subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(goal.getId());
        weightedProgressService.applyWeights(goal.getWeightingMode(), subgoals);
        goal.touch();
        return subgoalRepository.save(subgoal);
    }

    /** Changes only the status. Weights stay the same, so progress changes by this subgoal's weight. */
    @Transactional
    public Subgoal updateStatus(Long userId, Long id, String statusText) {
        SubgoalStatus status = SubgoalStatus.parse(statusText);
        Subgoal subgoal = findSubgoal(userId, id);
        subgoal.setStatus(status);
        subgoal.getGoal().touch();
        return subgoalRepository.save(subgoal);
    }

    @Transactional
    public void deleteSubgoal(Long userId, Long id) {
        Subgoal subgoal = findSubgoal(userId, id);
        Goal goal = lockGoal(userId, subgoal.getGoal().getId());
        subgoalRepository.delete(subgoal);

        // The query below sees the delete (JPA flushes pending changes before running a query).
        List<Subgoal> remaining = subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(goal.getId());
        weightedProgressService.applyWeights(goal.getWeightingMode(), remaining);
        goal.touch();
    }

    /** Finds the user's goal and locks its row until the transaction ends. */
    private Goal lockGoal(Long userId, Long goalId) {
        return goalRepository.findByIdAndUserIdForUpdate(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal with id " + goalId + " was not found."));
    }

    /**
     * EFFORT goals need valid hours. EQUAL goals (made before Version 3) do not use hours.
     * Returns the value to store.
     */
    private BigDecimal checkHours(Goal goal, BigDecimal hours) {
        if (goal.getWeightingMode() == WeightingMode.EFFORT) {
            weightedProgressService.requireValidHours(hours);
            return hours;
        }
        if (hours != null) {
            throw new ValidationFailedException("estimatedHours",
                    "This goal was made before effort estimates existed, so hours are not used.");
        }
        return null;
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
