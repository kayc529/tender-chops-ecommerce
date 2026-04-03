package com.kaycheung.payment_service.dto;

import com.kaycheung.payment_service.entity.PaymentAttemptStatus;
import com.kaycheung.payment_service.entity.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record GetPaymentResponse(
        UUID paymentId,
        UUID paymentAttemptId,
        UUID orderId,
        long amount,
        String currency,
        PaymentStatus paymentStatus,
        PaymentAttemptStatus currentAttemptStatus,
        boolean canRetry,
        PaymentRetryNotAllowedReason retryNotAllowedReason,
        Instant updatedAt
) {
}
