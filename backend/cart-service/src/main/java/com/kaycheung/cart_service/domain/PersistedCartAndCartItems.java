package com.kaycheung.cart_service.domain;

import com.kaycheung.cart_service.entity.Cart;
import com.kaycheung.cart_service.entity.CartItem;

import java.util.List;

public record PersistedCartAndCartItems(Cart cart, List<CartItem> cartItems) {
}
