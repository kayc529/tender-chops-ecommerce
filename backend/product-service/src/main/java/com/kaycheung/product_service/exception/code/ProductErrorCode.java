package com.kaycheung.product_service.exception.code;

public enum ProductErrorCode implements ApiErrorCode {
    PRODUCT_NOT_FOUND("Product not found"),
    PRODUCT_INVALID_CATEGORY("Invalid category"),
    PRODUCT_IMAGE_FORMAT_INVALID("Product image format invalid"),
    PRODUCT_IMAGE_UPLOAD_URL_FAILED("Failed to generate presigned upload URL for product image"),
    PRODUCT_IMAGE_UNAUTHORIZED_ACCESS("You don't have permission to access this resource");

    private final String message;

    ProductErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
