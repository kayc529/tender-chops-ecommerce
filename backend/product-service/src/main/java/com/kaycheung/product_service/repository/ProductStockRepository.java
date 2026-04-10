package com.kaycheung.product_service.repository;

import com.kaycheung.product_service.entity.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStock, UUID> {
}
