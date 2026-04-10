package com.kaycheung.product_service.repository;

import com.kaycheung.product_service.entity.Product;
import com.kaycheung.product_service.entity.ProductImageStatus;
import com.kaycheung.product_service.repository.projection.ProductBasePriceProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    List<ProductBasePriceProjection> findByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = "productStock")
    Page<Product> findAll(@NonNull Specification<Product> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = "productStock")
    Optional<Product> findById(@NonNull UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.pendingImageKey = :imageKey,
                p.pendingThumbnailKey = :thumbnailKey,
                p.imageUploadStatus = :status
            WHERE p.id = :productId
            """)
    int updateProductImageForNewUpload(
            @Param("productId") UUID productId,
            @Param("imageKey") String imageKey,
            @Param("thumbnailKey") String thumbnailKey,
            @Param("status") ProductImageStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.imageKey = :newImageKey,
                p.thumbnailKey = :newThumbnailKey,
                p.pendingImageKey = NULL,
                p.pendingThumbnailKey = NULL,
                p.imageUploadStatus = NULL
            WHERE p.id = :productId AND p.pendingImageKey = :newImageKey AND p.imageUploadStatus =:pendingStatus
            """)
    int updateProductImageForSuccessfulUpload(
            @Param("productId") UUID productId,
            @Param("newImageKey") String newImageKey,
            @Param("newThumbnailKey") String newThumbnailKey,
            @Param("pendingStatus") ProductImageStatus pendingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.imageUploadStatus = :failedStatus
            WHERE p.id = :productId AND p.pendingImageKey = :pendingImageKey AND p.imageUploadStatus = :pendingStatus
            """)
    int updateProductImageForFailedUpload(
            @Param("productId") UUID productId,
            @Param("pendingImageKey")String pendingImageKey,
            @Param("failedStatus")ProductImageStatus failedStatus,
            @Param("pendingStatus")ProductImageStatus pendingStatus
    );
}
