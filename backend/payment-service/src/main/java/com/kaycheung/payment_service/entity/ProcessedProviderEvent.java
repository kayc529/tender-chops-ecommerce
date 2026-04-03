package com.kaycheung.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_provider_events")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class ProcessedProviderEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String provider;
    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;
    private Instant receivedAt;
    private String payloadHash;
}
