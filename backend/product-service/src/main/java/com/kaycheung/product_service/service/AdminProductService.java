package com.kaycheung.product_service.service;

import com.kaycheung.product_service.client.InventoryClient;
import com.kaycheung.product_service.client.InventoryCreateRequestDTO;
import com.kaycheung.product_service.client.InventoryResponseDTO;
import com.kaycheung.product_service.config.properties.AwsS3Properties;
import com.kaycheung.product_service.dto.ProductRequestDTO;
import com.kaycheung.product_service.dto.ProductResponseDTO;
import com.kaycheung.product_service.dto.UploadProductImageRequestDTO;
import com.kaycheung.product_service.dto.UploadProductImageResponseDTO;
import com.kaycheung.product_service.entity.Product;
import com.kaycheung.product_service.exception.domain.ProductImageFormatInvalidException;
import com.kaycheung.product_service.exception.domain.ProductImageUploadUrlGenerationException;
import com.kaycheung.product_service.exception.domain.ProductNotFoundException;
import com.kaycheung.product_service.mapper.ProductMapper;
import com.kaycheung.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final ProductPersistService productPersistService;
    private final S3Presigner s3Presigner;
    private final AwsS3Properties s3BucketProperties;
    private final InventoryClient inventoryClient;
    private final ProductCacheService productCacheService;

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request){
        Product product =  productRepository.save(productMapper.toEntity(request));

        //  create inventory for new product
        InventoryCreateRequestDTO inventoryRequest = new InventoryCreateRequestDTO(product.getId());
        InventoryResponseDTO inventoryCreated = inventoryClient.createInventoryForNewProduct(inventoryRequest);

        log.info(inventoryCreated.toString());
        return productMapper.toDetailDto(product);
    }

    public ProductResponseDTO updateProduct(UUID productId, ProductRequestDTO request)
    {
        Product product = productRepository.findById(productId).orElseThrow(()-> new ProductNotFoundException(productId));
        productMapper.updateProductFromRequestDTO(request, product);
        productRepository.save(product);

        productCacheService.evictProduct(productId);

        return productMapper.toDetailDto(product);
    }

    public void deleteProduct(UUID productId)
    {
        Product product = productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException(productId));
        productRepository.delete(product);

        productCacheService.evictProduct(productId);
    }

    public UploadProductImageResponseDTO createImageUploadUrl(UUID productId, UploadProductImageRequestDTO request) {
        productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));

        String extension;

        switch (request.contentType()) {
            case "image/jpeg" -> extension = "jpg";
            case "image/png" -> extension = "png";
            default -> {
                log.warn("Invalid image content type received for productId={}, contentType={}", productId, request.contentType());
                throw new ProductImageFormatInvalidException(request.contentType() + " is not a valid format.");
            }
        }

        UUID imageId = UUID.randomUUID();
        String imageKey = "products/" + productId + "/original/" + imageId + "." + extension;
        String thumbnailKey = "products/" + productId + "/thumbnails/" + imageId + "." + extension;

        productPersistService.updateProductImageKeysWithPendingStatus(productId, imageKey, thumbnailKey);

        String presignedUrl = getS3PresignedUrl(imageKey, request.contentType());

        log.info("Created image upload intent for productId={}, imageKey={}, thumbnailKey={}, contentType={}",
                productId, imageKey, thumbnailKey, request.contentType());

        return new UploadProductImageResponseDTO(presignedUrl, request.contentType());
    }

    private String getS3PresignedUrl(String imageKey, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketProperties.getOriginalBucketName())
                    .key(imageKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(s3BucketProperties.getPresignDurationMinutes()))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);

            return presignedPutObjectRequest.url().toString();

        } catch (Exception ex) {
            log.error("Failed to generate presigned upload URL for imageKey={}, bucket={}, contentType={}",
                    imageKey, s3BucketProperties.getOriginalBucketName(), contentType, ex);
            throw new ProductImageUploadUrlGenerationException();
        }
    }
}
