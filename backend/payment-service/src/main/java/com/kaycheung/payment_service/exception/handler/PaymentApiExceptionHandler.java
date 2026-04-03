package com.kaycheung.payment_service.exception.handler;

import com.kaycheung.payment_service.dto.ApiErrorResponse;
import com.kaycheung.payment_service.exception.PaymentApiException;
import com.kaycheung.payment_service.util.ApiErrorResponseUtil;
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
public class PaymentApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PaymentApiExceptionHandler.class);

    @ExceptionHandler(PaymentApiException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderException(PaymentApiException ex, HttpServletRequest request) {
        log.warn("PaymentApiException: status={}, code={}, path={}, msg={}",
                ex.getHttpStatus(), ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiErrorResponseUtil.buildError(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage(), request));
    }
}
