package com.kaycheung.cart_service.client.product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ProductBasePriceRequestDTO(@NotEmpty List<@NotNull UUID> productIds) {
}
