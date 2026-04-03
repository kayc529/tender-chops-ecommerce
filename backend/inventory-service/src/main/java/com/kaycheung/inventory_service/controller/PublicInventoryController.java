package com.kaycheung.inventory_service.controller;

import com.kaycheung.inventory_service.dto.InventoryAvailabilityBatchRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAvailabilityResponseDTO;
import com.kaycheung.inventory_service.service.PublicInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class PublicInventoryController {

    private final PublicInventoryService publicInventoryService;

    @GetMapping("/{productId}")
    public InventoryAvailabilityResponseDTO getAvailability(@PathVariable("productId")UUID productId){
        return publicInventoryService.getAvailability(productId);
    }

    @PostMapping("/batch")
    public List<InventoryAvailabilityResponseDTO> getAvailabilityBatch(@Valid @RequestBody InventoryAvailabilityBatchRequestDTO request)
    {
        return publicInventoryService.getAvailabilityBatch(request);
    }
}
