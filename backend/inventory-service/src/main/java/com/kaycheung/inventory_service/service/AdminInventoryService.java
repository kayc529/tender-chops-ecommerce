package com.kaycheung.inventory_service.service;

import com.kaycheung.inventory_service.dto.InventoryAdminAvailabilityBatchRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminCreateInventoryRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminResponseDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminUpdateInventoryRequestDTO;
import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.exception.domain.InventoryNotFoundException;
import com.kaycheung.inventory_service.exception.domain.InventoryTotalQuantityBelowReservedException;
import com.kaycheung.inventory_service.mapper.AdminInventoryMapper;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminInventoryService {
    private static final Logger log = LoggerFactory.getLogger(AdminInventoryService.class);
    private final InventoryRepository inventoryRepository;
    private final AdminInventoryMapper inventoryMapper;

    public InventoryAdminResponseDTO getAvailability(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
        return inventoryMapper.toDto(inventory);
    }

    public List<InventoryAdminResponseDTO> getAvailabilityBatch(InventoryAdminAvailabilityBatchRequestDTO request) {
        List<Inventory> inventories = inventoryRepository.findByProductIdIn(request.productIds());
        return inventoryMapper.toDtoList(inventories);
    }

    public InventoryAdminResponseDTO createInventory(InventoryAdminCreateInventoryRequestDTO request) {
        UUID productId = request.productId();
        try {
            Inventory newInventory = new Inventory();
            newInventory.setProductId(productId);
            // v1 default -> new products start with 0 stock and 0 reserved
            newInventory.setTotalQuantity(0);
            newInventory.setReservedQuantity(0);
            Inventory saved = inventoryRepository.save(newInventory);
            return inventoryMapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            //  Idempotent + race-safe -> if the UNIQUE(product_id) constraint caused this exception
            //  if row with the same product id already exists (or was created concurrently), return it
            Inventory existingInventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> ex);
            return inventoryMapper.toDto(existingInventory);
        }
    }

    //  admin cannot update reservedQuantity -> reservation's logic
    public InventoryAdminResponseDTO updateInventory(UUID productId, InventoryAdminUpdateInventoryRequestDTO request) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> new InventoryNotFoundException(productId));

        int reservedQuantity = inventory.getReservedQuantity();

        //  total quantity guardrail
        if (request.totalQuantity() < reservedQuantity) {
            throw new InventoryTotalQuantityBelowReservedException(request.totalQuantity(), reservedQuantity);
        }

        inventory.setTotalQuantity(request.totalQuantity());
        //  make sure the timestamp updated
        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toDto(saved);
    }

    public void deleteInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
        inventoryRepository.delete(inventory);
    }
}
