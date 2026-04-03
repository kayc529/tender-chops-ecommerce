package com.kaycheung.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "order_item")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID orderId;

    private UUID productId;
    private String productTitleSnapshot;
    private Long unitPriceSnapshot;
    private int quantity;
    private Long lineTotalAmount;
}
