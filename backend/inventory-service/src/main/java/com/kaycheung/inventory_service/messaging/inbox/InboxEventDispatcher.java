package com.kaycheung.inventory_service.messaging.inbox;

import com.kaycheung.inventory_service.messaging.inbox.handler.OrderInboxEventHandler;
import com.kaycheung.inventory_service.messaging.inbox.handler.PaymentInboxEventHandler;
import com.kaycheung.inventory_service.messaging.inbox.handler.ProductInboxEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventDispatcher {

    private final ProductInboxEventHandler productInboxEventHandler;
    private final OrderInboxEventHandler orderInboxEventHandler;
    private final PaymentInboxEventHandler paymentInboxEventHandler;

    public void dispatchEvent(InboxEvent inboxEvent) {
        InboxEventType eventType = InboxEventType.valueOf(inboxEvent.getEventType());
        log.info("Dispatching event: {}, source={}", eventType, inboxEvent.getSource());

        switch (eventType) {
            case PRODUCT_CREATED -> productInboxEventHandler.handleEvent(inboxEvent);
            case    ORDER_CREATED,
                    ORDER_CREATION_FAILED,
                    ORDER_CANCELED
                 -> orderInboxEventHandler.handleEvent(inboxEvent);
            case PAYMENT_CAPTURED -> paymentInboxEventHandler.handleEvent(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported InboxEventType: " + eventType);
        }
    }
}
