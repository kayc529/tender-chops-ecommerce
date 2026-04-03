package com.kaycheung.cart_service.client.inventory;

public record InventoryErrorResponse(
        int status,
        String errorCode,
        String userMessage,
        String debugMessage) {
}