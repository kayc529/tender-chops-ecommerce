package com.kaycheung.product_service.exception.domain;

import com.kaycheung.product_service.exception.code.ProductErrorCode;
import org.springframework.http.HttpStatus;

public class ProductImageUnauthorizedAccessException extends ProductException {
    public ProductImageUnauthorizedAccessException() {
        super(ProductErrorCode.PRODUCT_IMAGE_UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN, ProductErrorCode.PRODUCT_IMAGE_UNAUTHORIZED_ACCESS.getMessage());
    }
}
