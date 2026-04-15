package com.kaycheung.inventory_service.messaging.inbox.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.inventory_service.messaging.inbox.InboxEvent;
import com.kaycheung.inventory_service.service.InventoryPersistService;
import com.kaycheung.inventory_service.utils.InboxEventPayloadUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductInboxEventOrchestrator {

    private final InboxEventPayloadUtil inboxEventPayloadUtil;
    private final InventoryPersistService inventoryPersistService;

    public void handleProductCreated(InboxEvent inboxEvent) {
        ProductCreatedPayload productCreatedPayload = inboxEventPayloadUtil.parsePayload(inboxEvent, ProductCreatedPayload.class);
        inventoryPersistService.createInventoryForNewProduct(productCreatedPayload.productId);
    }


    private record ProductCreatedPayload(
            UUID productId
    ) {
    }
}
