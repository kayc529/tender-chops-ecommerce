package com.kaycheung.order_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID userId;
    private UUID sourceQuoteId;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    private String currency;
    private Long totalAmount;

    private String receiver;
    private String phone;
    @Column(name = "address_line_1")
    private String addressLine1;
    @Column(name = "address_line_2")
    private String addressLine2;
    private String city;
    private String stateOrProvince;
    private String postalCode;
    private String country;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
