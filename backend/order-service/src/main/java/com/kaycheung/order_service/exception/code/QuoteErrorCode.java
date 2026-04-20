package com.kaycheung.order_service.exception.code;

public enum QuoteErrorCode implements ApiErrorCode {
    QUOTE_NOT_FOUND("This quote does not exist or is no longer available"),
    QUOTE_UNAUTHORIZED_ACCESS("You don't have permission to access this resource"),
    QUOTE_INVALID("This quote is no longer valid"),
    QUOTE_CONFLICT("This quote has been updated or changed. Please refresh and try again."),
    QUOTE_EXPIRED("This quote has expired. Please request a new one.");

    private final String message;

    QuoteErrorCode(String message) {
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
