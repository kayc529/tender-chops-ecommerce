package com.kaycheung.product_service.repository;

import com.kaycheung.product_service.entity.ProductStock;
import com.kaycheung.product_service.entity.StockAvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStock, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
                            update ProductStock ps
                            set ps.availableStock = :stock,
                                ps.availabilityStatus = :status,
                                ps.stockVersion = :stockVersion
                            where ps.productId = :id and ps.stockVersion < :stockVersion
                    """
    )
    int updateProductStockWithNewerStockVersion(
            @Param("id") UUID productId,
            @Param("stock") int availableStock,
            @Param("status") StockAvailabilityStatus availabilityStatus,
            @Param("stockVersion") long stockVersion
    );
}
