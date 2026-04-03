package com.kaycheung.order_service.exception.handler;

import com.kaycheung.order_service.dto.ApiErrorResponse;
import com.kaycheung.order_service.exception.client.InventoryClientException;
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
public class InventoryClientExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(InventoryClientExceptionHandler.class);

    @ExceptionHandler()
    public ResponseEntity<ApiErrorResponse> handleInventoryClientException(InventoryClientException ex, HttpServletRequest request){
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
