package com.kaycheung.order_service.dto.internal;

import jakarta.validation.constraints.NotNull;

public record InternalUpdateOrderStatusDTO(@NotNull String newOrderStatus) {
}
