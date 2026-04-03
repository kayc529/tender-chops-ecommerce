package com.kaycheung.inventory_service.exception.code;

public enum InventoryErrorCode implements ApiErrorCode {
    INVENTORY_NOT_FOUND("Inventory Not Found"),
    INVENTORY_INSUFFICIENT_STOCK("Inventory Conflict"),
    INVENTORY_INVALID_RESERVATION_REQUEST("Invalid Reservation Request"),
    INVENTORY_QUANTITY_CONFLICT("Inventory Conflict"),
    INVENTORY_INVARIANT_VIOLATION("Inventory Internal Error"),
    INVENTORY_SERVICE_UNAVAILABLE("Inventory Service Unavailable"),
    INVENTORY_BUSY_TRY_AGAIN("Inventory Service Busy");

    private final String message;

    InventoryErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
