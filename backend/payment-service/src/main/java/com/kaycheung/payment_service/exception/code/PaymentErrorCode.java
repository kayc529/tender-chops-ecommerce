package com.kaycheung.payment_service.exception.code;

public enum PaymentErrorCode implements ApiErrorCode {
    PAYMENT_UNAUTHORIZED("No access to this resource"),
    PAYMENT_NOT_PAYABLE("This order can’t be paid right now."),
    PAYMENT_AMOUNT_OR_CURRENCY_MISMATCH("Payment details don’t match the order. Please refresh and try again."),
    PAYMENT_ATTEMPT_NOT_ALLOWED("Payment is already in progress or completed. Please check the payment status."),
    PAYMENT_INVALID_CURRENCY("Unsupported currency."),
    PAYMENT_INVALID_AMOUNT("Invalid payment amount."),
    PAYMENT_NOT_FOUND("Payment not found."),
    PAYMENT_ATTEMPT_NOT_FOUND("Payment attempt not found."),
    PAYMENT_INCONSISTENT_STATE("Something went wrong on our side. Please try again later."),
    STRIPE_API_ERROR("Payment provider is temporarily unavailable. Please try again."),
    STRIPE_SESSION_INVALID("Payment session is no longer valid. Please try again.");

    private final String message;

    PaymentErrorCode(String message)
    {
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
