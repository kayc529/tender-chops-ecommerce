package com.kaycheung.inventory_service.exception.domain;

import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class InventoryInsufficientStockException extends InventoryException {
    public InventoryInsufficientStockException(UUID productId, int availableQuantity, int additionalNeeded) {
        super(HttpStatus.CONFLICT, InventoryErrorCode.INVENTORY_INSUFFICIENT_STOCK, String.format("Inventory of product(%s) insufficient: Available(%d) Additional needed(%d)", productId, availableQuantity, additionalNeeded));
    }
}
