package com.kaycheung.inventory_service.mapper;

import com.kaycheung.inventory_service.dto.InventoryAvailabilityResponseDTO;
import com.kaycheung.inventory_service.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PublicInventoryMapper {

    @Mapping(target = "availableQuantity", expression = "java(calculateAvailableQuantity(inventory.getTotalQuantity(), inventory.getReservedQuantity()))")
    InventoryAvailabilityResponseDTO toDto(Inventory inventory);

    List<InventoryAvailabilityResponseDTO> toDtoList(List<Inventory> inventories);

    default int calculateAvailableQuantity(int total, int reserve){
        return total - reserve;
    }
}
