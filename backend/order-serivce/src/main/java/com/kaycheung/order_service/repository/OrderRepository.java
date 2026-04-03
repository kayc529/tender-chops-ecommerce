package com.kaycheung.order_service.repository;

import com.kaycheung.order_service.entity.Order;
import com.kaycheung.order_service.entity.OrderStatus;
import com.kaycheung.order_service.repository.projection.OrderSourceQuoteIdProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Page<Order> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(UUID userId, Instant start, Instant end, Pageable pageable);
    Optional<Order> findByUserIdAndSourceQuoteId(UUID userId, UUID sourceQuoteId);

    Optional<OrderSourceQuoteIdProjection> findProjectedById(UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Order o
            set o.orderStatus = :toStatus,
                o.updatedAt = CURRENT_TIMESTAMP
            where o.id = :id and o.orderStatus in :fromStatuses
            """)
    int transitionStatusIfIn(
            @Param("id") UUID orderId,
            @Param("toStatus") OrderStatus toStatus,
            @Param("fromStatuses") List<OrderStatus> fromStatuses
    );
}
