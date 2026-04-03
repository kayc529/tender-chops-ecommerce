package com.kaycheung.inventory_service.exception.domain;


import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import org.springframework.http.HttpStatus;

public class InventoryInvariantViolationException extends InventoryException{
    public InventoryInvariantViolationException(String detail){
        super(HttpStatus.INTERNAL_SERVER_ERROR, InventoryErrorCode.INVENTORY_INVARIANT_VIOLATION, "Inventory state invariant violated: " + detail);
    }
}
