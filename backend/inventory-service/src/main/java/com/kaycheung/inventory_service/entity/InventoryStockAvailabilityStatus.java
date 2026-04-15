package com.kaycheung.inventory_service.entity;

public enum InventoryStockAvailabilityStatus {
    IN_STOCK,
    LOW_IN_STOCK,
    OUT_OF_STOCK;

    public static String getAvailabilityStatusWithAvailableStock(int availableStock){
       if(availableStock == 0)
       {
           return OUT_OF_STOCK.name();
       } else if (availableStock > 0 && availableStock < 10) {
           return LOW_IN_STOCK.name();
       } else if (availableStock >= 10) {
           return IN_STOCK.name();
       } else{
           throw new IllegalStateException("Invalid stock count: " + availableStock);
       }
    }
}
