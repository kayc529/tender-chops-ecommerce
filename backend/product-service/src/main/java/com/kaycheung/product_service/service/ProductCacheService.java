package com.kaycheung.product_service.service;

import com.kaycheung.product_service.config.properties.CacheProperties;
import com.kaycheung.product_service.dto.ProductPageResponse;
import com.kaycheung.product_service.dto.ProductResponseDTO;
import com.kaycheung.product_service.entity.ProductCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ProductCacheService {
    private final String keyVersion;
    private final Duration productListTtl;
    private final Duration productDetailTtl;
    private final RedisTemplate<String, Object> redisTemplate;

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate, CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.keyVersion = cacheProperties.getKeyVersion();
        this.productListTtl = Duration.ofMinutes(cacheProperties.getProductListTtlMinutes());
        this.productDetailTtl = Duration.ofMinutes(cacheProperties.getProductDetailTtlMinutes());
    }

    public Optional<ProductPageResponse> getProductList(int page, ProductCategory category) {
        String key = buildProductListKey(page, category);
        Object cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue == null) {
            return Optional.empty();
        }

        if (cachedValue instanceof ProductPageResponse productPageResponse) {
            return Optional.of(productPageResponse);
        }

        log.warn("Cache value type mismatch for key {}. Deleting key and falling back to DB. Actual type: {}",
                key, cachedValue.getClass().getName());
        redisTemplate.delete(key);

        return Optional.empty();
    }

    public void putProductList(ProductPageResponse productPageResponse, ProductCategory category) {
        String key = buildProductListKey(productPageResponse.page(), category);
        redisTemplate.opsForValue().set(key, productPageResponse, productListTtl);
    }

    public Optional<ProductResponseDTO> getProduct(UUID productId) {
        String key = buildProductKey(productId);

        Object cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue == null) {
            return Optional.empty();
        }

        if (cachedValue instanceof ProductResponseDTO productResponseDTO) {
            return Optional.of(productResponseDTO);
        }

        log.warn("Cache value type mismatch for key {}. Deleting key and falling back to DB. Actual type: {}",
                key, cachedValue.getClass().getName());
        redisTemplate.delete(key);

        return Optional.empty();
    }

    public void putProduct(ProductResponseDTO productResponseDTO) {
        String key = buildProductKey(productResponseDTO.id());
        redisTemplate.opsForValue().set(key, productResponseDTO, productDetailTtl);
    }

    public void evictProduct(UUID productId) {
        String key = buildProductKey(productId);
        redisTemplate.delete(key);
    }

    private String buildProductKey(UUID productId) {
        return keyVersion + ":product:" + productId;
    }

    private String buildProductKey(String productId) {
        return keyVersion + ":product:" + productId;
    }

    private String buildProductListKey(int page, ProductCategory category) {
        String categoryName = category != null ? category.name() : "ALL";
        return keyVersion + ":product:list:page=" + page + ":category=" + categoryName;
    }
}
