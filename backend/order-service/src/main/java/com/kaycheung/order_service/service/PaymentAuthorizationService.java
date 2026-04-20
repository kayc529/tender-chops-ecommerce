package com.kaycheung.order_service.service;

import com.kaycheung.order_service.client.inventory.InventoryClient;
import com.kaycheung.order_service.client.inventory.InventoryConfirmReservationRequest;
import com.kaycheung.order_service.client.inventory.InventoryConfirmReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentAuthorizationService {

    private final InventoryClient inventoryClient;

    public InventoryConfirmReservationResponse confirmReservation(UUID orderId, UUID quoteId)
    {
        InventoryConfirmReservationRequest request = new InventoryConfirmReservationRequest(orderId, quoteId);
        return inventoryClient.checkReservationForPayment(request);
    }
}
