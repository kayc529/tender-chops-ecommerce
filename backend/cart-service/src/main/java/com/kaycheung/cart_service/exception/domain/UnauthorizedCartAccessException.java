package com.kaycheung.cart_service.exception.domain;

import com.kaycheung.cart_service.exception.code.CartErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedCartAccessException extends CartException {
    public UnauthorizedCartAccessException() {
        super(HttpStatus.FORBIDDEN, CartErrorCode.CART_UNAUTHORIZED_ACCESS, "Unauthorized access");
    }
}
