package com.kaycheung.inventory_service.messaging.inbox.handler;

import com.kaycheung.inventory_service.messaging.inbox.InboxEvent;
import com.kaycheung.inventory_service.messaging.inbox.InboxEventType;
import com.kaycheung.inventory_service.messaging.inbox.orchestrator.PaymentInboxEventOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentInboxEventHandler implements InboxEventHandler {

    private final PaymentInboxEventOrchestrator paymentInboxEventOrchestrator;

    @Override
    public void handleEvent(InboxEvent inboxEvent) {

        InboxEventType eventType = InboxEventType.from(inboxEvent.getEventType());

        switch (eventType) {
            case PAYMENT_CAPTURED -> onPaymentCaptured(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported payment InboxEventType: " + eventType);
        }
    }

    private void onPaymentCaptured(InboxEvent inboxEvent) {
        paymentInboxEventOrchestrator.handlePaymentCaptured(inboxEvent);
    }
}
