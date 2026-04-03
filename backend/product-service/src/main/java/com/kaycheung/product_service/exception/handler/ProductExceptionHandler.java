package com.kaycheung.product_service.exception.handler;

import com.kaycheung.product_service.dto.ApiErrorResponse;
import com.kaycheung.product_service.exception.domain.ProductException;
import com.kaycheung.product_service.utils.ApiErrorResponseUtil;
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
public class ProductExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ProductExceptionHandler.class);

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ApiErrorResponse> handleProductException(ProductException ex, HttpServletRequest request) {
        log.warn("ProductException: status={}, code={}, path={}, msg={}",
                ex.getHttpStatus(), ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(ex.getHttpStatus()).body(ApiErrorResponseUtil.buildError(
                ex.getHttpStatus(),
                ex.getErrorCode(),
                ex.getMessage(),
                request
        ));
    }
}
