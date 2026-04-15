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
public class PaymentInboxEventOrchestrator {

    private final InboxEventPayloadUtil inboxEventPayloadUtil;
    private final ReservationPersistService reservationPersistService;

    public void handlePaymentCaptured(InboxEvent inboxEvent){
        //  commit reservations
        PaymentCapturedPayload paymentCapturedPayload = inboxEventPayloadUtil.parsePayload(inboxEvent, PaymentCapturedPayload.class);
        reservationPersistService.commitReservations(paymentCapturedPayload.orderId);
    }

    private record PaymentCapturedPayload(
            UUID orderId
    ){}
}
