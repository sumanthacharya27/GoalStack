package com.goalstack.service;

import com.goalstack.dto.SubgoalRequest;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.exception.InvalidStatusException;
import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.repository.GoalRepository;
import com.goalstack.repository.SubgoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SubgoalRepository subgoalRepository;

    @InjectMocks
    private SubgoalService subgoalService;

    @Test
    void newSubgoalsDefaultToPending() {
        when(goalRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(new Goal()));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        SubgoalRequest request = new SubgoalRequest();
        request.setTitle("  Data Types  ");
        request.setStatus(SubgoalStatus.COMPLETED); // must be ignored on creation

        Subgoal saved = subgoalService.createSubgoal(USER_ID, 1L, request);

        assertThat(saved.getStatus()).isEqualTo(SubgoalStatus.PENDING);
        assertThat(saved.getTitle()).isEqualTo("Data Types");
    }

    @Test
    void creatingASubgoalForAMissingOrForeignGoalFails() {
        when(goalRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());
        SubgoalRequest request = new SubgoalRequest();
        request.setTitle("Orphan");

        assertThatThrownBy(() -> subgoalService.createSubgoal(USER_ID, 5L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void unsupportedStatusIsRejected() {
        assertThatThrownBy(() -> subgoalService.updateStatus(USER_ID, 1L, "DONE"))
                .isInstanceOf(InvalidStatusException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void statusCanBeChangedBackFromCompleted() {
        Subgoal existing = new Subgoal();
        existing.setGoal(new Goal());
        existing.setStatus(SubgoalStatus.COMPLETED);
        when(subgoalRepository.findOwnedById(2L, USER_ID)).thenReturn(Optional.of(existing));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        Subgoal updated = subgoalService.updateStatus(USER_ID, 2L, "IN_PROGRESS");

        assertThat(updated.getStatus()).isEqualTo(SubgoalStatus.IN_PROGRESS);
    }

    @Test
    void aSubgoalOfAnotherUserCannotBeChanged() {
        when(subgoalRepository.findOwnedById(4L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subgoalService.updateStatus(USER_ID, 4L, "COMPLETED"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void deletingAMissingSubgoalFails() {
        when(subgoalRepository.findOwnedById(9L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subgoalService.deleteSubgoal(USER_ID, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subgoalRepository, never()).delete(any(Subgoal.class));
    }

    @Test
    void changingASubgoalUpdatesTheParentGoalTimestamp() {
        Goal goal = new Goal();
        Subgoal existing = new Subgoal();
        existing.setGoal(goal);
        when(subgoalRepository.findOwnedById(3L, USER_ID)).thenReturn(Optional.of(existing));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(goal.getUpdatedAt()).isNull();

        subgoalService.updateStatus(USER_ID, 3L, "COMPLETED");

        assertThat(goal.getUpdatedAt()).isNotNull();
    }
}
