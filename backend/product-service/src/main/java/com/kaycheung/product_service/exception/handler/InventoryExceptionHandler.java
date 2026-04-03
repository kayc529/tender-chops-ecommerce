package com.kaycheung.product_service.exception.handler;

import com.kaycheung.product_service.dto.ApiErrorResponse;
import com.kaycheung.product_service.exception.client.InventoryClientException;
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
public class InventoryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryExceptionHandler.class);

    @ExceptionHandler(InventoryClientException.class)
    public ResponseEntity<ApiErrorResponse> handleInventoryClient(InventoryClientException ex, HttpServletRequest request) {
        log.error(
                "Inventory client failure: path={}, message={}",
                request.getRequestURI(),
                ex.getMessage(),
                ex
        );
        return ResponseEntity.status(ex.getStatus()).body(ApiErrorResponseUtil.buildError(
                ex.getStatus(),
                ex.getErrorCode(),
                ex.getUserMessage(),
                ex.getDebugMessage(),
                request
        ));
    }
}
