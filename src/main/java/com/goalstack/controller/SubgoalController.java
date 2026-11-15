package com.goalstack.controller;

import com.goalstack.dto.GoalView;
import com.goalstack.dto.StatusRequest;
import com.goalstack.dto.StatusResponse;
import com.goalstack.dto.SubgoalRequest;
import com.goalstack.dto.SubgoalResponse;
import com.goalstack.entity.Subgoal;
import com.goalstack.security.CurrentUser;
import com.goalstack.service.GoalService;
import com.goalstack.service.SubgoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/** REST endpoints for subgoals. Every call is limited to the logged-in user's own data. */
@RestController
public class SubgoalController {

    private final SubgoalService subgoalService;
    private final GoalService goalService;

    public SubgoalController(SubgoalService subgoalService, GoalService goalService) {
        this.subgoalService = subgoalService;
        this.goalService = goalService;
    }

    @PostMapping("/api/goals/{goalId}/subgoals")
    public ResponseEntity<SubgoalResponse> createSubgoal(@PathVariable Long goalId,
                                                         @Valid @RequestBody SubgoalRequest request,
                                                         Principal principal) {
        Subgoal created = subgoalService.createSubgoal(CurrentUser.id(principal), goalId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SubgoalResponse.from(created));
    }

    @GetMapping("/api/subgoals/{id}")
    public SubgoalResponse getSubgoal(@PathVariable Long id, Principal principal) {
        return SubgoalResponse.from(subgoalService.findSubgoal(CurrentUser.id(principal), id));
    }

    @PutMapping("/api/subgoals/{id}")
    public SubgoalResponse updateSubgoal(@PathVariable Long id, @Valid @RequestBody SubgoalRequest request,
                                         Principal principal) {
        return SubgoalResponse.from(subgoalService.updateSubgoal(CurrentUser.id(principal), id, request));
    }

    @PatchMapping("/api/subgoals/{id}/status")
    public StatusResponse updateStatus(@PathVariable Long id, @RequestBody StatusRequest request,
                                       Principal principal) {
        Long userId = CurrentUser.id(principal);
        Subgoal updated = subgoalService.updateStatus(userId, id, request.getStatus());
        GoalView goal = goalService.getGoalView(userId, updated.getGoal().getId());
        return new StatusResponse(SubgoalResponse.from(updated), goal.getProgress(), goal.isComplete());
    }

    @DeleteMapping("/api/subgoals/{id}")
    public ResponseEntity<Void> deleteSubgoal(@PathVariable Long id, Principal principal) {
        subgoalService.deleteSubgoal(CurrentUser.id(principal), id);
        return ResponseEntity.noContent().build();
    }
}
