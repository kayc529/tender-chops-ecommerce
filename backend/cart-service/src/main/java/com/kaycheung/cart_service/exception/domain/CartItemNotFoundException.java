package com.kaycheung.cart_service.exception.domain;

import com.kaycheung.cart_service.exception.code.CartErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CartItemNotFoundException extends CartException {
    public CartItemNotFoundException(UUID cartItemId) {
        super(HttpStatus.NOT_FOUND, CartErrorCode.CART_ITEM_NOT_FOUND, "Cart item with id " + cartItemId + " not found");
    }
}
