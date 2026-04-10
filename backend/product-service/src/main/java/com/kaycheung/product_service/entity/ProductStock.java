package com.kaycheung.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_stock")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStock {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    private StockAvailabilityStatus availabilityStatus;

    @Column(name = "stock_version", nullable = false)
    private Long stockVersion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
