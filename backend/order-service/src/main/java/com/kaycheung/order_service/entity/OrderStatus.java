package com.kaycheung.order_service.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PROCESSING,
    PAID,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELED,
    EXPIRED
}
