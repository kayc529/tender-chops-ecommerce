package com.kaycheung.product_service.service;

import com.kaycheung.product_service.entity.StockAvailabilityStatus;
import com.kaycheung.product_service.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductStockPersistService {

    private final ProductStockRepository productStockRepository;
    private final ProductCacheService productCacheService;

    @Transactional
    public int updateProductStock(UUID productId, int availableStock, StockAvailabilityStatus availabilityStatus, long stockVersion) {
        int updatedRows = productStockRepository.updateProductStockWithNewerStockVersion(productId, availableStock, availabilityStatus, stockVersion);

        if (updatedRows > 0) {
            productCacheService.evictProduct(productId);
        }

        return updatedRows;
    }
}
