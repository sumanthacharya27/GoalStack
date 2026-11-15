package com.goalstack.repository;

import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
import com.goalstack.entity.User;
import com.goalstack.entity.WeightingMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against an in-memory H2 database (no MySQL needed) to check the mapping, cascade delete,
 * ownership queries and the new weight columns. Flyway is switched off here: its SQL files are written for
 * MySQL, so Hibernate creates the tables for these tests instead.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
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
        Subgoal subgoal = saveSubgoal(goal, "Data Types", SubgoalStatus.PENDING, 1, "5.00", "100.00");

        assertThat(subgoal.getStatus()).isEqualTo(SubgoalStatus.PENDING);
        assertThat(subgoal.getCreatedAt()).isNotNull();
        assertThat(goal.getCreatedAt()).isNotNull();
        assertThat(subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(goal.getId())).hasSize(1);
    }

    @Test
    void hoursWeightAndOrderAreStored() {
        Goal goal = saveGoal("Learn Java", null);
        Subgoal saved = saveSubgoal(goal, "OOP", SubgoalStatus.PENDING, 3, "12.50", "33.33");

        Subgoal loaded = subgoalRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getEstimatedHours()).isEqualByComparingTo("12.50");
        assertThat(loaded.getWeight()).isEqualByComparingTo("33.33");
        assertThat(loaded.getSortOrder()).isEqualTo(3);
    }

    @Test
    void olderGoalsKeepNoHours() {
        Goal older = new Goal();
        older.setTitle("Old goal");
        older.setWeightingMode(WeightingMode.EQUAL);
        goalRepository.saveAndFlush(older);
        Subgoal saved = saveSubgoal(older, "Old step", SubgoalStatus.PENDING, 1, null, "100.00");

        assertThat(goalRepository.findById(older.getId()).orElseThrow().getWeightingMode())
                .isEqualTo(WeightingMode.EQUAL);
        assertThat(subgoalRepository.findById(saved.getId()).orElseThrow().getEstimatedHours()).isNull();
    }

    @Test
    void newGoalsDefaultToEffortWeighting() {
        Goal goal = saveGoal("New goal", null);

        assertThat(goalRepository.findById(goal.getId()).orElseThrow().getWeightingMode())
                .isEqualTo(WeightingMode.EFFORT);
    }

    @Test
    void subgoalsComeBackInSortOrderNotInsertOrder() {
        Goal goal = saveGoal("Learn Java", null);
        saveSubgoal(goal, "Second", SubgoalStatus.PENDING, 2, "5.00", "50.00");
        saveSubgoal(goal, "First", SubgoalStatus.PENDING, 1, "5.00", "50.00");

        List<Subgoal> loaded = subgoalRepository.findByGoalIdOrderBySortOrderAscIdAsc(goal.getId());

        assertThat(loaded).extracting(Subgoal::getTitle).containsExactly("First", "Second");
    }

    @Test
    void deletingAGoalRemovesItsSubgoals() {
        Goal goal = saveGoal("Learn Java", null);
        saveSubgoal(goal, "Data Types", SubgoalStatus.PENDING, 1, "5.00", "50.00");
        saveSubgoal(goal, "Collections", SubgoalStatus.PENDING, 2, "5.00", "50.00");
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
    void lockedLookupAlsoRespectsTheOwner() {
        User asha = saveUser("asha@example.com");
        User ravi = saveUser("ravi@example.com");
        Goal ashasGoal = saveGoal("Asha goal", asha);

        assertThat(goalRepository.findByIdAndUserIdForUpdate(ashasGoal.getId(), asha.getId())).isPresent();
        assertThat(goalRepository.findByIdAndUserIdForUpdate(ashasGoal.getId(), ravi.getId())).isEmpty();
    }

    @Test
    void subgoalLookupIsScopedToTheOwner() {
        User asha = saveUser("asha@example.com");
        User ravi = saveUser("ravi@example.com");
        Subgoal ashasSubgoal = saveSubgoal(saveGoal("Asha goal", asha), "Step", SubgoalStatus.PENDING, 1, "5.00", "100.00");

        assertThat(subgoalRepository.findOwnedById(ashasSubgoal.getId(), asha.getId())).isPresent();
        assertThat(subgoalRepository.findOwnedById(ashasSubgoal.getId(), ravi.getId())).isEmpty();
    }

    @Test
    void groupedRowsHaveCountsWeightSumsAndHourSumsOnlyForTheUsersGoals() {
        User asha = saveUser("asha@example.com");
        User ravi = saveUser("ravi@example.com");
        Goal ashasGoal = saveGoal("Asha goal", asha);
        saveSubgoal(ashasGoal, "One", SubgoalStatus.COMPLETED, 1, "10.00", "25.00");
        saveSubgoal(ashasGoal, "Two", SubgoalStatus.PENDING, 2, "30.00", "75.00");
        saveSubgoal(saveGoal("Ravi goal", ravi), "Other", SubgoalStatus.COMPLETED, 1, "5.00", "100.00");

        List<Object[]> rows = subgoalRepository.countByGoalAndStatusForUser(asha.getId());

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> assertThat(row[0]).isEqualTo(ashasGoal.getId()));
        Object[] completed = rows.stream().filter(r -> r[1] == SubgoalStatus.COMPLETED).findFirst().orElseThrow();
        assertThat(((Number) completed[2]).intValue()).isEqualTo(1);
        assertThat(new BigDecimal(completed[3].toString())).isEqualByComparingTo("25.00");
        assertThat(new BigDecimal(completed[4].toString())).isEqualByComparingTo("10.00");
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

    private Subgoal saveSubgoal(Goal goal, String title, SubgoalStatus status, int order, String hours, String weight) {
        Subgoal subgoal = new Subgoal();
        goal.addSubgoal(subgoal);
        subgoal.setTitle(title);
        subgoal.setStatus(status);
        subgoal.setSortOrder(order);
        subgoal.setEstimatedHours(hours == null ? null : new BigDecimal(hours));
        subgoal.setWeight(new BigDecimal(weight));
        return subgoalRepository.saveAndFlush(subgoal);
    }
}
