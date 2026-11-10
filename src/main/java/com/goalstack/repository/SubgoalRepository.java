package com.goalstack.repository;

import com.goalstack.entity.Subgoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubgoalRepository extends JpaRepository<Subgoal, Long> {

    // "WHERE goal_id = ? ORDER BY id ASC", derived from the method name.
    List<Subgoal> findByGoalIdOrderByIdAsc(Long goalId);

    long countByGoal_Id(Long goalId);
}
