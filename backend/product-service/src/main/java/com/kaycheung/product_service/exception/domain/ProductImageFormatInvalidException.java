package com.kaycheung.product_service.exception.domain;

import com.kaycheung.product_service.exception.code.ProductErrorCode;
import org.springframework.http.HttpStatus;

public class ProductImageFormatInvalidException extends ProductException{
    public ProductImageFormatInvalidException(String message) {
        super(ProductErrorCode.PRODUCT_IMAGE_FORMAT_INVALID, HttpStatus.BAD_REQUEST, message);
    }
}
