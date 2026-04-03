package com.kaycheung.cart_service.exception.handler;

import com.kaycheung.cart_service.dto.ApiErrorResponse;
import com.kaycheung.cart_service.exception.domain.CartException;
import com.kaycheung.cart_service.util.ApiErrorResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CartExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CartExceptionHandler.class);

    @ExceptionHandler(CartException.class)
    public ResponseEntity<ApiErrorResponse> handleCartException(CartException ex, HttpServletRequest request) {
        log.warn("CartException: status={}, code={}, path={}, msg={}",
                ex.getHttpStatus(), ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiErrorResponseUtil.buildError(
                ex.getHttpStatus(),
                ex.getErrorCode(),
                ex.getMessage(),
                request
        ));
    }
}
