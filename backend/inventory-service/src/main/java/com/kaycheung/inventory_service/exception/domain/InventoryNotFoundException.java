package com.kaycheung.inventory_service.exception.domain;

import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class InventoryNotFoundException extends InventoryException {

    public InventoryNotFoundException(UUID productId) {
        super(HttpStatus.NOT_FOUND, InventoryErrorCode.INVENTORY_NOT_FOUND, "Inventory not found for product: " + productId);
    }

    public InventoryNotFoundException() {
        super(HttpStatus.NOT_FOUND, InventoryErrorCode.INVENTORY_NOT_FOUND, "Inventory not found.");
    }
}
