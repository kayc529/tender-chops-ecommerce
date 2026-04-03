package com.kaycheung.cart_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddCartItemRequestDTO(
        @NotNull
        UUID productId,
        @NotNull
        @Positive
        int quantity
) {
}
