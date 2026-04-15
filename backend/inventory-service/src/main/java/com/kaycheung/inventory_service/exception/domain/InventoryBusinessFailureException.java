package com.kaycheung.inventory_service.exception.domain;

import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import org.springframework.http.HttpStatus;

public class InventoryBusinessFailureException extends InventoryException{
    public InventoryBusinessFailureException(String detail) {
        super(HttpStatus.CONFLICT, InventoryErrorCode.INVENTORY_RESERVATION_INVALID_STATE, detail);
    }
}
