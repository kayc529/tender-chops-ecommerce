package com.kaycheung.payment_service.repository;

import com.kaycheung.payment_service.entity.CheckoutSessionExpireTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutSessionExpireTaskRepository extends JpaRepository<CheckoutSessionExpireTask, UUID> {
    Optional<CheckoutSessionExpireTask> findByCheckoutSessionId(String checkoutSessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CheckoutSessionExpireTask task
            set task.completedAt = :now
            where task.id = :id and task.completedAt is null
            """)
    int updateTaskCompletedAt(
            @Param("id") UUID id,
            @Param("now") Instant now);


    @Query("""
            select task from CheckoutSessionExpireTask task
            where task.completedAt is null
            and task.nextAttemptAt <= :now
            order by task.createdAt asc
            """)
    List<CheckoutSessionExpireTask> findDueTasks(@Param("now") Instant now, Pageable pageable);
}
