package com.kaycheung.inventory_service.entity;

import com.kaycheung.inventory_service.messaging.contract.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class ProcessedEvent {
    @Id
    private UUID eventId;
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    private Instant processedAt;
}
