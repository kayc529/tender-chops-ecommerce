package com.kaycheung.inventory_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.inventory_service.entity.Inventory;
import com.kaycheung.inventory_service.entity.OutboxEvent;
import com.kaycheung.inventory_service.entity.ProcessedEvent;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservation;
import com.kaycheung.inventory_service.entity.inventory_reservation.InventoryReservationStatus;
import com.kaycheung.inventory_service.messaging.contract.EventType;
import com.kaycheung.inventory_service.messaging.contract.InventoryEventReasonCode;
import com.kaycheung.inventory_service.messaging.inbound.PaymentSucceededEvent;
import com.kaycheung.inventory_service.messaging.outbound.InventoryReservationsCommitFailedEvent;
import com.kaycheung.inventory_service.messaging.outbound.InventoryReservationsCommitSucceededEvent;
import com.kaycheung.inventory_service.repo.InventoryRepository;
import com.kaycheung.inventory_service.repo.InventoryReservationRepository;
import com.kaycheung.inventory_service.repo.OutboxEventRepository;
import com.kaycheung.inventory_service.repo.ProcessedEventRepository;
import com.kaycheung.inventory_service.utils.ObjectMapperUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationCommitService {

    private static final Logger log = LoggerFactory.getLogger(ReservationCommitService.class);
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;

    private final ObjectMapperUtils objectMapperUtils;


    @Transactional
    public void commitReservations(PaymentSucceededEvent event) {
        Instant now = Instant.now();

        //  check if current event has already been processed
        try {
            processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId(), EventType.PAYMENT_SUCCEEDED, now));
        } catch (DataIntegrityViolationException ex) {
            log.info("PaymentSucceededEvent already processed, eventId={}", event.eventId());
            return;
        }

        List<InventoryReservation> reservations = reservationRepository.findByQuoteIdOrderByProductIdAsc(event.quoteId());

        if (reservations.isEmpty()) {
            OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.RESERVATION_NOT_FOUND, "No reservations found under this quote id");
            outboxEventRepository.save(outboxEvent);
            return;
        }


        InventoryReservationStatus status = reservations.get(0).getReservationStatus();
        boolean hasMixedStatus = reservations.stream().anyMatch(r->r.getReservationStatus()!=status);

        if (hasMixedStatus) {
            OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.MIXED_STATUS, "Reservations under this quote id have mixed statuses");
            outboxEventRepository.save(outboxEvent);
            return;
        }

        if (status == InventoryReservationStatus.COMMITTED) {
            boolean alreadyEmitted = outboxEventRepository.existsByEventTypeAndAggregateId(EventType.INVENTORY_RESERVATIONS_COMMIT_SUCCEEDED, event.quoteId());
            if (!alreadyEmitted) {
                OutboxEvent outboxEvent = createCommitSucceededOutboxEvent(event, now);
                try {
                    outboxEventRepository.save(outboxEvent);
                } catch (DataIntegrityViolationException ex) {
                    log.info("CommitSucceeded has already been emitted for quoteId={}", event.quoteId());
                }
            }
            return;
        }

        if (status == InventoryReservationStatus.EXPIRED) {
            OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.RESERVATION_EXPIRED, "Reservations under this quote id have expired");
            outboxEventRepository.save(outboxEvent);
            return;
        }

        if (status == InventoryReservationStatus.RELEASED) {
            OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.RESERVATIONS_RELEASED, "Reservations under this quote id have already been released");
            outboxEventRepository.save(outboxEvent);
            return;
        }

        //  unknown status
        if (status != InventoryReservationStatus.RESERVED) {
            OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.RESERVATION_UNKNOWN_STATUS, "Reservations under this quote id have unknown status");
            outboxEventRepository.save(outboxEvent);
            return;
        }


        List<UUID> productIds = reservations.stream().map(InventoryReservation::getProductId).sorted().toList();
        List<Inventory> inventories = inventoryRepository.findByProductIdInForUpdate(productIds);

        if (inventories.size() != productIds.size()) {
            OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.INVARIANT_VIOLATION, "Inventory count does not match with productId");
            outboxEventRepository.save(outboxEvent);
            return;
        }

        Map<UUID, Inventory> inventoryByProductId = inventories.stream().collect(Collectors.toMap(Inventory::getProductId, i -> i));

        for (InventoryReservation reservation : reservations) {
            //  check if reservation expired
            if (!reservation.getExpiresAt().isAfter(now)) {
                OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.RESERVATION_EXPIRED, "Reservations under this quote id have expired");
                outboxEventRepository.save(outboxEvent);
                return;
            }

            Inventory inventory = inventoryByProductId.get(reservation.getProductId());
            if(inventory==null)
            {
                OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.INVARIANT_VIOLATION, "Inventory not found");
                outboxEventRepository.save(outboxEvent);
                return;
            }

            int newReservedQty = inventory.getReservedQuantity() - reservation.getQuantity();
            int newTotalQty = inventory.getTotalQuantity() - reservation.getQuantity();

            if (newReservedQty < 0 || newTotalQty < 0) {
                OutboxEvent outboxEvent = createCommitFailedOutboxEvent(event, now, InventoryEventReasonCode.INVARIANT_VIOLATION, String.format("Inventory's reservedQty/totalQty of productId:%s will go negative if reservation is committed", reservation.getProductId().toString()));
                outboxEventRepository.save(outboxEvent);
                return;
            }

            reservation.setReservationStatus(InventoryReservationStatus.COMMITTED);
            reservation.setCommittedAt(now);

            inventory.setReservedQuantity(newReservedQty);
            inventory.setTotalQuantity(newTotalQty);
        }

        //  save inventories first then reservations
        inventoryRepository.saveAll(inventories);
        reservationRepository.saveAll(reservations);

        OutboxEvent outboxEvent = createCommitSucceededOutboxEvent(event, now);
        try {
            outboxEventRepository.save(outboxEvent);
        } catch (DataIntegrityViolationException ex) {
            log.info("CommitSucceeded has already been emitted for quoteId={}", event.quoteId());
        }
    }

    private OutboxEvent createCommitSucceededOutboxEvent(PaymentSucceededEvent event, Instant now) {
        InventoryReservationsCommitSucceededEvent succeededEvent = createSucceededEvent(event, now);
        String payloadString = objectMapperUtils.toJson(succeededEvent);
        return createOutboxEvent(EventType.INVENTORY_RESERVATIONS_COMMIT_SUCCEEDED, event.quoteId(), payloadString, now);
    }

    private OutboxEvent createCommitFailedOutboxEvent(PaymentSucceededEvent event, Instant now, InventoryEventReasonCode reasonCode, String message) {
        InventoryReservationsCommitFailedEvent failedEvent = createFailedEvent(event, now, reasonCode, message);
        String payloadString = objectMapperUtils.toJson(failedEvent);
        return createOutboxEvent(EventType.INVENTORY_RESERVATIONS_COMMIT_FAILED, event.quoteId(), payloadString, now);
    }

    private InventoryReservationsCommitSucceededEvent createSucceededEvent(PaymentSucceededEvent event, Instant now) {
        return new InventoryReservationsCommitSucceededEvent(event.eventId(), now, event.quoteId(), event.orderId(), event.paymentId());
    }

    private InventoryReservationsCommitFailedEvent createFailedEvent(PaymentSucceededEvent event, Instant now, InventoryEventReasonCode reasonCode, String message) {
        return new InventoryReservationsCommitFailedEvent(event.eventId(), now, event.quoteId(), event.orderId(), event.paymentId(), reasonCode, message);
    }

    private OutboxEvent createOutboxEvent(EventType eventType, UUID aggregateId, String payload, Instant now) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType(eventType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setPayloadJsonString(payload);
        outboxEvent.setOccurredAt(now);
        return outboxEvent;
    }
}
