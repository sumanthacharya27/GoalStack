package com.goalstack.repository;

import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against an in-memory H2 database (no MySQL needed) to check the mapping, cascade delete and ownership queries. */
@DataJpaTest
class GoalSubgoalRepositoryTest {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private SubgoalRepository subgoalRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void subgoalsAreSavedWithTimestampsAndDefaultStatus() {
        Goal goal = saveGoal("Learn Java", null);
        Subgoal subgoal = saveSubgoal(goal, "Data Types", SubgoalStatus.PENDING);

        assertThat(subgoal.getStatus()).isEqualTo(SubgoalStatus.PENDING);
        assertThat(subgoal.getCreatedAt()).isNotNull();
        assertThat(goal.getCreatedAt()).isNotNull();
        assertThat(subgoalRepository.findByGoalIdOrderByIdAsc(goal.getId())).hasSize(1);
    }

    @Test
    void deletingAGoalRemovesItsSubgoals() {
        Goal goal = saveGoal("Learn Java", null);
        saveSubgoal(goal, "Data Types", SubgoalStatus.PENDING);
        saveSubgoal(goal, "Collections", SubgoalStatus.PENDING);
        assertThat(subgoalRepository.count()).isEqualTo(2);

        goalRepository.delete(goal);
        goalRepository.flush();

        assertThat(subgoalRepository.count()).isZero();
    }

    @Test
    void goalsAreScopedToTheirOwner() {
        User asha = saveUser("asha@example.com");
        User ravi = saveUser("ravi@example.com");
        Goal ashasGoal = saveGoal("Asha goal", asha);
        Goal ravisGoal = saveGoal("Ravi goal", ravi);

        List<Goal> ashasGoals = goalRepository.findByUserIdOrderByCreatedAtDesc(asha.getId());

        assertThat(ashasGoals).extracting(Goal::getTitle).containsExactly("Asha goal");
        assertThat(goalRepository.findByIdAndUserId(ashasGoal.getId(), asha.getId())).isPresent();
        assertThat(goalRepository.findByIdAndUserId(ravisGoal.getId(), asha.getId())).isEmpty();
    }

    @Test
    void subgoalLookupIsScopedToTheOwner() {
        User asha = saveUser("asha@example.com");
        User ravi = saveUser("ravi@example.com");
        Subgoal ashasSubgoal = saveSubgoal(saveGoal("Asha goal", asha), "Step", SubgoalStatus.PENDING);

        assertThat(subgoalRepository.findOwnedById(ashasSubgoal.getId(), asha.getId())).isPresent();
        assertThat(subgoalRepository.findOwnedById(ashasSubgoal.getId(), ravi.getId())).isEmpty();
    }

    @Test
    void groupedCountsOnlyIncludeTheUsersOwnGoals() {
        User asha = saveUser("asha@example.com");
        User ravi = saveUser("ravi@example.com");
        Goal ashasGoal = saveGoal("Asha goal", asha);
        saveSubgoal(ashasGoal, "One", SubgoalStatus.COMPLETED);
        saveSubgoal(ashasGoal, "Two", SubgoalStatus.PENDING);
        saveSubgoal(saveGoal("Ravi goal", ravi), "Other", SubgoalStatus.COMPLETED);

        List<Object[]> rows = subgoalRepository.countByGoalAndStatusForUser(asha.getId());

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> assertThat(row[0]).isEqualTo(ashasGoal.getId()));
        long total = rows.stream().mapToLong(row -> ((Number) row[2]).longValue()).sum();
        assertThat(total).isEqualTo(2);
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Test user");
        user.setEmail(email);
        user.setPasswordHash("hash");
        return userRepository.saveAndFlush(user);
    }

    private Goal saveGoal(String title, User owner) {
        Goal goal = new Goal();
        goal.setTitle(title);
        goal.setUser(owner);
        return goalRepository.saveAndFlush(goal);
    }

    private Subgoal saveSubgoal(Goal goal, String title, SubgoalStatus status) {
        Subgoal subgoal = new Subgoal();
        subgoal.setGoal(goal);
        subgoal.setTitle(title);
        subgoal.setStatus(status);
        return subgoalRepository.saveAndFlush(subgoal);
    }
}
