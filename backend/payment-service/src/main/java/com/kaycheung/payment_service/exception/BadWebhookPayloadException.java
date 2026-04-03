package com.kaycheung.payment_service.exception;

public class BadWebhookPayloadException extends RuntimeException {
    public BadWebhookPayloadException(String message) {
        super(message);
    }

    public BadWebhookPayloadException(String message, Throwable e) {
        super(message, e);
    }
}
