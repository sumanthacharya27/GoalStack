package com.goalstack.repository;

import com.goalstack.entity.Subgoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubgoalRepository extends JpaRepository<Subgoal, Long> {

    // "WHERE goal_id = ? ORDER BY sort_order, id", derived from the method name.
    // Only call this after the goal's owner has been checked.
    List<Subgoal> findByGoalIdOrderBySortOrderAscIdAsc(Long goalId);

    /** A subgoal, but only if its goal belongs to the given user. */
    @Query("select s from Subgoal s where s.id = :id and s.goal.user.id = :userId")
    Optional<Subgoal> findOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * One row per (goal, status) for this user's goals: {goalId, status, count, sum of weights, sum of hours}.
     * Builds the dashboard in one query.
     */
    @Query("select s.goal.id, s.status, count(s), sum(s.weight), sum(s.estimatedHours) from Subgoal s "
            + "where s.goal.user.id = :userId group by s.goal.id, s.status")
    List<Object[]> countByGoalAndStatusForUser(@Param("userId") Long userId);
}
