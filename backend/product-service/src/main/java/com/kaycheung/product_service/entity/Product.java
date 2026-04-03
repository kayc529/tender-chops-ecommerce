package com.kaycheung.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private String description;

    @Column(name = "portion_description")
    private String portionDescription;

    //  stored in cents
    private Long basePrice;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_upload_status")
    private ProductImageStatus imageUploadStatus;

    @Column(name = "pending_image_key")
    private String pendingImageKey;

    @Column(name = "pending_thumbnail_key")
    private String pendingThumbnailKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private ProductCategory productCategory;

    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
}
