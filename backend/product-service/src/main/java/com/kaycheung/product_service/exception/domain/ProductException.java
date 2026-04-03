package com.kaycheung.product_service.exception.domain;

import com.kaycheung.product_service.exception.code.ProductErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ProductException extends RuntimeException {
    private final ProductErrorCode errorCode;
    private final HttpStatus httpStatus;

    public ProductException(ProductErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
