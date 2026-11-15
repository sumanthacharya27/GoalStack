package com.goalstack.service;

import com.goalstack.dto.GoalRequest;
import com.goalstack.dto.GoalView;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.entity.User;
import com.goalstack.entity.WeightingMode;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import com.goalstack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SubgoalRepository subgoalRepository;

    @Mock
    private UserRepository userRepository;

    private final WeightedProgressService progressService = new WeightedProgressService();

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(goalRepository, subgoalRepository, userRepository, progressService);
    }

    // ----- completion -----

    @Test
    void goalWithNoSubgoalsIsNotCompleteAndHasZeroProgress() {
        GoalView view = viewFor(goal(null, WeightingMode.EFFORT), List.of());

        assertThat(view.getProgress()).isEqualByComparingTo("0.00");
        assertThat(view.isComplete()).isFalse();
    }

    @Test
    void goalIsCompleteWhenAllSubgoalsAreCompleted() {
        GoalView view = viewFor(goal(null, WeightingMode.EFFORT),
                subs(WeightingMode.EFFORT, "10", SubgoalStatus.COMPLETED, "30", SubgoalStatus.COMPLETED));

        assertThat(view.getProgress()).isEqualByComparingTo("100.00");
        assertThat(view.isComplete()).isTrue();
    }

    // ----- weighted progress in the view -----

    @Test
    void effortGoalProgressFollowsTheHoursNotTheNumberOfSubgoals() {
        GoalView view = viewFor(goal(null, WeightingMode.EFFORT), subs(WeightingMode.EFFORT,
                "10", SubgoalStatus.COMPLETED,
                "25", SubgoalStatus.COMPLETED,
                "15", SubgoalStatus.IN_PROGRESS,
                "35", SubgoalStatus.PENDING,
                "15", SubgoalStatus.PENDING));

        assertThat(view.getProgress()).isEqualByComparingTo("35.00");
        assertThat(view.getCompletedCount()).isEqualTo(2);
        assertThat(view.getTotalCount()).isEqualTo(5);
        assertThat(view.getTotalEstimatedHours()).isEqualByComparingTo("100.00");
        assertThat(view.isComplete()).isFalse();
    }

    @Test
    void olderGoalsShareTheWeightEquallyAndHaveNoTotalHours() {
        GoalView view = viewFor(goal(null, WeightingMode.EQUAL), subs(WeightingMode.EQUAL,
                null, SubgoalStatus.COMPLETED,
                null, SubgoalStatus.COMPLETED,
                null, SubgoalStatus.IN_PROGRESS,
                null, SubgoalStatus.PENDING,
                null, SubgoalStatus.PENDING));

        assertThat(view.getProgress()).isEqualByComparingTo("40.00");
        assertThat(view.getTotalEstimatedHours()).isNull();
    }

    // ----- overdue rules -----

    @Test
    void incompleteGoalWithPastDeadlineIsOverdue() {
        GoalView view = viewFor(goal(LocalDate.now().minusDays(1), WeightingMode.EFFORT),
                subs(WeightingMode.EFFORT, "5", SubgoalStatus.PENDING));
        assertThat(view.isOverdue()).isTrue();
    }

    @Test
    void completedGoalIsNeverOverdue() {
        GoalView view = viewFor(goal(LocalDate.now().minusDays(1), WeightingMode.EFFORT),
                subs(WeightingMode.EFFORT, "5", SubgoalStatus.COMPLETED));
        assertThat(view.isOverdue()).isFalse();
    }

    @Test
    void goalWithoutDeadlineOrWithFutureDeadlineIsNotOverdue() {
        assertThat(viewFor(goal(null, WeightingMode.EFFORT),
                subs(WeightingMode.EFFORT, "5", SubgoalStatus.PENDING)).isOverdue()).isFalse();
        assertThat(viewFor(goal(LocalDate.now().plusDays(3), WeightingMode.EFFORT),
                subs(WeightingMode.EFFORT, "5", SubgoalStatus.PENDING)).isOverdue()).isFalse();
    }

    @Test
    void goalDueTodayIsNotOverdue() {
        assertThat(GoalService.isOverdue(LocalDate.of(2026, 1, 10), false, LocalDate.of(2026, 1, 10))).isFalse();
    }

    // ----- missing data and ownership -----

    @Test
    void deletingAMissingGoalThrowsNotFound() {
        when(goalRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.deleteGoal(USER_ID, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aGoalOfAnotherUserLooksLikeItDoesNotExist() {
        when(goalRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoalView(USER_ID, 1L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(subgoalRepository);
    }

    @Test
    void newGoalsBelongToTheLoggedInUserAndUseEffortWeights() {
        User owner = new User();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));
        GoalRequest request = new GoalRequest();
        request.setTitle("  Learn Java  ");

        Goal saved = goalService.createGoal(USER_ID, request);

        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getTitle()).isEqualTo("Learn Java");
        assertThat(saved.getWeightingMode()).isEqualTo(WeightingMode.EFFORT);
    }

    // ----- dashboard (grouped rows: goalId, status, count, sum of weights, sum of hours) -----

    @Test
    void dashboardProgressAndHoursComeFromTheGroupedRows() {
        Goal effortGoal = goalWithId(1L, WeightingMode.EFFORT);
        Goal olderGoal = goalWithId(2L, WeightingMode.EQUAL);
        Goal emptyGoal = goalWithId(3L, WeightingMode.EFFORT);
        List<Object[]> rows = List.of(
                new Object[]{1L, SubgoalStatus.COMPLETED, 2L, new BigDecimal("40.00"), new BigDecimal("20.00")},
                new Object[]{1L, SubgoalStatus.PENDING, 3L, new BigDecimal("60.00"), new BigDecimal("30.00")},
                new Object[]{2L, SubgoalStatus.COMPLETED, 1L, new BigDecimal("33.34"), null},
                new Object[]{2L, SubgoalStatus.PENDING, 2L, new BigDecimal("66.66"), null});
        when(subgoalRepository.countByGoalAndStatusForUser(USER_ID)).thenReturn(rows);
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(effortGoal, olderGoal, emptyGoal));

        List<GoalView> views = goalService.getAllGoalViews(USER_ID);

        assertThat(views).hasSize(3);
        assertThat(views.get(0).getProgress()).isEqualByComparingTo("40.00");
        assertThat(views.get(0).getCompletedCount()).isEqualTo(2);
        assertThat(views.get(0).getTotalCount()).isEqualTo(5);
        assertThat(views.get(0).getTotalEstimatedHours()).isEqualByComparingTo("50.00");

        assertThat(views.get(1).getProgress()).isEqualByComparingTo("33.34");
        assertThat(views.get(1).getTotalEstimatedHours()).isNull();

        assertThat(views.get(2).getTotalCount()).isZero();
        assertThat(views.get(2).getProgress()).isEqualByComparingTo("0.00");
        assertThat(views.get(2).isComplete()).isFalse();
    }

    // ----- helpers -----

    private GoalView viewFor(Goal goal, List<Subgoal> subgoals) {
        when(goalRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(goal));
        when(subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(1L)).thenReturn(subgoals);
        return goalService.getGoalView(USER_ID, 1L);
    }

    private Goal goal(LocalDate deadline, WeightingMode mode) {
        Goal goal = new Goal();
        goal.setTitle("Learn Java");
        goal.setDeadline(deadline);
        goal.setWeightingMode(mode);
        return goal;
    }

    private Goal goalWithId(Long id, WeightingMode mode) {
        Goal goal = goal(null, mode);
        ReflectionTestUtils.setField(goal, "id", id);
        return goal;
    }

    /** Arguments alternate: hours (or null), status... Weights are set like the real service does. */
    private List<Subgoal> subs(WeightingMode mode, Object... hoursAndStatus) {
        List<Subgoal> list = new ArrayList<>();
        for (int i = 0; i < hoursAndStatus.length; i += 2) {
            Subgoal subgoal = new Subgoal();
            subgoal.setTitle("Step");
            String hours = (String) hoursAndStatus[i];
            subgoal.setEstimatedHours(hours == null ? null : new BigDecimal(hours));
            subgoal.setStatus((SubgoalStatus) hoursAndStatus[i + 1]);
            list.add(subgoal);
        }
        progressService.applyWeights(mode, list);
        return list;
    }
}
