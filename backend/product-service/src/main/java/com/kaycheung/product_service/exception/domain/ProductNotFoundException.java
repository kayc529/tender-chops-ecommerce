package com.kaycheung.product_service.exception.domain;

import com.kaycheung.product_service.exception.code.ProductErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ProductNotFoundException extends ProductException {
    public ProductNotFoundException(UUID productId) {
        super(ProductErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product with id " + productId + " not found");
    }
}
