package com.kaycheung.payment_service.messaging.outbox;

public enum OutboxEventType {
    PAYMENT_ATTEMPT_AUTHORIZED,
    PAYMENT_ATTEMPT_CANCELED,
    PAYMENT_ATTEMPT_FAILED,
    PAYMENT_CAPTURED,
    PAYMENT_CANCELED,
}
