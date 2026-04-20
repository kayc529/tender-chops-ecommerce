package com.kaycheung.order_service.client.cart;

import java.util.List;
import java.util.UUID;

public record CartResponseDTO(UUID cartId, Long cartVersion, List<CartItemResponseDTO> cartItems) {
}
