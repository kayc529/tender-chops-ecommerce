package com.kaycheung.order_service.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("""
            select oe from OutboxEvent oe
            where oe.publishedAt is null
            and oe.nextAttemptAt <= :now
            order by oe.occurredAt asc
            """)
    List<OutboxEvent> findUnpublishedOutboxEvents(
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent oe
            set oe.publishedAt = :now,
                oe.lastError = null
            where oe.id = :id and oe.publishedAt is null
            """)
    int updateOutboxEventOnSuccess(
            @Param("id") UUID id,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent oe
            set oe.attemptCount = oe.attemptCount + 1,
                oe.lastError = :newLastError,
                oe.nextAttemptAt = :nextAttemptAt
            where oe.id = :id and oe.publishedAt is null
            """)
    int updateOutboxEventOnFailure(
            @Param("id") UUID id,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("newLastError") String newLastError);
}
