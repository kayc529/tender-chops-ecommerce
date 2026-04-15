package com.kaycheung.product_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaycheung.product_service.client.InventoryClient;
import com.kaycheung.product_service.client.InventoryCreateRequestDTO;
import com.kaycheung.product_service.config.properties.AwsS3Properties;
import com.kaycheung.product_service.dto.ProductRequestDTO;
import com.kaycheung.product_service.dto.ProductResponseDTO;
import com.kaycheung.product_service.dto.UploadProductImageRequestDTO;
import com.kaycheung.product_service.dto.UploadProductImageResponseDTO;
import com.kaycheung.product_service.entity.Product;
import com.kaycheung.product_service.entity.ProductStock;
import com.kaycheung.product_service.entity.StockAvailabilityStatus;
import com.kaycheung.product_service.exception.domain.ProductImageFormatInvalidException;
import com.kaycheung.product_service.exception.domain.ProductImageUploadUrlGenerationException;
import com.kaycheung.product_service.exception.domain.ProductNotFoundException;
import com.kaycheung.product_service.mapper.ProductMapper;
import com.kaycheung.product_service.messaging.outbox.OutboxEvent;
import com.kaycheung.product_service.messaging.outbox.OutboxEventService;
import com.kaycheung.product_service.messaging.outbox.OutboxEventType;
import com.kaycheung.product_service.repository.ProductRepository;
import com.kaycheung.product_service.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    private final ProductPersistService productPersistService;
    private final ProductCacheService productCacheService;
    private final OutboxEventService outboxEventService;

    private final S3Presigner s3Presigner;
    private final AwsS3Properties s3BucketProperties;

    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Product product = productPersistService.createNewProductAndProductStock(request);

        //  create OutboxEvent PRODUCT_CREATED
        String key = "product:" + product.getId() + ":created";
        String payload = buildOutboxPayloadForCreateProduct(product);
        outboxEventService.createOutboxEvent(OutboxEventType.PRODUCT_CREATED, payload, key);

        return productMapper.toDetailDto(product);
    }

    public ProductResponseDTO updateProduct(UUID productId, ProductRequestDTO request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        productMapper.updateProductFromRequestDTO(request, product);
        productRepository.save(product);

        productCacheService.evictProduct(productId);

        return productMapper.toDetailDto(product);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
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

    private String buildOutboxPayloadForCreateProduct(Product product)
    {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("productId", product.getId().toString());

        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload (PRODUCT_CREATED) for productId==" + product.getId(), e);
        }
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
