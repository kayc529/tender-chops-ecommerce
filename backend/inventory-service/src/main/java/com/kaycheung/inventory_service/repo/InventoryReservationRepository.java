package com.kaycheung.inventory_service.repo;

import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservation;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    List<InventoryReservation> findByQuoteIdAndProductIdInAndReservationStatusAndExpiresAtAfterOrderByProductIdAsc(UUID quoteId, List<UUID> productIds, InventoryReservationStatus status, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InventoryReservation> findByQuoteIdOrderByProductIdAsc(UUID quoteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InventoryReservation> findByQuoteIdAndReservationStatus(UUID quoteId, InventoryReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InventoryReservation> findByReservationStatusAndExpiresAtBeforeOrderByProductIdAsc(InventoryReservationStatus status, Instant now, Pageable pageable);
}
