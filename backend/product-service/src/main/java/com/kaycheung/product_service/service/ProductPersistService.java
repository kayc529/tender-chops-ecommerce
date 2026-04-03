package com.kaycheung.product_service.service;

import com.kaycheung.product_service.entity.ProductImageStatus;
import com.kaycheung.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductPersistService {
    private final ProductRepository productRepository;

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
