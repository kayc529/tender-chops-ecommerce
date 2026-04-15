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
    private final ReservationPersistService reservationPersistService;
    private final InventoryPersistService inventoryPersistService;

    public void createInventory(InventoryAdminCreateInventoryRequestDTO request) {
        UUID productId = request.productId();
        inventoryPersistService.createInventoryForNewProduct(productId);
    }

    public ReservationCreateResponseDTO createReservation(ReservationCreateRequestDTO request) {
        Instant expiresAt = reservationPersistService.createReservations(request);
        return new ReservationCreateResponseDTO(expiresAt);
    }

    public void releaseReservationsForCompensation(ReservationReleaseRequestDTO request) {
        reservationPersistService.releaseReservations(request.quoteId());
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
