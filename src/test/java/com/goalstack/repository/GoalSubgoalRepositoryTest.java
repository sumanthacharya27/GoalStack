package com.goalstack.repository;

import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        Goal goal = new Goal();
        goal.setTitle("Learn Spring Boot");

        goalRepository.saveAndFlush(goal);

        Subgoal subgoal1 = new Subgoal();
        subgoal1.setTitle("Learn Spring Core");

        Subgoal subgoal2 = new Subgoal();
        subgoal2.setTitle("Learn Spring MVC");

        // Maintain both sides of the relationship
        goal.addSubgoal(subgoal1);
        goal.addSubgoal(subgoal2);

        // Save the goal and its subgoals through cascading
        goalRepository.saveAndFlush(goal);

        assertEquals(2, subgoalRepository.countByGoal_Id(goal.getId()));

        // Delete the parent goal
        goalRepository.deleteById(goal.getId());
        goalRepository.flush();

        assertEquals(0, subgoalRepository.countByGoal_Id(goal.getId()));
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
