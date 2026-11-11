package com.goalstack.service;

import com.goalstack.dto.GoalView;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import com.goalstack.repository.UserRepository;
import com.goalstack.dto.GoalRequest;
import com.goalstack.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SubgoalRepository subgoalRepository;

    @Mock
    private UserRepository userRepository;

    private static final Long USER_ID = 7L;

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
        when(goalRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.deleteGoal(USER_ID, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ----- ownership -----

    @Test
    void aGoalOfAnotherUserLooksLikeItDoesNotExist() {
        when(goalRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoalView(USER_ID, 1L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(subgoalRepository);
    }

    @Test
    void newGoalsBelongToTheLoggedInUser() {
        User owner = new User();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));
        GoalRequest request = new GoalRequest();
        request.setTitle("  Learn Java  ");

        Goal saved = goalService.createGoal(USER_ID, request);

        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getTitle()).isEqualTo("Learn Java");
    }

    // ----- dashboard (grouped counts) -----

    @Test
    void dashboardProgressComesFromTheGroupedCounts() {
        Goal withSubgoals = goal(null);
        ReflectionTestUtils.setField(withSubgoals, "id", 1L);
        Goal empty = goal(null);
        ReflectionTestUtils.setField(empty, "id", 2L);
        List<Object[]> rows = List.of(
                new Object[]{1L, SubgoalStatus.COMPLETED, 2L},
                new Object[]{1L, SubgoalStatus.PENDING, 2L});
        when(subgoalRepository.countByGoalAndStatusForUser(USER_ID)).thenReturn(rows);
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(withSubgoals, empty));

        List<GoalView> views = goalService.getAllGoalViews(USER_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).getTotalCount()).isEqualTo(4);
        assertThat(views.get(0).getCompletedCount()).isEqualTo(2);
        assertThat(views.get(0).getProgress()).isEqualTo(50);
        assertThat(views.get(1).getTotalCount()).isZero();
        assertThat(views.get(1).getProgress()).isZero();
        assertThat(views.get(1).isComplete()).isFalse();
    }

    // ----- helpers -----

    private GoalView viewFor(Goal goal, List<Subgoal> subgoals) {
        when(goalRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(goal));
        when(subgoalRepository.findByGoalIdOrderByIdAsc(1L)).thenReturn(subgoals);
        return goalService.getGoalView(USER_ID, 1L);
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
