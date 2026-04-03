package com.kaycheung.inventory_service.controller;

import com.kaycheung.inventory_service.dto.InventoryAdminCreateInventoryRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminResponseDTO;
import com.kaycheung.inventory_service.dto.internal.*;
import com.kaycheung.inventory_service.service.InternalInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/inventory")
public class InternalInventoryController {

    private final InternalInventoryService inventoryService;

    @PostMapping
    public InventoryAdminResponseDTO createInventory(@Valid @RequestBody InventoryAdminCreateInventoryRequestDTO request) {
        return inventoryService.createInventory(request);
    }

    @PostMapping("/reservations")
    public ReservationCreateResponseDTO createReservation(@Valid @RequestBody ReservationCreateRequestDTO request) {
        return inventoryService.createReservation(request);
    }

    @PostMapping("/reservations/release-for-compensation")
    public void releaseReservationsForCompensation(@Valid @RequestBody ReservationReleaseRequestDTO request) {
        inventoryService.releaseReservationsForCompensation(request);
    }

    @PostMapping("/reservations/release-for-cancellation")
    public void releaseReservationsForCancellation(@Valid @RequestBody ReservationReleaseRequestDTO request) {
        inventoryService.releaseReservationsForCompensation(request);
    }

    @PostMapping("/reservations/confirm-for-payment")
    public ReservationConfirmResponseDTO confirmReservationsForPayment(@Valid @RequestBody ReservationConfirmRequestDTO request) {
        return inventoryService.confirmReservation(request);
    }
}
