package com.kaycheung.inventory_service.messaging.inbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
    @Query("""
            select ie
            from InboxEvent ie
            where ie.processedAt is null
              and ie.dead = false
              and ie.nextAttemptAt <= :now
            order by ie.receivedAt asc
            """)
    List<InboxEvent> findDueUnprocessed(
            @Param("now") Instant now,
            Pageable pageable);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update InboxEvent ie
               set ie.processedAt = :processedAt,
                   ie.lastError = null
             where ie.id = :id
               and ie.processedAt is null
               and ie.dead = false
            """)
    int updateInboxEventOnSuccess(
            @Param("id") UUID id,
            @Param("processedAt") Instant processedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update InboxEvent ie
               set ie.attemptCount = ie.attemptCount + 1,
                   ie.lastError = :lastError,
                   ie.nextAttemptAt = :nextAttemptAt
             where ie.id = :id
               and ie.processedAt is null
               and ie.dead = false
            """)
    int updateInboxEventOnFailure(
            @Param("id") UUID id,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update InboxEvent ie
               set ie.dead = true
             where ie.id = :id
               and ie.processedAt is null
               and ie.dead = false
            """)
    int markInboxEventDead(@Param("id") UUID id);
}
