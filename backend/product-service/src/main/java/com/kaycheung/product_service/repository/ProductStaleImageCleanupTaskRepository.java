package com.kaycheung.product_service.repository;

import com.kaycheung.product_service.entity.ProductStaleImageCleanupTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductStaleImageCleanupTaskRepository extends JpaRepository<ProductStaleImageCleanupTask, UUID> {
    @Query("""
            select task from ProductStaleImageCleanupTask task
            where task.processedAt is null
            order by task.createdAt asc
            """)
    List<ProductStaleImageCleanupTask> findProcessedAtIsNull(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update ProductStaleImageCleanupTask task
           set task.processedAt = :now,
               task.lastError = null
           where task.id = :id
           """)
    int markTaskAsProcessed(
            @Param("id") UUID taskId,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update ProductStaleImageCleanupTask task
           set task.attemptCount = task.attemptCount + 1,
               task.lastError = :lastError
           where task.id = :id
           """)
    int increaseAttemptCount(
            @Param("id") UUID taskId,
            @Param("lastError") String errorMessage);
}
