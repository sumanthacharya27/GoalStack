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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubgoalServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long GOAL_ID = 10L;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SubgoalRepository subgoalRepository;

    private final WeightedProgressService progressService = new WeightedProgressService();

    private SubgoalService subgoalService;

    @BeforeEach
    void setUp() {
        subgoalService = new SubgoalService(goalRepository, subgoalRepository, progressService);
    }

    // ----- creating -----

    @Test
    void newSubgoalsDefaultToPendingAndTheOnlySubgoalGets100Percent() {
        Goal goal = lockedGoal(WeightingMode.EFFORT, List.of());
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));
        SubgoalRequest request = request("  Data Types  ", "5");
        request.setStatus(SubgoalStatus.COMPLETED); // must be ignored on creation

        Subgoal saved = subgoalService.createSubgoal(USER_ID, GOAL_ID, request);

        assertThat(saved.getStatus()).isEqualTo(SubgoalStatus.PENDING);
        assertThat(saved.getTitle()).isEqualTo("Data Types");
        assertThat(saved.getSortOrder()).isEqualTo(1);
        assertThat(saved.getWeight()).isEqualByComparingTo("100.00");
        assertThat(goal.getUpdatedAt()).isNotNull();
    }

    @Test
    void addingASubgoalRecalculatesTheWeightsOfTheExistingOnes() {
        Subgoal first = existing("10", SubgoalStatus.PENDING);
        Subgoal second = existing("30", SubgoalStatus.COMPLETED);
        lockedGoal(WeightingMode.EFFORT, List.of(first, second));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        Subgoal added = subgoalService.createSubgoal(USER_ID, GOAL_ID, request("Third", "20"));

        assertThat(first.getWeight()).isEqualByComparingTo("16.67");
        assertThat(second.getWeight()).isEqualByComparingTo("50.00");
        assertThat(added.getWeight()).isEqualByComparingTo("33.33");
        assertThat(added.getSortOrder()).isEqualTo(3);
    }

    @Test
    void effortGoalsRequireHours() {
        lockedGoal(WeightingMode.EFFORT, List.of());

        assertThatThrownBy(() -> subgoalService.createSubgoal(USER_ID, GOAL_ID, request("No hours", null)))
                .isInstanceOfSatisfying(ValidationFailedException.class,
                        ex -> assertThat(ex.getFieldErrors()).containsKey("estimatedHours"));
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void hoursOutsideTheAllowedRangeAreRejected() {
        lockedGoal(WeightingMode.EFFORT, List.of());

        assertThatThrownBy(() -> subgoalService.createSubgoal(USER_ID, GOAL_ID, request("Tiny", "0.10")))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> subgoalService.createSubgoal(USER_ID, GOAL_ID, request("Huge", "2000")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void olderGoalsKeepEqualWeightsAndRejectHours() {
        Subgoal first = existing(null, SubgoalStatus.PENDING);
        Subgoal second = existing(null, SubgoalStatus.PENDING);
        lockedGoal(WeightingMode.EQUAL, List.of(first, second));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        Subgoal added = subgoalService.createSubgoal(USER_ID, GOAL_ID, request("Third", null));

        assertThat(added.getEstimatedHours()).isNull();
        assertThat(first.getWeight()).isEqualByComparingTo("33.34");
        assertThat(second.getWeight()).isEqualByComparingTo("33.33");
        assertThat(added.getWeight()).isEqualByComparingTo("33.33");

        assertThatThrownBy(() -> subgoalService.createSubgoal(USER_ID, GOAL_ID, request("With hours", "4")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void aGoalCannotHaveMoreThan50Subgoals() {
        List<Subgoal> fifty = new ArrayList<>();
        for (int i = 0; i < WeightedProgressService.MAX_SUBGOALS_PER_GOAL; i++) {
            fifty.add(existing("2", SubgoalStatus.PENDING));
        }
        lockedGoal(WeightingMode.EFFORT, fifty);

        assertThatThrownBy(() -> subgoalService.createSubgoal(USER_ID, GOAL_ID, request("One too many", "2")))
                .isInstanceOf(ValidationFailedException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void creatingASubgoalForAMissingOrForeignGoalFails() {
        when(goalRepository.findByIdAndUserIdForUpdate(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subgoalService.createSubgoal(USER_ID, 5L, request("Orphan", "3")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subgoalRepository, never()).save(any());
    }

    // ----- updating hours -----

    @Test
    void changingHoursRecalculatesWeights() {
        Goal goal = goalWithId(WeightingMode.EFFORT);
        Subgoal a = existing("10", SubgoalStatus.PENDING);
        Subgoal b = existing("10", SubgoalStatus.PENDING);
        a.setGoal(goal);
        when(subgoalRepository.findOwnedById(1L, USER_ID)).thenReturn(Optional.of(a));
        when(goalRepository.findByIdAndUserIdForUpdate(GOAL_ID, USER_ID)).thenReturn(Optional.of(goal));
        when(subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(GOAL_ID)).thenReturn(List.of(a, b));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        SubgoalRequest request = request("A", "30");
        request.setStatus(SubgoalStatus.IN_PROGRESS);
        subgoalService.updateSubgoal(USER_ID, 1L, request);

        assertThat(a.getEstimatedHours()).isEqualByComparingTo("30");
        assertThat(a.getWeight()).isEqualByComparingTo("75.00");
        assertThat(b.getWeight()).isEqualByComparingTo("25.00");
        assertThat(a.getStatus()).isEqualTo(SubgoalStatus.IN_PROGRESS);
    }

    // ----- status -----

    @Test
    void unsupportedStatusIsRejected() {
        assertThatThrownBy(() -> subgoalService.updateStatus(USER_ID, 1L, "DONE"))
                .isInstanceOf(InvalidStatusException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void statusCanBeChangedBackFromCompletedWithoutChangingTheWeight() {
        Subgoal existing = existing("10", SubgoalStatus.COMPLETED);
        existing.setGoal(new Goal());
        existing.setWeight(new BigDecimal("40.00"));
        when(subgoalRepository.findOwnedById(2L, USER_ID)).thenReturn(Optional.of(existing));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        Subgoal updated = subgoalService.updateStatus(USER_ID, 2L, "IN_PROGRESS");

        assertThat(updated.getStatus()).isEqualTo(SubgoalStatus.IN_PROGRESS);
        assertThat(updated.getWeight()).isEqualByComparingTo("40.00");
    }

    @Test
    void aSubgoalOfAnotherUserCannotBeChanged() {
        when(subgoalRepository.findOwnedById(4L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subgoalService.updateStatus(USER_ID, 4L, "COMPLETED"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void changingAStatusUpdatesTheParentGoalTimestamp() {
        Goal goal = new Goal();
        Subgoal existing = existing("10", SubgoalStatus.PENDING);
        existing.setGoal(goal);
        when(subgoalRepository.findOwnedById(3L, USER_ID)).thenReturn(Optional.of(existing));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(goal.getUpdatedAt()).isNull();

        subgoalService.updateStatus(USER_ID, 3L, "COMPLETED");

        assertThat(goal.getUpdatedAt()).isNotNull();
    }

    // ----- deleting -----

    @Test
    void deletingASubgoalRebalancesTheRemainingOnes() {
        Goal goal = goalWithId(WeightingMode.EFFORT);
        Subgoal removed = existing("10", SubgoalStatus.PENDING);
        removed.setGoal(goal);
        Subgoal b = existing("10", SubgoalStatus.PENDING);
        Subgoal c = existing("30", SubgoalStatus.PENDING);
        when(subgoalRepository.findOwnedById(1L, USER_ID)).thenReturn(Optional.of(removed));
        when(goalRepository.findByIdAndUserIdForUpdate(GOAL_ID, USER_ID)).thenReturn(Optional.of(goal));
        when(subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(GOAL_ID)).thenReturn(List.of(b, c));

        subgoalService.deleteSubgoal(USER_ID, 1L);

        verify(subgoalRepository).delete(removed);
        assertThat(b.getWeight()).isEqualByComparingTo("25.00");
        assertThat(c.getWeight()).isEqualByComparingTo("75.00");
        assertThat(b.getSortOrder()).isEqualTo(1);
        assertThat(c.getSortOrder()).isEqualTo(2);
    }

    @Test
    void deletingAMissingSubgoalFails() {
        when(subgoalRepository.findOwnedById(9L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subgoalService.deleteSubgoal(USER_ID, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subgoalRepository, never()).delete(any(Subgoal.class));
    }

    // ----- helpers -----

    private Goal goalWithId(WeightingMode mode) {
        Goal goal = new Goal();
        goal.setWeightingMode(mode);
        ReflectionTestUtils.setField(goal, "id", GOAL_ID);
        return goal;
    }

    /** Makes the repository return a locked goal and its current subgoals (the service copies the list). */
    private Goal lockedGoal(WeightingMode mode, List<Subgoal> subgoals) {
        Goal goal = goalWithId(mode);
        when(goalRepository.findByIdAndUserIdForUpdate(GOAL_ID, USER_ID)).thenReturn(Optional.of(goal));
        when(subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(GOAL_ID)).thenReturn(subgoals);
        return goal;
    }

    private Subgoal existing(String hours, SubgoalStatus status) {
        Subgoal subgoal = new Subgoal();
        subgoal.setTitle("Existing");
        subgoal.setEstimatedHours(hours == null ? null : new BigDecimal(hours));
        subgoal.setStatus(status);
        return subgoal;
    }

    private SubgoalRequest request(String title, String hours) {
        SubgoalRequest request = new SubgoalRequest();
        request.setTitle(title);
        request.setEstimatedHours(hours == null ? null : new BigDecimal(hours));
        return request;
    }
}
