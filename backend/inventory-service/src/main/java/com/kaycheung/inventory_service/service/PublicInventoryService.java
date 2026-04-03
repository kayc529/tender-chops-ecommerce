package com.kaycheung.inventory_service.service;

import com.kaycheung.inventory_service.dto.InventoryAvailabilityBatchRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAvailabilityResponseDTO;
import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.exception.domain.InventoryNotFoundException;
import com.kaycheung.inventory_service.mapper.PublicInventoryMapper;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicInventoryService {

    private final InventoryRepository inventoryRepository;
    private final PublicInventoryMapper inventoryMapper;

    public InventoryAvailabilityResponseDTO getAvailability(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
        return inventoryMapper.toDto(inventory);
    }

    public List<InventoryAvailabilityResponseDTO> getAvailabilityBatch(InventoryAvailabilityBatchRequestDTO request)
    {
        List<Inventory> inventories = inventoryRepository.findByProductIdIn(request.productIds());
        return inventoryMapper.toDtoList(inventories);
    }
}
