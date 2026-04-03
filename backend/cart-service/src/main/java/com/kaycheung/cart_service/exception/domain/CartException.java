package com.kaycheung.cart_service.exception.domain;

import com.kaycheung.cart_service.exception.code.CartErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class CartException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final CartErrorCode errorCode;

    public CartException(HttpStatus httpStatus, CartErrorCode errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
