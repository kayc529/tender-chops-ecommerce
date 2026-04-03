package com.kaycheung.payment_service.messaging.outbox;

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
            select outboxEvent from OutboxEvent outboxEvent
            where outboxEvent.publishedAt is null
            and outboxEvent.nextAttemptAt <= :now
            order by outboxEvent.occurredAt asc
            """)
    List<OutboxEvent> findUnpublishedOutboxEvents(@Param("now") Instant now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent outboxEvent
            set outboxEvent.publishedAt = :now,
                outboxEvent.lastError = null
            where outboxEvent.id = :id and outboxEvent.publishedAt is null
            """)
    int updateOutboxEventOnSuccess(
            @Param("id") UUID id,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent outboxEvent
            set outboxEvent.attemptCount = outboxEvent.attemptCount + 1,
                outboxEvent.lastError = :newLastError,
                outboxEvent.nextAttemptAt = :nextAttemptAt
            where outboxEvent.id = :id and outboxEvent.publishedAt is null
            """)
    int updateOutboxEventOnFailure(
            @Param("id") UUID id,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("newLastError") String newLastError);
}
