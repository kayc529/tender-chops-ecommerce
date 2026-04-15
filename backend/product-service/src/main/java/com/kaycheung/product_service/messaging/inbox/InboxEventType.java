package com.kaycheung.product_service.messaging.inbox;

public enum InboxEventType {
    INVENTORY_STOCK_UPDATED,
    UNKNOWN;

    public static InboxEventType from(String value) {
        try {
            return InboxEventType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
