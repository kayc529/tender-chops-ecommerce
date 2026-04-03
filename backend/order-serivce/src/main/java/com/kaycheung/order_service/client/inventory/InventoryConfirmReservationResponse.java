package com.kaycheung.order_service.client.inventory;

public record InventoryConfirmReservationResponse(
        boolean reservationFulfilled,
        String reason) {
}
