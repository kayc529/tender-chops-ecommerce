package com.kaycheung.product_service.messaging.inbox.handler;

import com.kaycheung.product_service.messaging.inbox.InboxEvent;
import com.kaycheung.product_service.messaging.inbox.InboxEventProcessResult;
import com.kaycheung.product_service.messaging.inbox.InboxEventProcessStatus;
import com.kaycheung.product_service.messaging.inbox.InboxEventType;
import com.kaycheung.product_service.messaging.inbox.orchestrator.InventoryBatchInboxEventOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryBatchInboxEventHandler implements BatchInboxEventHandler {

    private final InventoryBatchInboxEventOrchestrator inventoryBatchInboxEventOrchestrator;

    @Override
    public List<InboxEventProcessResult> handleEvents(List<InboxEvent> inboxEvents) {
        InboxEventType eventType = InboxEventType.from(inboxEvents.get(0).getEventType());

        switch (eventType) {
            case INVENTORY_STOCK_UPDATED -> {
                return onInventoryStockUpdated(inboxEvents);
            }
            default -> {
                return onUnknown(inboxEvents);
            }
        }
    }

    private List<InboxEventProcessResult> onInventoryStockUpdated(List<InboxEvent> inboxEvents) {
        return inventoryBatchInboxEventOrchestrator.onInventoryStockUpdated(inboxEvents);
    }

    private List<InboxEventProcessResult> onUnknown(List<InboxEvent> inboxEvents) {
        List<InboxEventProcessResult> results = new ArrayList<>();

        for (InboxEvent inboxEvent : inboxEvents) {
            InboxEventProcessResult processResult = new InboxEventProcessResult(inboxEvent, InboxEventProcessStatus.DEAD, "Unknown event type");
            results.add(processResult);
        }

        return results;
    }
}
