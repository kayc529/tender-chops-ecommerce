package com.kaycheung.order_service.client.cart;

public record CartErrorResponse(
        int status,
        String errorCode,
        String userMessage,
        String debugMessage) {
}
