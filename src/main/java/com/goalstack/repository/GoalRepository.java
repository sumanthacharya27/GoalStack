package com.goalstack.repository;

import com.goalstack.entity.Goal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    // Every goal query includes the user id, so one user can never read another user's goals.
    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    /**
     * Same lookup, but locks the goal row until the transaction ends. Used before changing subgoals,
     * so two requests cannot recalculate the same goal's weights at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goal g where g.id = :id and g.user.id = :userId")
    Optional<Goal> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
