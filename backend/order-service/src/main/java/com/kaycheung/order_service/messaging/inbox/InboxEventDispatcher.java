package com.kaycheung.order_service.messaging.inbox;

import com.kaycheung.order_service.messaging.inbox.handler.PaymentInboxEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InboxEventDispatcher {

    private final PaymentInboxEventHandler paymentInboxEventHandler;

    public void dispatchEvent(InboxEvent inboxEvent) {
        InboxEventType eventType = InboxEventType.valueOf(inboxEvent.getEventType());

        switch (eventType) {
            case PAYMENT_ATTEMPT_AUTHORIZED,
                 PAYMENT_ATTEMPT_CANCELED,
                 PAYMENT_ATTEMPT_FAILED,
                 PAYMENT_CAPTURED,
                 PAYMENT_CANCELED -> paymentInboxEventHandler.handleEvent(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported InboxEventType: " + eventType);
        }
    }
}
