package com.kaycheung.order_service.messaging.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name="outbox_events")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class OutboxEvent {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
}
