package com.goalstack.service;

import com.goalstack.dto.SubgoalRequest;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubgoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SubgoalRepository subgoalRepository;

    @InjectMocks
    private SubgoalService subgoalService;

    @Test
    void newSubgoalsDefaultToPending() {
        when(goalRepository.findById(1L)).thenReturn(Optional.of(new Goal()));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        SubgoalRequest form = new SubgoalRequest();
        form.setTitle("  Data Types  ");
        form.setStatus(SubgoalStatus.COMPLETED); // must be ignored on creation

        Subgoal saved = subgoalService.createSubgoal(1L, form);

        assertThat(saved.getStatus()).isEqualTo(SubgoalStatus.PENDING);
        assertThat(saved.getTitle()).isEqualTo("Data Types");
    }

    @Test
    void creatingASubgoalForAMissingGoalFails() {
        when(goalRepository.findById(5L)).thenReturn(Optional.empty());
        SubgoalRequest form = new SubgoalRequest();
        form.setTitle("Orphan");

        assertThatThrownBy(() -> subgoalService.createSubgoal(5L, form))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void unsupportedStatusIsRejected() {
        assertThatThrownBy(() -> subgoalService.updateStatus(1L, "DONE"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(subgoalRepository, never()).save(any());
    }

    @Test
    void statusCanBeChangedBackFromCompleted() {
        Subgoal existing = new Subgoal();
        existing.setStatus(SubgoalStatus.COMPLETED);
        when(subgoalRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(subgoalRepository.save(any(Subgoal.class))).thenAnswer(inv -> inv.getArgument(0));

        Subgoal updated = subgoalService.updateStatus(2L, "IN_PROGRESS");

        assertThat(updated.getStatus()).isEqualTo(SubgoalStatus.IN_PROGRESS);
    }

    @Test
    void deletingAMissingSubgoalFails() {
        when(subgoalRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subgoalService.deleteSubgoal(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
