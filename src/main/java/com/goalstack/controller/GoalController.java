package com.goalstack.controller;

import com.goalstack.dto.GoalRequest;
import com.goalstack.dto.GoalResponse;
import com.goalstack.entity.Goal;
import com.goalstack.security.CurrentUser;
import com.goalstack.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * REST endpoints for goals. Returns JSON for the React app; business rules live in GoalService.
 * Every method gets the logged-in user from the JWT and passes the user id to the service.
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalResponse> listGoals(Principal principal) {
        return goalService.getAllGoalViews(CurrentUser.id(principal)).stream()
                .map(view -> GoalResponse.from(view, false))
                .toList();
    }

    @GetMapping("/{id}")
    public GoalResponse getGoal(@PathVariable Long id, Principal principal) {
        return GoalResponse.from(goalService.getGoalView(CurrentUser.id(principal), id), true);
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request, Principal principal) {
        Long userId = CurrentUser.id(principal);
        Goal goal = goalService.createGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GoalResponse.from(goalService.getGoalView(userId, goal.getId()), true));
    }

    @PutMapping("/{id}")
    public GoalResponse updateGoal(@PathVariable Long id, @Valid @RequestBody GoalRequest request,
                                   Principal principal) {
        Long userId = CurrentUser.id(principal);
        goalService.updateGoal(userId, id, request);
        return GoalResponse.from(goalService.getGoalView(userId, id), true);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id, Principal principal) {
        goalService.deleteGoal(CurrentUser.id(principal), id);
        return ResponseEntity.noContent().build();
    }
}
