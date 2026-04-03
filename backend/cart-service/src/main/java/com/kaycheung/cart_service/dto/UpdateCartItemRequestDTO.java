package com.kaycheung.cart_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequestDTO(
        @NotNull
        @Positive
        int quantity
) {
}
