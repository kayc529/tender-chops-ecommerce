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
@Table(name = "payment_attempts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="payment_id", nullable = false)
    private UUID paymentId;

    @Column(name="attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "payment_attempt_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentAttemptStatus paymentAttemptStatus;

    @Column(name="idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name="checkout_session_id")
    private String checkoutSessionId;

    @Column(name="payment_intent_id")
    private String paymentIntentId;

    @Column(name="redirect_url")
    private String redirectUrl;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
