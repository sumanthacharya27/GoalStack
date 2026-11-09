package com.goalstack.service;

import com.goalstack.dto.SubgoalRequest;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subgoal business logic. Every subgoal is always reached through its own record, so it can
 * never be changed "through" an unrelated parent goal. New subgoals always start as PENDING.
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

    public Subgoal findSubgoal(Long id) {
        return subgoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subgoal with id " + id + " was not found."));
    }

    public SubgoalRequest getSubgoalRequest(Long id) {
        return SubgoalRequest.from(findSubgoal(id));
    }

    @Transactional
    public Subgoal createSubgoal(Long goalId, SubgoalRequest form) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal with id " + goalId + " was not found."));
        Subgoal subgoal = new Subgoal();
        subgoal.setGoal(goal);
        subgoal.setTitle(form.getTitle().trim());
        subgoal.setDescription(blankToNull(form.getDescription()));
        subgoal.setStatus(SubgoalStatus.PENDING);
        return subgoalRepository.save(subgoal);
    }

    @Transactional
    public Subgoal updateSubgoal(Long id, SubgoalRequest form) {
        Subgoal subgoal = findSubgoal(id);
        subgoal.setTitle(form.getTitle().trim());
        subgoal.setDescription(blankToNull(form.getDescription()));
        subgoal.setStatus(form.getStatus());
        return subgoalRepository.save(subgoal);
    }

    /** @throws IllegalArgumentException if the status text is not PENDING, IN_PROGRESS or COMPLETED */
    @Transactional
    public Subgoal updateStatus(Long id, String statusText) {
        SubgoalStatus status = SubgoalStatus.parse(statusText);
        Subgoal subgoal = findSubgoal(id);
        subgoal.setStatus(status);
        return subgoalRepository.save(subgoal);
    }

    @Transactional
    public void deleteSubgoal(Long id) {
        subgoalRepository.delete(findSubgoal(id));
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
