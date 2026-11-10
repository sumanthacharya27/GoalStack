package com.goalstack.repository;

import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against an in-memory H2 database (no MySQL needed) to check the mapping and cascade delete. */
@DataJpaTest
class GoalSubgoalRepositoryTest {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private SubgoalRepository subgoalRepository;

    @Test
    void subgoalsAreSavedWithTimestampsAndDefaultStatus() {
        Goal goal = saveGoal("Learn Java");
        Subgoal subgoal = saveSubgoal(goal, "Data Types");

        assertThat(subgoal.getStatus()).isEqualTo(SubgoalStatus.PENDING);
        assertThat(subgoal.getCreatedAt()).isNotNull();
        assertThat(goal.getCreatedAt()).isNotNull();
        assertThat(subgoalRepository.findByGoalIdOrderByIdAsc(goal.getId())).hasSize(1);
    }

    @Test
    void deletingAGoalRemovesItsSubgoals() {
        Goal goal = saveGoal("Learn Java");
        saveSubgoal(goal, "Data Types");
        saveSubgoal(goal, "Collections");
        assertThat(subgoalRepository.count()).isEqualTo(2);

        goalRepository.delete(goal);
        goalRepository.flush();

        assertThat(subgoalRepository.count()).isZero();
    }

    private Goal saveGoal(String title) {
        Goal goal = new Goal();
        goal.setTitle(title);
        return goalRepository.saveAndFlush(goal);
    }

    private Subgoal saveSubgoal(Goal goal, String title) {
        Subgoal subgoal = new Subgoal();
        subgoal.setGoal(goal);
        subgoal.setTitle(title);
        return subgoalRepository.saveAndFlush(subgoal);
    }
}
