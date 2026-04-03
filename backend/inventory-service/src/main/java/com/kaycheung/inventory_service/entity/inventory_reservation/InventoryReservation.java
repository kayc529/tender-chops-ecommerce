package com.kaycheung.inventory_service.entity.inventory_reservation;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="inventory_reservation")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class InventoryReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID productId;
    private UUID quoteId;
    private UUID orderId;

    private int quantity;
    @Enumerated(EnumType.STRING)
    private InventoryReservationStatus reservationStatus;

    @CreatedDate
    private Instant createdAt;
    private Instant expiresAt;
    private Instant releasedAt;
    private Instant committedAt;
}
