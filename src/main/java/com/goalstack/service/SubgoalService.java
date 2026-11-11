package com.goalstack.service;

import com.goalstack.dto.SubgoalRequest;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.exception.InvalidStatusException;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subgoal business logic. A subgoal is only reachable if its goal belongs to the logged-in user,
 * so a user can never read or change someone else's subgoal. New subgoals always start as PENDING.
 */
@Service
@Transactional(readOnly = true)
public class SubgoalService {

    private final GoalRepository goalRepository;
    private final SubgoalRepository subgoalRepository;

    public SubgoalService(GoalRepository goalRepository, SubgoalRepository subgoalRepository) {
        this.goalRepository = goalRepository;
        this.subgoalRepository = subgoalRepository;
    }

    public Subgoal findSubgoal(Long userId, Long id) {
        return subgoalRepository.findOwnedById(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subgoal with id " + id + " was not found."));
    }

    @Transactional
    public Subgoal createSubgoal(Long userId, Long goalId, SubgoalRequest request) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal with id " + goalId + " was not found."));
        Subgoal subgoal = new Subgoal();
        subgoal.setGoal(goal);
        subgoal.setTitle(request.getTitle().trim());
        subgoal.setDescription(blankToNull(request.getDescription()));
        subgoal.setStatus(SubgoalStatus.PENDING);
        goal.touch();
        return subgoalRepository.save(subgoal);
    }

    @Transactional
    public Subgoal updateSubgoal(Long userId, Long id, SubgoalRequest request) {
        Subgoal subgoal = findSubgoal(userId, id);
        subgoal.setTitle(request.getTitle().trim());
        subgoal.setDescription(blankToNull(request.getDescription()));
        subgoal.setStatus(request.getStatus());
        subgoal.getGoal().touch();
        return subgoalRepository.save(subgoal);
    }

    /** @throws InvalidStatusException if the status text is not PENDING, IN_PROGRESS or COMPLETED */
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
        subgoal.getGoal().touch();
        subgoalRepository.delete(subgoal);
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
    public static SubgoalRequest from(Subgoal subgoal) {
        SubgoalRequest request = new SubgoalRequest();

        request.setTitle(subgoal.getTitle());
        request.setDescription(subgoal.getDescription());
        request.setStatus(subgoal.getStatus());

        return request;
    }
}
