package com.kaycheung.order_service.messaging.inbox.orchestrator;


import com.kaycheung.order_service.client.inventory.InventoryConfirmReservationResponse;
import com.kaycheung.order_service.exception.client.InventoryClientException;
import com.kaycheung.order_service.messaging.inbox.handler.PaymentInboxEventHandler;
import com.kaycheung.order_service.repository.projection.OrderSourceQuoteIdProjection;
import com.kaycheung.order_service.service.OrderPersistenceService;
import com.kaycheung.order_service.service.PaymentAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentInboxEventOrchestrator {
    private final PaymentAuthorizationService paymentAuthorizationService;
    private final OrderPersistenceService orderPersistenceService;

    public void handlePaymentAttemptAuthorized(UUID inboxEventId, PaymentInboxEventHandler.PaymentEventPayload p) {
        // 1) Re-check reservation validity (source of truth = inventory/reservation layer)
        InventoryConfirmReservationResponse response;
        OrderSourceQuoteIdProjection orderQuoteId = orderPersistenceService.getOrderSourceQuoteId(p.orderId());

        try {
            response = paymentAuthorizationService.confirmReservation(p.orderId(), orderQuoteId.getSourceQuoteId());
        } catch (InventoryClientException ex) {
            log.error(
                    "Inventory service error while confirming reservation. orderId={} inboxEventId={} status={} errorCode={}",
                    p.orderId(), inboxEventId, ex.getStatus(), ex.getErrorCode(), ex
            );
            throw ex;
        } catch (RuntimeException ex) {
            // transient failure: let worker retry by throwing
            log.error("Failed to re-check reservation validity for orderId={} inboxEventId={}", p.orderId(), inboxEventId, ex);
            throw new RuntimeException("Failed to re-check reservation validity for orderId=" + p.orderId(), ex);
        }

        if (response == null) {
            throw new IllegalStateException(
                    "Inventory service returned null for orderId=" + p.orderId()
            );
        }

        log.info("confirmReservation response={}", response);

        if (response.reservationFulfilled()) {
            orderPersistenceService.markProcessingAndEnqueueReadyToCapture(inboxEventId, p);
            return;
        }

        // NOT allowed to capture:
        orderPersistenceService.markExpiredAndEnqueueDoNotCapture(inboxEventId, p, response.reason());
    }

    public void handlePaymentAttemptFailed(UUID inboxEventId, PaymentInboxEventHandler.PaymentEventPayload p) {
        // Expected order status on entry: PENDING_PAYMENT
        // Transition: order -> PAYMENT_FAILED (retry allowed)
        int updated = orderPersistenceService.markPaymentFailedIfPayable(p.orderId());
        if (updated == 0) {
            // Already moved (canceled/expired/processing/paid/etc). Nothing to do.
            log.warn("Skip order->PAYMENT_FAILED: order not in payable state anymore. orderId={} paymentId={} paymentAttemptId={} inboxEventId={}",
                    p.orderId(), p.paymentId(), p.paymentAttemptId(), inboxEventId);
        }
    }

    public void handlePaymentCaptured(UUID inboxEventId, PaymentInboxEventHandler.PaymentEventPayload p) {
        // Expected order status on entry: PROCESSING
        // Transition: order -> PAID
        // v1 rule: if order is already CANCELED/EXPIRED (or anything else), do NOT flip to PAID; log + alert (refund flow later).
        //  TODO(v2) refund if order is already CANCELED/EXPIRED
        int updated = orderPersistenceService.markPaidIfProcessing(p.orderId());

        if (updated == 0) {
            // Either:
            // - event re-delivered (already PAID), OR
            // - order is not in PROCESSING (canceled/expired/pending_payment/payment_failed/etc)
            // In v1, do not force it to PAID
            log.warn(
                    "Skip order->PAID on PAYMENT_CAPTURED: order not in PROCESSING. orderId={} paymentId={} paymentAttemptId={} inboxEventId={}",
                    p.orderId(), p.paymentId(), p.paymentAttemptId(), inboxEventId);
        }
    }
}
