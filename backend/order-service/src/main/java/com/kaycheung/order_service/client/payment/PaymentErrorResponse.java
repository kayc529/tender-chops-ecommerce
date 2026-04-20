package com.kaycheung.order_service.client.payment;

public record PaymentErrorResponse(
        int status,
        String errorCode,
        String userMessage,
        String debugMessage
) {
}
