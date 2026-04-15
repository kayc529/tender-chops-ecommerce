package com.kaycheung.inventory_service.messaging.inbox.orchestrator;

import com.kaycheung.inventory_service.messaging.inbox.InboxEvent;
import com.kaycheung.inventory_service.service.ReservationPersistService;
import com.kaycheung.inventory_service.utils.InboxEventPayloadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInboxEventOrchestrator {

    private final InboxEventPayloadUtil inboxEventPayloadUtil;
    private final ReservationPersistService reservationPersistService;

    public void handleOrderCreated(InboxEvent inboxEvent) {
        //  reservations have already been created synchronously when order was created
        //  but the reservations did not know the orderId at that time so this is to update the reservations' orderId column
        OrderCreatedPayload orderCreatedPayload = inboxEventPayloadUtil.parsePayload(inboxEvent, OrderCreatedPayload.class);
        int updatedRows = reservationPersistService.updateReservationOrderId(orderCreatedPayload.quoteId, orderCreatedPayload.orderId);
        if (updatedRows == 0) {
            log.debug(
                    "No reservation rows updated for quoteId={}, orderId={}. Possibly already processed or no matching reservations.",
                    orderCreatedPayload.quoteId(),
                    orderCreatedPayload.orderId()
            );
        }
    }

    public void handleOrderCanceled(InboxEvent inboxEvent) {
        //  release reservations
        OrderCanceledPayload orderCanceledPayload = inboxEventPayloadUtil.parsePayload(inboxEvent, OrderCanceledPayload.class);
        reservationPersistService.releaseReservations(orderCanceledPayload.quoteId);
    }

    public void handleOrderCreationFailed(InboxEvent inboxEvent)
    {
        OrderCreationFailedPayload orderCreationFailedPayload = inboxEventPayloadUtil.parsePayload(inboxEvent, OrderCreationFailedPayload.class);
        reservationPersistService.releaseReservations(orderCreationFailedPayload.quoteId);
    }


    private record OrderCreatedPayload(
            UUID quoteId,
            UUID orderId
    ) {
    }

    private record OrderCanceledPayload(
            UUID quoteId
    ) {
    }

    private record OrderCreationFailedPayload(
            UUID quoteId
    ){

    }


}
