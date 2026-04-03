package com.kaycheung.order_service.client.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @Positive long amount,
        @NotNull String currency
) {
}
