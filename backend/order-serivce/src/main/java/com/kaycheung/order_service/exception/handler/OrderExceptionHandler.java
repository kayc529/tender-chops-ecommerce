package com.kaycheung.order_service.exception.handler;

import com.kaycheung.order_service.dto.ApiErrorResponse;
import com.kaycheung.order_service.exception.domain.order.OrderException;
import com.kaycheung.order_service.util.ApiErrorResponseUtil;
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
public class OrderExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderExceptionHandler.class);

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderException(OrderException ex, HttpServletRequest request) {
        log.warn("OrderException: status={}, code={}, path={}, msg={}",
                ex.getHttpStatus(), ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiErrorResponseUtil.buildError(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage(), request));
    }
}
