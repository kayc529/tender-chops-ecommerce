package com.kaycheung.order_service.client.product;

public record ProductErrorResponse(
        int status,
        String errorCode,
        String userMessage,
        String debugMessage
) {
}
