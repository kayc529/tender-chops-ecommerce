package com.kaycheung.inventory_service.entity;

import com.kaycheung.inventory_service.messaging.contract.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    private UUID aggregateId;
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payloadJsonString;
    private Instant occurredAt;

    private Instant publishedAt;
    private int attemptCount;
    private String lastError;
}
