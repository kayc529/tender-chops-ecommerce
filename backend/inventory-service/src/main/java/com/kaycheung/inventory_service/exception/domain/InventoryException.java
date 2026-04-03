package com.kaycheung.inventory_service.exception.domain;

import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class InventoryException extends RuntimeException {
    private final InventoryErrorCode errorCode;
    private final HttpStatus httpStatus;

    public InventoryException(HttpStatus httpStatus, InventoryErrorCode errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
