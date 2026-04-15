package com.kaycheung.inventory_service.service;

import com.kaycheung.inventory_service.dto.InventoryAdminAvailabilityBatchRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminResponseDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminUpdateInventoryRequestDTO;
import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.entity.InventoryStockAvailabilityStatus;
import com.kaycheung.inventory_service.exception.domain.InventoryNotFoundException;
import com.kaycheung.inventory_service.mapper.AdminInventoryMapper;
import com.kaycheung.inventory_service.messaging.outbox.OutboxEventService;
import com.kaycheung.inventory_service.messaging.outbox.OutboxEventType;
import com.kaycheung.inventory_service.messaging.outbox.payload.InventoryStockUpdatedPayload;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import com.kaycheung.inventory_service.utils.ObjectMapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInventoryService {

    private final InventoryRepository inventoryRepository;
    private final AdminInventoryMapper inventoryMapper;
    private final InventoryPersistService inventoryPersistService;
    private final OutboxEventService outboxEventService;
    private final ObjectMapperUtils objectMapperUtils;

    public InventoryAdminResponseDTO getAvailability(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
        return inventoryMapper.toDto(inventory);
    }

    public List<InventoryAdminResponseDTO> getAvailabilityBatch(InventoryAdminAvailabilityBatchRequestDTO request) {
        List<Inventory> inventories = inventoryRepository.findByProductIdIn(request.productIds());
        return inventoryMapper.toDtoList(inventories);
    }

    //  admin cannot update reservedQuantity -> reservation's logic
    @Transactional
    public InventoryAdminResponseDTO updateInventory(UUID productId, InventoryAdminUpdateInventoryRequestDTO request) {
        Inventory updateInventory = inventoryPersistService.updateInventoryTotalQuantity(productId, request.totalQuantityDelta());

        int availableStock = updateInventory.getTotalQuantity() - updateInventory.getReservedQuantity();
        long stockVersion = updateInventory.getStockVersion();
        String payload = getInventoryStockUpdatedPayloadString(productId, availableStock, stockVersion);
        String key = getInventoryStockUpdatedIdempotencyKey(productId, stockVersion);

        outboxEventService.createOutboxEvent(OutboxEventType.INVENTORY_STOCK_UPDATED, payload, key);

        return inventoryMapper.toDto(updateInventory);
    }

    public void deleteInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
        inventoryRepository.delete(inventory);
    }

    private String getInventoryStockUpdatedPayloadString(UUID productId, int availableStock, long stockVersion) {
        String stockStatus = InventoryStockAvailabilityStatus.getAvailabilityStatusWithAvailableStock(availableStock);
        InventoryStockUpdatedPayload payloadObject = new InventoryStockUpdatedPayload(productId, availableStock, stockStatus, stockVersion);
        return objectMapperUtils.toJson(payloadObject);
    }

    private String getInventoryStockUpdatedIdempotencyKey(UUID productId, long stockVersion) {
        return "inventory-stock-updated:product:" + productId + ":v:" + stockVersion;
    }
}
