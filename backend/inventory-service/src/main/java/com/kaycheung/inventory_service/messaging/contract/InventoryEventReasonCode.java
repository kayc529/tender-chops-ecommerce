package com.kaycheung.inventory_service.messaging.contract;

public enum InventoryEventReasonCode {
    RESERVATION_EXPIRED,
    INVARIANT_VIOLATION,
    RESERVATION_NOT_FOUND,
    RESERVATION_UNKNOWN_STATUS,
    INVENTORY_NOT_FOUND,
    MIXED_STATUS,
    RESERVATIONS_RELEASED
}
