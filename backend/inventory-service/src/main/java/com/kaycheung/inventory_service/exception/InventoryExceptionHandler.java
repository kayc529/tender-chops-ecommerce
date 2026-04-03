package com.kaycheung.inventory_service.exception;

import com.kaycheung.inventory_service.dto.ApiErrorResponse;
import com.kaycheung.inventory_service.exception.domain.InventoryException;
import com.kaycheung.inventory_service.utils.ApiErrorResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InventoryExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(InventoryExceptionHandler.class);

    public ResponseEntity<ApiErrorResponse> handleInventoryException(InventoryException ex, HttpServletRequest request) {
        log.warn("InventoryException: status={}, code={}, path={}, msg={}",
                ex.getHttpStatus(), ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiErrorResponseUtil.buildError(
                ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage(), request
        ));
    }

}
