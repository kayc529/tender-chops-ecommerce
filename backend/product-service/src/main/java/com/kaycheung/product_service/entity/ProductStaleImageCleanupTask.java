package com.kaycheung.product_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "product_stale_image_cleanup_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductStaleImageCleanupTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "thumbnail_key", nullable = false)
    private String thumbnailKey;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "last_error")
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
