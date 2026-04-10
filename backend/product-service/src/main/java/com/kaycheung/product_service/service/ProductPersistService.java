package com.kaycheung.product_service.service;

import com.kaycheung.product_service.dto.ProductRequestDTO;
import com.kaycheung.product_service.entity.Product;
import com.kaycheung.product_service.entity.ProductImageStatus;
import com.kaycheung.product_service.entity.ProductStock;
import com.kaycheung.product_service.entity.StockAvailabilityStatus;
import com.kaycheung.product_service.exception.domain.ProductNotFoundException;
import com.kaycheung.product_service.mapper.ProductMapper;
import com.kaycheung.product_service.repository.ProductRepository;
import com.kaycheung.product_service.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductPersistService {
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    @Transactional
    public Product createNewProductAndProductStock(ProductRequestDTO request){
        Product product =  productRepository.save(productMapper.toEntity(request));

        ProductStock productStock = ProductStock.builder()
                .productId(product.getId())
                .availableStock(0)
                .availabilityStatus(StockAvailabilityStatus.OUT_OF_STOCK)
                .stockVersion(0L)
                .build();

        productStockRepository.save(productStock);

        return productRepository.findById(product.getId()).orElseThrow(()->new ProductNotFoundException(product.getId()));
    }

    @Transactional
    public void updateProductImageKeysWithPendingStatus(UUID productId, String imageKey, String thumbnailKey)
    {
        productRepository.updateProductImageForNewUpload(productId, imageKey, thumbnailKey, ProductImageStatus.PENDING);
    }

    @Transactional
    public int updateProductImageForSuccessfulUpload(UUID productId, String pendingImageKey, String pendingThumbnailKey){
        return productRepository.updateProductImageForSuccessfulUpload(productId, pendingImageKey, pendingThumbnailKey, ProductImageStatus.PENDING);
    }

    @Transactional
    public int updateProductImageForFailedUpload(UUID productId, String incomingImageKey){
        return productRepository.updateProductImageForFailedUpload(productId, incomingImageKey, ProductImageStatus.FAILED, ProductImageStatus.PENDING);
    }
}
