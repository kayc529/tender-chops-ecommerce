package com.kaycheung.inventory_service.service;

import com.kaycheung.inventory_service.config.InventoryReservationProperties;
import com.kaycheung.inventory_service.dto.internal.ReservationCreateRequestDTO;
import com.kaycheung.inventory_service.dto.internal.ReservationCreateRequestItem;
import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.entity.InventoryStockAvailabilityStatus;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservation;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservationStatus;
import com.kaycheung.inventory_service.exception.domain.*;
import com.kaycheung.inventory_service.messaging.outbox.OutboxEventService;
import com.kaycheung.inventory_service.messaging.outbox.OutboxEventType;
import com.kaycheung.inventory_service.messaging.outbox.payload.InventoryStockUpdatedPayload;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import com.kaycheung.inventory_service.repo.InventoryReservationRepository;
import com.kaycheung.inventory_service.utils.ObjectMapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationPersistService {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final ObjectMapperUtils objectMapperUtils;
    private final OutboxEventService outboxEventService;
    private final InventoryReservationProperties reservationProperties;

    @Transactional
    public int updateReservationOrderId(UUID quoteId, UUID orderId) {
        return reservationRepository.updateReservationOrderId(quoteId, orderId);
    }

    @Transactional
    public Instant createReservations(ReservationCreateRequestDTO request)
    {
        //  1. Validate request
        Map<UUID, Integer> quantityByProductId = new HashMap<>();
        for (ReservationCreateRequestItem item : request.itemsToReserve()) {
            //  check for duplicate products
            if (quantityByProductId.containsKey(item.productId())) {
                throw new InventoryInvalidReservationRequestException();
            }

            quantityByProductId.put(item.productId(), item.quantity());
        }

        //  sorted productId list for batch fetch (to prevent deadlock)
        List<UUID> productIdsSorted = quantityByProductId.keySet().stream().sorted().toList();


        //  2. Load Data
        //  batch fetch (with pessimistic locking) the inventory of the above productIds
        UUID quoteId = request.quoteId();
        Instant now = Instant.now();
        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIdsSorted);

        if (inventories.size() != productIdsSorted.size()) {
            throw new InventoryInvariantViolationException("Inventory rows missing while releasing reservations for quoteId=" + quoteId);
        }

        //  batch fetch existing reservations (quoteId + productId) with status and expiresAt filter
        List<InventoryReservation> existingReservations = reservationRepository.findByQuoteIdAndProductIdInAndReservationStatusAndExpiresAtAfterOrderByProductIdAsc(quoteId, productIdsSorted, InventoryReservationStatus.RESERVED, now);

        Map<UUID, Inventory> inventoryByProductId = inventories.stream().collect(Collectors.toMap(Inventory::getProductId, i -> i));
        Map<UUID, InventoryReservation> existingReservationByProductId = existingReservations.stream().collect(Collectors.toMap(InventoryReservation::getProductId, r -> r));

        //  3. Validate stock (all or nothing)
        for (UUID productId : productIdsSorted) {
            InventoryReservation existingReservation = existingReservationByProductId.get(productId);
            Inventory inventory = inventoryByProductId.get(productId);
            int requestedQuantity = quantityByProductId.get(productId);
            int availableQty = inventory.getTotalQuantity() - inventory.getReservedQuantity();
            int originalReservedQty = inventory.getReservedQuantity();
            int additionalNeeded = 0;

            //  if the product has an existing reservation, check if the delta in quantity (newQty - oldQty) > 0
            if (existingReservation != null) {
                int delta = requestedQuantity - existingReservation.getQuantity();
                if (delta > 0) {
                    additionalNeeded = delta;
                }
                //  check for inventory state invariant violation
                if (originalReservedQty + delta < 0) {
                    log.error(String.format("originalReservedQty + delta < 0 for quoteId: %s productId: %s", request.quoteId(), productId));
                    throw new InventoryInvariantViolationException("reservedQuantity would become negative for productId=" + productId);
                }
            } else {
                additionalNeeded = requestedQuantity;
            }

            if (availableQty < additionalNeeded) {
                throw new InventoryInsufficientStockException(productId, availableQty, additionalNeeded);
            }
        }

        //  4. Apply updates
        //  check existing reservations
        //  if no -> create new reservation, update inventory reservedQuantity
        //  if yes -> update the quantity and TTL of the old reservation, and update inventory reservedQuantity (apply delta)
        Instant expiresAt = now.plus(reservationProperties.getTtl());
//        Instant expiresAt = now.plus(Duration.ofMinutes(15));
        List<InventoryReservation> reservationsToPersist = new ArrayList<>();
        List<Inventory> inventoryToPersist = new ArrayList<>();

        for (UUID productId : productIdsSorted) {
            InventoryReservation existingReservation = existingReservationByProductId.get(productId);
            Inventory inventory = inventoryByProductId.get(productId);
            int requestedQuantity = quantityByProductId.get(productId);
            int originalReservedQty = inventory.getReservedQuantity();
            int newReservedQty;

            if (existingReservation != null) {
                int delta = requestedQuantity - existingReservation.getQuantity();
                newReservedQty = originalReservedQty + delta;

                existingReservation.setExpiresAt(expiresAt);
                existingReservation.setQuantity(requestedQuantity);
                existingReservation.setReservationStatus(InventoryReservationStatus.RESERVED);

                reservationsToPersist.add(existingReservation);
            } else {
                newReservedQty = originalReservedQty + requestedQuantity;

                InventoryReservation newReservation = new InventoryReservation();
                newReservation.setProductId(productId);
                newReservation.setQuoteId(request.quoteId());
                newReservation.setQuantity(requestedQuantity);
                newReservation.setReservationStatus(InventoryReservationStatus.RESERVED);
                newReservation.setCreatedAt(now);
                newReservation.setExpiresAt(expiresAt);

                reservationsToPersist.add(newReservation);
            }

            long newStockVersion = inventory.getStockVersion() + 1;
            inventory.setReservedQuantity(newReservedQty);
            inventory.setStockVersion(newStockVersion);

            int newAvailableStock = inventory.getTotalQuantity() - newReservedQty;
            String payload = getInventoryStockUpdatedPayloadString(productId, newAvailableStock, newStockVersion);
            String key = getInventoryStockUpdatedIdempotencyKey(productId, newStockVersion);
            outboxEventService.createOutboxEvent(OutboxEventType.INVENTORY_STOCK_UPDATED, payload, key);

            inventoryToPersist.add(inventory);
        }

        //  5. Persist
        //  saveAll updated inventories and new/updated reservations
        inventoryRepository.saveAll(inventoryToPersist);
        reservationRepository.saveAll(reservationsToPersist);

        return expiresAt;
    }

    @Transactional
    public void releaseReservations(UUID quoteId) {
        //  fetch all reservations that has status RESERVED and matching quoteId
        //  add pessimistic lock to reservation fetch to prevent concurrency issues
        List<InventoryReservation> reservations = reservationRepository.findByQuoteIdAndReservationStatus(quoteId, InventoryReservationStatus.RESERVED);

        //  if no reservations fetched -> return (idempotent release; API may be called multiple times)
        if (reservations.isEmpty()) {
            return;
        }

        //  fetch all (with lock) inventories with productIds (sorted list to prevent deadlock)
        List<UUID> productIds = reservations.stream().map(InventoryReservation::getProductId).sorted().toList();
        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIds);

        if (inventories.size() != productIds.size()) {
            throw new InventoryInvariantViolationException("Inventory rows missing while releasing reservations for quoteId=" + quoteId);
        }

        //  map inventory with productId (unique constraint on product Id, no duplicate productIds)
        Map<UUID, Inventory> inventoryByProductId = inventories.stream().collect(Collectors.toMap(Inventory::getProductId, i -> i));

        //  loop through reservations
        Instant now = Instant.now();

        for (InventoryReservation reservation : reservations) {
            UUID productId = reservation.getProductId();
            Inventory inventory = inventoryByProductId.get(productId);

            if (inventory == null) {
                throw new InventoryInvariantViolationException(
                        "Inventory not found for productId=" + productId + " while releasing reservations for quoteId=" + quoteId
                );
            }

            //  set status to RELEASED, releasedAt to now
            reservation.setReservationStatus(InventoryReservationStatus.RELEASED);
            reservation.setReleasedAt(now);

            //  check if inventory.reservedQuantity < reservation.quantity () -> invariant violation
            if (inventory.getReservedQuantity() < reservation.getQuantity()) {
                throw new InventoryInvariantViolationException(
                        "Reserved quantity underflow detected for productId=" + productId
                                + " reserved=" + inventory.getReservedQuantity()
                                + " reservationQty=" + reservation.getQuantity()
                                + " quoteId=" + quoteId
                );
            }

            //  newReserved = inventory.reservedQuantity - reservation.quantity, newReserved should not be negative -> invariant violation
            int newReserved = inventory.getReservedQuantity() - reservation.getQuantity();
            if (newReserved < 0) {
                throw new InventoryInvariantViolationException(
                        "Inventory reservedQuantity would become negative for productId=" + productId
                                + " after releasing reservationQty=" + reservation.getQuantity()
                                + " quoteId=" + quoteId
                );
            }

            long newStockVersion = inventory.getStockVersion() + 1;
            inventory.setReservedQuantity(newReserved);
            inventory.setStockVersion(newStockVersion);

            int newAvailableStock = inventory.getTotalQuantity() - newReserved;
            String payload = getInventoryStockUpdatedPayloadString(productId, newAvailableStock, newStockVersion);
            String key = getInventoryStockUpdatedIdempotencyKey(productId, newStockVersion);
            outboxEventService.createOutboxEvent(OutboxEventType.INVENTORY_STOCK_UPDATED, payload, key);
        }

        //  persist reservations and inventories in batch
        inventoryRepository.saveAll(inventories);
        reservationRepository.saveAll(reservations);
    }

    @Transactional
    public void commitReservations(UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderIdOrderByProductIdAsc(orderId);

        if (reservations.isEmpty()) {
            throw new InventoryInvariantViolationException(
                    "No reservations found while committing reservations for orderId=" + orderId
            );
        }

        InventoryReservationStatus status = reservations.get(0).getReservationStatus();
        boolean hasMixedStatus = reservations.stream().anyMatch(r -> r.getReservationStatus() != status);

        if (hasMixedStatus) {
            throw new InventoryInvariantViolationException(
                    "Mixed reservation statuses found while committing reservations for orderId=" + orderId
            );
        }

        //  reservations have already been committed
        if (status == InventoryReservationStatus.COMMITTED) {
            log.debug("Reservations already committed for orderId={}", orderId);
            return;
        }

        if (status == InventoryReservationStatus.EXPIRED || status == InventoryReservationStatus.RELEASED) {
            throw new InventoryBusinessFailureException(
                    "Reservations cannot be committed because they are already in terminal status=" + status + " for orderId=" + orderId
            );
        }

        List<UUID> productIds = reservations.stream().map(InventoryReservation::getProductId).sorted().toList();
        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIds);

        if (inventories.size() != productIds.size()) {
            throw new InventoryInvariantViolationException(
                    "Inventory rows missing while committing reservations for orderId=" + orderId
            );
        }

        Map<UUID, Inventory> inventoryByProductId = inventories.stream().collect(Collectors.toMap(Inventory::getProductId, i -> i));
        Instant now = Instant.now();

        for (InventoryReservation reservation : reservations) {
            //  check if reservation expired
            UUID productId = reservation.getProductId();

            if (!reservation.getExpiresAt().isAfter(now)) {
                throw new InventoryBusinessFailureException(
                        "Reservation expired before commit for productId=" + productId
                                + " orderId=" + orderId
                                + " expiresAt=" + reservation.getExpiresAt()
                                + " now=" + now
                );
            }

            Inventory inventory = inventoryByProductId.get(productId);
            if (inventory == null) {
                throw new InventoryInvariantViolationException(
                        "Inventory not found for productId=" + productId
                                + " while committing reservations for orderId=" + orderId
                );
            }

            int newReservedQty = inventory.getReservedQuantity() - reservation.getQuantity();
            int newTotalQty = inventory.getTotalQuantity() - reservation.getQuantity();

            if (newReservedQty < 0 || newTotalQty < 0) {
                throw new InventoryInvariantViolationException(
                        "Inventory quantity underflow detected while committing reservation for productId=" + productId
                                + " orderId=" + orderId
                                + " reservedBefore=" + inventory.getReservedQuantity()
                                + " totalBefore=" + inventory.getTotalQuantity()
                                + " reservationQty=" + reservation.getQuantity()
                                + " reservedAfter=" + newReservedQty
                                + " totalAfter=" + newTotalQty
                );
            }

            reservation.setReservationStatus(InventoryReservationStatus.COMMITTED);
            reservation.setCommittedAt(now);

            long newStockVersion = inventory.getStockVersion() + 1;
            inventory.setReservedQuantity(newReservedQty);
            inventory.setTotalQuantity(newTotalQty);
            inventory.setStockVersion(newStockVersion);

            int newAvailableStock = newTotalQty - newReservedQty;
            String payload = getInventoryStockUpdatedPayloadString(productId, newAvailableStock, newStockVersion);
            String key = getInventoryStockUpdatedIdempotencyKey(productId, newStockVersion);
            outboxEventService.createOutboxEvent(OutboxEventType.INVENTORY_STOCK_UPDATED, payload, key);
        }

        //  save inventories first then reservations
        inventoryRepository.saveAll(inventories);
        reservationRepository.saveAll(reservations);
    }

    private String getInventoryStockUpdatedPayloadString(UUID productId, int availableStock, long stockVersion) {
        String stockStatus = InventoryStockAvailabilityStatus.getAvailabilityStatusWithAvailableStock(availableStock);
        InventoryStockUpdatedPayload payloadObject = new InventoryStockUpdatedPayload(productId, availableStock, stockStatus, stockVersion);
        return objectMapperUtils.toJson(payloadObject);
    }

    private String getInventoryStockUpdatedIdempotencyKey(UUID productId, long stockVersion) {
        return "inventory-stock-updated:product:" + productId + ":v:" + stockVersion;
    }
}
