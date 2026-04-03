package com.kaycheung.cart_service.exception.domain;

import com.kaycheung.cart_service.exception.code.CartErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CartNotFoundException extends CartException {
    public CartNotFoundException() {
        super(HttpStatus.NOT_FOUND, CartErrorCode.CART_NOT_FOUND, "Cart not found");
    }

    public CartNotFoundException(UUID cartId) {
        super(HttpStatus.NOT_FOUND, CartErrorCode.CART_NOT_FOUND, "Cart with id " + cartId + " not found");
    }
}
