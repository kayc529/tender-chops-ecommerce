package com.kaycheung.inventory_service.mapper;

import com.kaycheung.inventory_service.dto.InventoryAdminResponseDTO;
import com.kaycheung.inventory_service.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminInventoryMapper {

    @Mapping(target = "availableQuantity", expression = ("java(calculateAvailableQuantity(inventory.getTotalQuantity(), inventory.getReservedQuantity()))"))
    InventoryAdminResponseDTO toDto(Inventory inventory);

    List<InventoryAdminResponseDTO> toDtoList(List<Inventory> inventories);

    default int calculateAvailableQuantity(int total, int reserve){
        return total - reserve;
    }
}
