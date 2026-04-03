package com.kaycheung.payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @Positive long amount,
        @NotNull String currency
) {
}
