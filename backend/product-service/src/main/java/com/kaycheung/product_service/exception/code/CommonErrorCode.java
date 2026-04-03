package com.kaycheung.product_service.exception.code;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ApiErrorCode {
    COMMON_AUTH_INVALID_TOKEN( HttpStatus.UNAUTHORIZED, "Invalid or expired access token"),
    COMMON_AUTH_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication failed"),
    COMMON_AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "No permission to access this resource"),
    COMMON_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    COMMON_CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, "One or more request parameters are invalid"),
    COMMON_MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "Malformed or invalid request body"),
    COMMON_STATE_CONFLICT(HttpStatus.CONFLICT, "Request could not be completed due to data conflict"),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,"Request method not supported"),
    COMMON_MISSING_HEADER(HttpStatus.BAD_REQUEST, "Required header missing"),
    COMMON_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND,"Route not found"),
    COMMON_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable. Please try again."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"Something went wrong. Please try again.");

    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus status(){
        return httpStatus;
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