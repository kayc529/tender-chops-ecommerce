package com.kaycheung.cart_service.util;

import com.kaycheung.cart_service.dto.ApiErrorResponse;
import com.kaycheung.cart_service.exception.code.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public class ApiErrorResponseUtil {
    //  for common and domain exceptions
    public static ApiErrorResponse buildError(HttpStatus status, ApiErrorCode errorCode, String debugMessage, HttpServletRequest request) {
        return new ApiErrorResponse(
                status.value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                debugMessage,
                request.getRequestURI(),
                Instant.now()
        );
    }

    //  for client exceptions
    public static ApiErrorResponse buildError(int status, String errorCode, String userMessage, String debugMessage, HttpServletRequest request) {
        return new ApiErrorResponse(
                status,
                errorCode,
                userMessage,
                debugMessage,
                request.getRequestURI(),
                Instant.now()
        );
    }
}
