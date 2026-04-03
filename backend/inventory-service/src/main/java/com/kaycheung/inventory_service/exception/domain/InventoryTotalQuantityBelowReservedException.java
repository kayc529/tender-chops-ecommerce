package com.kaycheung.inventory_service.exception.domain;

import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import org.springframework.http.HttpStatus;

public class InventoryTotalQuantityBelowReservedException extends InventoryException {
    public InventoryTotalQuantityBelowReservedException(int totalQuantity, int reservedQuantity) {
        super(HttpStatus.CONFLICT, InventoryErrorCode.INVENTORY_QUANTITY_CONFLICT, String.format("Total quantity (%d) cannot be less than reserved quantity (%d)", totalQuantity, reservedQuantity));
    }
}
