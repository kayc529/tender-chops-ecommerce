package com.kaycheung.inventory_service.service;

import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.exception.domain.InventoryNotFoundException;
import com.kaycheung.inventory_service.exception.domain.InventoryTotalQuantityBelowReservedException;
import com.kaycheung.inventory_service.exception.domain.InventoryTotalQuantityNegativeException;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryPersistService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public void createInventoryForNewProduct(UUID productId) {
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .totalQuantity(0)
                .reservedQuantity(0)
                .stockVersion(0)
                .build();

        inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory updateInventoryTotalQuantity(UUID productId, int totalQuantityDelta) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId).orElseThrow(() -> new InventoryNotFoundException(productId));

        int reservedQuantity = inventory.getReservedQuantity();
        int newTotalQuantity = inventory.getTotalQuantity() + totalQuantityDelta;

        if (newTotalQuantity < 0) {
            throw new InventoryTotalQuantityNegativeException(
                    "Total inventory quantity cannot be negative for productId=" + productId
                            + ", currentTotal=" + inventory.getTotalQuantity()
                            + ", delta=" + totalQuantityDelta
                            + ", newTotal=" + newTotalQuantity
            );
        }

        if (newTotalQuantity < reservedQuantity) {
            throw new InventoryTotalQuantityBelowReservedException(newTotalQuantity, reservedQuantity);
        }

        long newStockVersion = inventory.getStockVersion() + 1;

        inventory.setTotalQuantity(newTotalQuantity);
        inventory.setStockVersion(newStockVersion);

        return inventoryRepository.save(inventory);
    }
}
