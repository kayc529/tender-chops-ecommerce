package com.kaycheung.inventory_service.dto.internal;

public record ReservationConfirmResponseDTO(boolean reservationFulfilled, String reason) {
    public enum ReservationConfirmReason {
        RESERVATION_NOT_FOUND,
        RESERVATION_NOT_RESERVED,
        RESERVATION_EXPIRED,
        INVENTORY_NOT_FOUND,
        RESERVATION_NOT_BACKED_BY_INVENTORY
    }
}

