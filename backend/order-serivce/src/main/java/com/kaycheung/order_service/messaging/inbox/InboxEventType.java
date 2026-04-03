package com.kaycheung.order_service.messaging.inbox;

public enum InboxEventType {
    PAYMENT_ATTEMPT_AUTHORIZED,
    PAYMENT_ATTEMPT_CANCELED,
    PAYMENT_ATTEMPT_FAILED,
    PAYMENT_CAPTURED,
    PAYMENT_CANCELED,
}
