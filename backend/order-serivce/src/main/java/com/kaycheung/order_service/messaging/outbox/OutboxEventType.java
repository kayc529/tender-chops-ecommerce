package com.kaycheung.order_service.messaging.outbox;

public enum OutboxEventType {
    ORDER_CANCELED,
    ORDER_READY_TO_CAPTURE,
    ORDER_DO_NOT_CAPTURE,
}
