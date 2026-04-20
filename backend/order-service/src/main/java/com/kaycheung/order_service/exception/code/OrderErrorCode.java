package com.kaycheung.order_service.exception.code;

public enum OrderErrorCode implements ApiErrorCode{
    ORDER_NOT_FOUND("This order does not exist or is no longer available"),
    ORDER_INVALID_STATUS("This order cannot be updated in its current state"),
    ORDER_INVALID_STATUS_VALUE("Invalid order status provided"),
    ORDER_UNAUTHORIZED_ACCESS("You don't have permission to access this resource"),
    ORDER_PERSISTENCE_FAILED("Failed to create order. Please try again");

    private final String message;

    OrderErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
