package com.kaycheung.cart_service.client.product;

public record ProductErrorResponse(
        int status,
        String errorCode,
        String userMessage,
        String debugMessage
) {
}
