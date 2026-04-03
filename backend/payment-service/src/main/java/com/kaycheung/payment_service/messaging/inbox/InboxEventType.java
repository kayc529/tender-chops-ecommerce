package com.kaycheung.payment_service.messaging.inbox;

public enum InboxEventType {
    ORDER_CANCELED,
    ORDER_READY_TO_CAPTURE,
    ORDER_DO_NOT_CAPTURE,
}
