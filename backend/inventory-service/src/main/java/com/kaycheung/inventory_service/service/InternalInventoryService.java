package com.kaycheung.inventory_service.service;

import com.kaycheung.inventory_service.config.InventoryReservationProperties;
import com.kaycheung.inventory_service.dto.InventoryAdminCreateInventoryRequestDTO;
import com.kaycheung.inventory_service.dto.InventoryAdminResponseDTO;
import com.kaycheung.inventory_service.dto.internal.*;
import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservation;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservationStatus;
import com.kaycheung.inventory_service.exception.domain.InventoryInsufficientStockException;
import com.kaycheung.inventory_service.exception.domain.InventoryInvalidReservationRequestException;
import com.kaycheung.inventory_service.exception.domain.InventoryInvariantViolationException;
import com.kaycheung.inventory_service.exception.domain.InventoryNotFoundException;
import com.kaycheung.inventory_service.mapper.AdminInventoryMapper;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import com.kaycheung.inventory_service.repo.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InternalInventoryService {
    private static final Logger log = LoggerFactory.getLogger(InternalInventoryService.class);
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final AdminInventoryMapper inventoryMapper;
    private final InventoryReservationProperties reservationProperties;

    public InventoryAdminResponseDTO createInventory(InventoryAdminCreateInventoryRequestDTO request) {
        UUID productId = request.productId();
        try {
            Inventory newInventory = new Inventory();
            newInventory.setProductId(productId);
            // v1 default -> new products start with 0 stock and 0 reserved
            newInventory.setTotalQuantity(0);
            newInventory.setReservedQuantity(0);
            Inventory saved = inventoryRepository.save(newInventory);
            return inventoryMapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            Inventory existingInventory = inventoryRepository.findByProductId(productId).orElseThrow(() -> ex);
            return inventoryMapper.toDto(existingInventory);
        }
    }

    @Transactional
    public ReservationCreateResponseDTO createReservation(ReservationCreateRequestDTO request) {
        //  1. Validate request
        Map<UUID, Integer> quantityByProductId = new HashMap<>();
        for (ReservationCreateRequestItem item : request.itemsToReserve()) {
            //  check for duplicate products
            if (quantityByProductId.containsKey(item.productId())) {
                throw new InventoryInvalidReservationRequestException();
            }

            //  guardrail for negative requested quantity
            if (item.quantity() <= 0) {
                throw new InventoryInvalidReservationRequestException();
            }

            quantityByProductId.put(item.productId(), item.quantity());
        }

        //  sorted productId list for batch fetch (to prevent deadlock)
        List<UUID> productIdsSorted = quantityByProductId.keySet().stream().sorted().toList();


        //  2. Load Data
        //  batch fetch (with pessimistic locking) the inventory of the above productIds
        Instant now = Instant.now();
        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIdsSorted);
        if (inventories.size() != productIdsSorted.size()) {
            throw new InventoryNotFoundException();
        }
        //  batch fetch existing reservations (quoteId + productId) with status and expiresAt filter
        List<InventoryReservation> existingReservations = reservationRepository.findByQuoteIdAndProductIdInAndReservationStatusAndExpiresAtAfterOrderByProductIdAsc(request.quoteId(), productIdsSorted, InventoryReservationStatus.RESERVED, now);

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

            if (existingReservation != null) {
                int delta = requestedQuantity - existingReservation.getQuantity();
                int newQty = originalReservedQty + delta;

                inventory.setReservedQuantity(newQty);

                existingReservation.setExpiresAt(expiresAt);
                existingReservation.setQuantity(requestedQuantity);
                existingReservation.setReservationStatus(InventoryReservationStatus.RESERVED);

                reservationsToPersist.add(existingReservation);
            } else {
                inventory.setReservedQuantity(originalReservedQty + requestedQuantity);

                InventoryReservation newReservation = new InventoryReservation();
                newReservation.setProductId(productId);
                newReservation.setQuoteId(request.quoteId());
                newReservation.setQuantity(requestedQuantity);
                newReservation.setReservationStatus(InventoryReservationStatus.RESERVED);
                newReservation.setCreatedAt(now);
                newReservation.setExpiresAt(expiresAt);

                reservationsToPersist.add(newReservation);
            }

            inventoryToPersist.add(inventory);
        }


        //  5. Persist
        //  saveAll updated inventories and new/updated reservations
        inventoryRepository.saveAll(inventoryToPersist);
        reservationRepository.saveAll(reservationsToPersist);

        //  6. Return expiresAt if transaction is successful
        return new ReservationCreateResponseDTO(expiresAt);
    }

    @Transactional
    public void releaseReservationsForCompensation(ReservationReleaseRequestDTO request) {

        //  fetch all reservations that has status RESERVED and matching quoteId
        //  add pessimistic lock to reservation fetch to prevent concurrency issues
        List<InventoryReservation> reservations = reservationRepository.findByQuoteIdAndReservationStatus(request.quoteId(), InventoryReservationStatus.RESERVED);

        //  if no reservations fetched -> return (idempotent release; API may be called multiple times)
        if (reservations.isEmpty()) {
            return;
        }

        //  fetch all (with lock) inventories with productIds (sorted list to prevent deadlock)
        List<UUID> productIds = reservations.stream().map(InventoryReservation::getProductId).sorted().toList();
        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIds);

        if (inventories.size() != productIds.size()) {
            throw new InventoryInvariantViolationException("Inventory rows missing while releasing reservations for quoteId=" + request.quoteId());
        }

        //  map inventory with productId (unique constraint on product Id, no duplicate productIds)
        Map<UUID, Inventory> inventoryByProductId = inventories.stream().collect(Collectors.toMap(Inventory::getProductId, i -> i));

        //  loop through reservations
        Instant now = Instant.now();

        for (InventoryReservation reservation : reservations) {
            Inventory inventory = inventoryByProductId.get(reservation.getProductId());

            if (inventory == null) {
                throw new InventoryInvariantViolationException(
                        "Inventory not found for productId=" + reservation.getProductId() + " while releasing reservations for quoteId=" + request.quoteId()
                );
            }

            //  set status to RELEASED, releasedAt to now
            reservation.setReservationStatus(InventoryReservationStatus.RELEASED);
            reservation.setReleasedAt(now);

            //  check if inventory.reservedQuantity < reservation.quantity () -> invariant violation
            if (inventory.getReservedQuantity() < reservation.getQuantity()) {
                throw new InventoryInvariantViolationException(
                        "Reserved quantity underflow detected for productId=" + reservation.getProductId()
                                + " reserved=" + inventory.getReservedQuantity()
                                + " reservationQty=" + reservation.getQuantity()
                                + " quoteId=" + request.quoteId()
                );
            }

            //  newReserved = inventory.reservedQuantity - reservation.quantity, newReserved should not be negative -> invariant violation
            int newReserved = inventory.getReservedQuantity() - reservation.getQuantity();
            if (newReserved < 0) {
                throw new InventoryInvariantViolationException(
                        "Inventory reservedQuantity would become negative for productId=" + reservation.getProductId()
                                + " after releasing reservationQty=" + reservation.getQuantity()
                                + " quoteId=" + request.quoteId()
                );
            }

            //  update reservedQty of inventory to newReserved
            inventory.setReservedQuantity(newReserved);
        }


        //  persist reservations and inventories in batch
        inventoryRepository.saveAll(inventoryByProductId.values());
        reservationRepository.saveAll(reservations);
    }

    @Transactional
    public ReservationConfirmResponseDTO confirmReservation(ReservationConfirmRequestDTO request) {
        Instant now = Instant.now();

        // Fetch all reservations for the quote (no lock). We are only validating current state here.
        List<InventoryReservation> reservations = reservationRepository.findByQuoteIdOrderByProductIdAsc(request.quoteId());

        if (reservations.isEmpty()) {
            return new ReservationConfirmResponseDTO(false, ReservationConfirmResponseDTO.ReservationConfirmReason.RESERVATION_NOT_FOUND.name());
        }

        // Check reservation state first.
        for (InventoryReservation reservation : reservations) {
            if (reservation.getReservationStatus() != InventoryReservationStatus.RESERVED) {
                return new ReservationConfirmResponseDTO(false, ReservationConfirmResponseDTO.ReservationConfirmReason.RESERVATION_NOT_RESERVED.name());
            }

            Instant expiresAt = reservation.getExpiresAt();
            if (expiresAt == null || !expiresAt.isAfter(now)) {
                return new ReservationConfirmResponseDTO(false, ReservationConfirmResponseDTO.ReservationConfirmReason.RESERVATION_EXPIRED.name());
            }
        }

        // Lock inventories in stable productId order before verifying reserved backing.
        List<UUID> productIdsSorted = reservations.stream()
                .map(InventoryReservation::getProductId)
                .sorted()
                .toList();

        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIdsSorted);
        if (inventories.size() != productIdsSorted.size()) {
            return new ReservationConfirmResponseDTO(false, ReservationConfirmResponseDTO.ReservationConfirmReason.INVENTORY_NOT_FOUND.name());
        }

        Map<UUID, Inventory> inventoryByProductId = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        // Reservation is confirmable only if each reservation is still backed by current reserved stock.
        for (InventoryReservation reservation : reservations) {
            Inventory inventory = inventoryByProductId.get(reservation.getProductId());
            if (inventory == null) {
                return new ReservationConfirmResponseDTO(false, ReservationConfirmResponseDTO.ReservationConfirmReason.INVENTORY_NOT_FOUND.name());
            }

            if (inventory.getReservedQuantity() < reservation.getQuantity()) {
                log.error("Reservation is no longer backed by reserved inventory. quoteId={} productId={} reservedQty={} reservationQty={}",
                        request.quoteId(), reservation.getProductId(), inventory.getReservedQuantity(), reservation.getQuantity());
                return new ReservationConfirmResponseDTO(false, ReservationConfirmResponseDTO.ReservationConfirmReason.RESERVATION_NOT_BACKED_BY_INVENTORY.name());
            }
        }

        return new ReservationConfirmResponseDTO(true, null);
    }
}
