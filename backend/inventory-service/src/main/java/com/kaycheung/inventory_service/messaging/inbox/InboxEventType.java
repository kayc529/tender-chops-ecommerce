package com.kaycheung.inventory_service.messaging.inbox;

public enum InboxEventType {
    PRODUCT_CREATED,
    ORDER_CREATED,
    ORDER_CREATION_FAILED,
    ORDER_CANCELED,
    PAYMENT_CAPTURED;

    public static InboxEventType from(String value) {
        try {
            return InboxEventType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported InboxEventType: " + value);
        }
    }
}
