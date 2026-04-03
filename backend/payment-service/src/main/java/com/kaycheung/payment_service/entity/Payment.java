package com.kaycheung.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name="payment")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name ="payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String provider;

    @Column(name ="current_attempt_id")
    private UUID currentAttemptId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
