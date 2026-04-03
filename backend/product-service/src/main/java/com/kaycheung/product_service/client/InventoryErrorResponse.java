package com.kaycheung.product_service.client;

public record InventoryErrorResponse(
        int status,
        String errorCode,
        String userMessage,
        String debugMessage) {
}
