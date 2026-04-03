package com.kaycheung.product_service.service;

import com.kaycheung.product_service.config.properties.AwsSecretsProperties;
import com.kaycheung.product_service.dto.ProductImageUploadResultRequestDTO;
import com.kaycheung.product_service.entity.Product;
import com.kaycheung.product_service.entity.ProductImageStatus;
import com.kaycheung.product_service.exception.domain.ProductImageUnauthorizedAccessException;
import com.kaycheung.product_service.exception.domain.ProductNotFoundException;
import com.kaycheung.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductRepository productRepository;
    private final AwsSecretsProperties awsSecretsProperties;
    private final ProductPersistService productPersistService;
    private final AwsS3Service awsS3Service;
    private final ProductStaleImageCleanupTaskPersistService productStaleImageCleanupTaskPersistService;
    private final ProductCacheService productCacheService;


    public void confirmProductImageUploadResult(String token, ProductImageUploadResultRequestDTO request) {
        if (!Objects.equals(awsSecretsProperties.getImageProcessorCallbackSecret(), token)) {
            throw new ProductImageUnauthorizedAccessException();
        }

        UUID productId = request.productId();

        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));

        String incomingImageKey = request.imageKey();
        String incomingThumbnailKey = request.thumbnailKey();
        String pendingImageKey = product.getPendingImageKey();
        String pendingThumbnailKey = product.getPendingThumbnailKey();

        if (!incomingImageKey.equals(pendingImageKey) || !incomingThumbnailKey.equals(pendingThumbnailKey)) {
            try {
                awsS3Service.removeS3Objects(productId, incomingImageKey, incomingThumbnailKey);
            } catch (Exception ex) {
                log.error("Failed to delete S3 objects for productId={}, imageKey={}, thumbnailKey={}",
                        productId, incomingImageKey, incomingThumbnailKey, ex);
                productStaleImageCleanupTaskPersistService.createTask(productId, incomingImageKey, incomingThumbnailKey);
            }
            return;
        }

        // failed upload
        if (!request.success()) {
            //  update image upload status to FAILED
            int updatedRows = productPersistService.updateProductImageForFailedUpload(productId, incomingImageKey);

            if (updatedRows == 0) {
                log.warn("Stale or no-op FAILED image callback. productId={}, imageKey={}, reason=Pending image no longer matches or status already updated",
                        productId, incomingImageKey);
            }

            // whether updated or stale, still delete incoming objects
            try {
                awsS3Service.removeS3Objects(productId, incomingImageKey, incomingThumbnailKey);
            } catch (Exception ex) {
                log.error("Failed to delete S3 objects for productId={}, imageKey={}, thumbnailKey={}",
                        productId, incomingImageKey, incomingThumbnailKey, ex);
                productStaleImageCleanupTaskPersistService.createTask(productId, incomingImageKey, incomingThumbnailKey);
            }
            return;
        }

        String originalImageKey = product.getImageKey();
        String originalThumbnailKey = product.getThumbnailKey();

        int updatedRow = productPersistService.updateProductImageForSuccessfulUpload(productId, pendingImageKey, pendingThumbnailKey);

        if (updatedRow > 0) {
            productCacheService.evictProduct(productId);
        }

        //  data are stale
        if (updatedRow == 0) {
            //  stale data - delete S3 objects with incomingImageKey & incomingThumbnailKey
            try {
                awsS3Service.removeS3Objects(productId, incomingImageKey, incomingThumbnailKey);
            } catch (Exception ex) {
                log.error("Failed to delete S3 objects for productId={}, imageKey={}, thumbnailKey={}",
                        productId, incomingImageKey, incomingThumbnailKey, ex);
                productStaleImageCleanupTaskPersistService.createTask(productId, incomingImageKey, incomingThumbnailKey);
            }
            return;
        }


        //  delete S3 objects with originalImageKey/originalThumbnailKey
        if (originalImageKey != null && originalThumbnailKey != null) {
            try {
                awsS3Service.removeS3Objects(productId, originalImageKey, originalThumbnailKey);
            } catch (Exception ex) {
                log.error("Failed to delete S3 objects for productId={}, imageKey={}, thumbnailKey={}",
                        productId, incomingImageKey, incomingThumbnailKey, ex);
                productStaleImageCleanupTaskPersistService.createTask(productId, originalImageKey, originalThumbnailKey);
            }
        }
    }

}
