package com.kaycheung.order_service.event;

import com.kaycheung.order_service.client.cart.CartClient;
import com.kaycheung.order_service.client.inventory.InventoryClient;
import com.kaycheung.order_service.client.inventory.InventoryReleaseReservationRequestDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;

    // TEMP: synchronous
    // TODO Will be moved to SNS/SQS consumer when payment-service is added.

//    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event){
        log.info("OrderCreatedEvent event - UUID={}", event.userId());
        cartClient.emptyCart(event.userId());
    }

//    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCanceled(OrderCanceledEvent event)
    {
        log.info("OrderCanceledEvent event - sourceQuoteId={}", event.sourceQuoteId());
        InventoryReleaseReservationRequestDTO request = new InventoryReleaseReservationRequestDTO(event.sourceQuoteId());
        inventoryClient.releaseReservationsForCancellation(request);
    }
}
