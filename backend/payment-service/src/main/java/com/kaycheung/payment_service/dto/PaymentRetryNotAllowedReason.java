package com.kaycheung.payment_service.dto;

public enum PaymentRetryNotAllowedReason{
    ALREADY_PAID,
    PAYMENT_PROCESSING,
    PAYMENT_CANCELED,
    ORDER_NOT_PAYABLE,
    UNKNOWN
}
