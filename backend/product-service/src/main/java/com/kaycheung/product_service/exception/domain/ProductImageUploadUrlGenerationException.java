package com.kaycheung.product_service.exception.domain;

import com.kaycheung.product_service.exception.code.ProductErrorCode;
import org.springframework.http.HttpStatus;

public class ProductImageUploadUrlGenerationException extends ProductException{
    public ProductImageUploadUrlGenerationException() {
        super(ProductErrorCode.PRODUCT_IMAGE_UPLOAD_URL_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, ProductErrorCode.PRODUCT_IMAGE_UPLOAD_URL_FAILED.getMessage());
    }
}
