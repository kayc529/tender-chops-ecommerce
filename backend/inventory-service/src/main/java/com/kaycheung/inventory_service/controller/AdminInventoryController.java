package com.kaycheung.inventory_service.controller;

import com.kaycheung.inventory_service.dto.InventoryAdminAvailabilityBatchRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminCreateInventoryRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminResponseDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminUpdateInventoryRequestDTO;
import com.kaycheung.inventory_service.service.AdminInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

    private final AdminInventoryService adminInventoryService;

    @GetMapping("/{productId}")
    public InventoryAdminResponseDTO getAvailability(@PathVariable("productId") UUID productId) {
        return adminInventoryService.getAvailability(productId);
    }

    @PostMapping("/batch")
    public List<InventoryAdminResponseDTO> getAvailabilityBatch(@Valid @RequestBody InventoryAdminAvailabilityBatchRequestDTO request) {
        return adminInventoryService.getAvailabilityBatch(request);
    }

    @PutMapping("/{productId}")
    public InventoryAdminResponseDTO updateInventory(@PathVariable("productId") UUID productId, @Valid @RequestBody InventoryAdminUpdateInventoryRequestDTO request) {
        return adminInventoryService.updateInventory(productId, request);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteInventory(@PathVariable("productId")UUID productId)
    {
        adminInventoryService.deleteInventory(productId);
        return ResponseEntity.noContent().build();
    }

}
