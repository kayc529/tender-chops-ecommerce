package com.kaycheung.inventory_service.exception.domain;

import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import org.springframework.http.HttpStatus;

public class InventoryTotalQuantityNegativeException extends InventoryException{
    public InventoryTotalQuantityNegativeException(String message) {
        super(HttpStatus.BAD_REQUEST, InventoryErrorCode.INVENTORY_TOTAL_QUANTITY_NEGATIVE, message);
    }
}
