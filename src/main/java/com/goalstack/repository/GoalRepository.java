package com.goalstack.repository;

import com.goalstack.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    // Spring Data builds the SQL from the method name: newest goals first.
    List<Goal> findAllByOrderByCreatedAtDesc();
}
