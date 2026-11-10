package com.goalstack.service;

import com.goalstack.dto.GoalView;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SubgoalRepository subgoalRepository;

    @InjectMocks
    private GoalService goalService;

    // ----- progress formula -----

    @Test
    void progressIsZeroWhenThereAreNoSubgoals() {
        assertThat(GoalService.calculateProgress(0, 0)).isZero();
    }

    @Test
    void progressIsCompletedOverTotal() {
        assertThat(GoalService.calculateProgress(2, 5)).isEqualTo(40);
    }

    @Test
    void progressIsRoundedDownSoOnly100MeansAllDone() {
        assertThat(GoalService.calculateProgress(1, 3)).isEqualTo(33);
        assertThat(GoalService.calculateProgress(199, 200)).isEqualTo(99);
    }

    @Test
    void progressIs100WhenEverythingIsCompleted() {
        assertThat(GoalService.calculateProgress(4, 4)).isEqualTo(100);
    }

    @Test
    void reopeningASubgoalLowersProgress() {
        int before = GoalService.calculateProgress(3, 4);
        int after = GoalService.calculateProgress(2, 4);
        assertThat(after).isLessThan(before);
    }

    // ----- view built from subgoal statuses -----

    @Test
    void inProgressSubgoalsDoNotCountAsCompleted() {
        GoalView view = viewFor(goal(null), List.of(
                sub(SubgoalStatus.COMPLETED), sub(SubgoalStatus.COMPLETED),
                sub(SubgoalStatus.IN_PROGRESS), sub(SubgoalStatus.PENDING), sub(SubgoalStatus.PENDING)));

        assertThat(view.getProgress()).isEqualTo(40);
        assertThat(view.getCompletedCount()).isEqualTo(2);
        assertThat(view.getTotalCount()).isEqualTo(5);
        assertThat(view.isComplete()).isFalse();
    }

    @Test
    void goalWithNoSubgoalsIsNotComplete() {
        GoalView view = viewFor(goal(null), List.of());

        assertThat(view.getProgress()).isZero();
        assertThat(view.isComplete()).isFalse();
    }

    @Test
    void goalIsCompleteWhenAllSubgoalsAreCompleted() {
        GoalView view = viewFor(goal(null), List.of(sub(SubgoalStatus.COMPLETED), sub(SubgoalStatus.COMPLETED)));

        assertThat(view.getProgress()).isEqualTo(100);
        assertThat(view.isComplete()).isTrue();
    }

    // ----- overdue rules -----

    @Test
    void incompleteGoalWithPastDeadlineIsOverdue() {
        GoalView view = viewFor(goal(LocalDate.now().minusDays(1)), List.of(sub(SubgoalStatus.PENDING)));
        assertThat(view.isOverdue()).isTrue();
    }

    @Test
    void completedGoalIsNeverOverdue() {
        GoalView view = viewFor(goal(LocalDate.now().minusDays(1)), List.of(sub(SubgoalStatus.COMPLETED)));
        assertThat(view.isOverdue()).isFalse();
    }

    @Test
    void goalWithoutDeadlineOrWithFutureDeadlineIsNotOverdue() {
        assertThat(viewFor(goal(null), List.of(sub(SubgoalStatus.PENDING))).isOverdue()).isFalse();
        assertThat(viewFor(goal(LocalDate.now().plusDays(3)), List.of(sub(SubgoalStatus.PENDING))).isOverdue()).isFalse();
    }

    @Test
    void goalDueTodayIsNotOverdue() {
        assertThat(GoalService.isOverdue(LocalDate.of(2026, 1, 10), false, LocalDate.of(2026, 1, 10))).isFalse();
    }

    // ----- missing data -----

    @Test
    void deletingAMissingGoalThrowsNotFound() {
        when(goalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.deleteGoal(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ----- helpers -----

    private GoalView viewFor(Goal goal, List<Subgoal> subgoals) {
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(subgoalRepository.findByGoalIdOrderByIdAsc(1L)).thenReturn(subgoals);
        return goalService.getGoalView(1L);
    }

    private Goal goal(LocalDate deadline) {
        Goal goal = new Goal();
        goal.setTitle("Learn Java");
        goal.setDeadline(deadline);
        return goal;
    }

    private Subgoal sub(SubgoalStatus status) {
        Subgoal subgoal = new Subgoal();
        subgoal.setTitle("Step");
        subgoal.setStatus(status);
        return subgoal;
    }
}
