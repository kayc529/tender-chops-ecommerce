package com.kaycheung.cart_service.exception.code;

public enum CartErrorCode implements ApiErrorCode {
    CART_NOT_FOUND("Cart not found"),
    CART_ITEM_NOT_FOUND("Cart item not found"),
    CART_UNAUTHORIZED_ACCESS("No access to this resource");

    private final String message;

    CartErrorCode(String message) {
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
