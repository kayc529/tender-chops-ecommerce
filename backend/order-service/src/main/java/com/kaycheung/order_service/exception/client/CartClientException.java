package com.kaycheung.order_service.exception.client;

import lombok.Getter;

@Getter
public class CartClientException extends RuntimeException {
    private final int status;
    private final String errorCode;
    private final String userMessage;
    private final String debugMessage;

    public CartClientException(int status, String errorCode, String userMessage, String debugMessage) {
        super(debugMessage);
        this.status = status;
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.debugMessage = debugMessage;
    }

    public CartClientException(int status, String errorCode, String userMessage, String debugMessage, Throwable cause) {
        super(debugMessage, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.debugMessage = debugMessage;
    }
}
