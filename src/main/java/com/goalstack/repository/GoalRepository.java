package com.goalstack.repository;

import com.goalstack.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    // Every goal query includes the user id, so one user can never read another user's goals.
    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);
}
