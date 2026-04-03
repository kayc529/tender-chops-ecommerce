package com.kaycheung.inventory_service.exception.domain;

import com.kaycheung.inventory_service.exception.code.InventoryErrorCode;
import org.springframework.http.HttpStatus;

public class InventoryInvalidReservationRequestException extends InventoryException {
    public InventoryInvalidReservationRequestException() {
        super(HttpStatus.BAD_REQUEST, InventoryErrorCode.INVENTORY_INVALID_RESERVATION_REQUEST, "Duplicate productId in reservation request");
    }
}
