package com.kaycheung.inventory_service.job.reservation_expiry;

import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservation;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservationStatus;
import com.kaycheung.inventory_service.exception.domain.InventoryInvariantViolationException;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import com.kaycheung.inventory_service.repo.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationExpiryService {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;


    @Transactional
    public void releaseExpiredReservations(int batchSize) {
        Instant now = Instant.now();
        var page = PageRequest.of(0, batchSize);

        //  fetch all reservations (with pessimistic lock) with RESERVED status and expires at less than now (can set a limit on count)
        List<InventoryReservation> reservations = reservationRepository.findByReservationStatusAndExpiresAtBeforeOrderByProductIdAsc(InventoryReservationStatus.RESERVED, now, page);

        //  if reservations count = 0 -> return
        if (reservations.isEmpty()) {
            return;
        }

        //  group all the productIds into a set, sorted
        List<UUID> productIdSorted = reservations.stream().map(InventoryReservation::getProductId).distinct().sorted().toList();
        //  fetch all inventories (with pessimistic lock)  with productId list
        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIdSorted);

        //  log product ids of missing inventory rows
        if (inventories.size() != productIdSorted.size()) {
            Set<UUID> fetchedProductIds = inventories.stream()
                    .map(Inventory::getProductId)
                    .collect(Collectors.toSet());
            List<UUID> missingProductIds = productIdSorted.stream()
                    .filter(productId -> !fetchedProductIds.contains(productId))
                    .toList();

            log.error("Missing inventory rows while expiring reservations. missingProductIds={}", missingProductIds);
        }

        //  map inventories by productId
        Map<UUID, Inventory> inventoryByProductId = inventories.stream().collect(Collectors.toMap(Inventory::getProductId, i -> i));
        List<InventoryReservation> releasableReservations = new ArrayList<>();
        Set<UUID> touchedProductIds = new HashSet<>();

        //  TODO(v2): mark invariant-violating reservations as dead/broken
        //  so corrupt rows do not get retried forever by the expiry worker
        //  loop through reservation list
        for (InventoryReservation reservation : reservations) {
            UUID productId = reservation.getProductId();
            Inventory inventory = inventoryByProductId.get(productId);

            if (inventory == null) {
                log.error("Skip expiring reservation because inventory row is missing. reservationId={} productId={} quoteId={}",
                        reservation.getId(), productId, reservation.getQuoteId());
                continue;
            }

            int newReservedQty = inventory.getReservedQuantity() - reservation.getQuantity();

            if (newReservedQty < 0) {
                log.error("Skip expiring reservation because reservedQuantity would go negative. reservationId={} productId={} quoteId={} reservedQty={} reservationQty={}",
                        reservation.getId(), productId, reservation.getQuoteId(), inventory.getReservedQuantity(), reservation.getQuantity());
                continue;
            }

            //  set reservation status to EXPIRED, set releasedAt to now
            reservation.setReservationStatus(InventoryReservationStatus.EXPIRED);
            reservation.setReleasedAt(now);

            //  set inventory.reservedQuantity to newReservedQty
            inventory.setReservedQuantity(newReservedQty);

            releasableReservations.add(reservation);
            touchedProductIds.add(productId);
        }

        if (releasableReservations.isEmpty()) {
            return;
        }

        //  filter out the only inventory rows with changes
        List<Inventory> touchedInventories = inventories.stream()
                .filter(inventory -> touchedProductIds.contains(inventory.getProductId()))
                .toList();

        //  save all changed reservations & inventories
        inventoryRepository.saveAll(touchedInventories);
        reservationRepository.saveAll(releasableReservations);
    }
}
