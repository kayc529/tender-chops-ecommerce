package com.kaycheung.payment_service.messaging.inbox.orchestrator;

import com.kaycheung.payment_service.messaging.inbox.handler.OrderInboxEventHandler;
import com.kaycheung.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderInboxEventOrchestrator {

    private final PaymentService paymentService;

    // TODO v2:
    // If ORDER_READY_TO_CAPTURE inbox event becomes dead after maxRetries,
    // the payment may remain stuck in PENDING and the order in PROCESSING.
    // Implement a reconciliation worker that:
    //   1) scans payments stuck in PENDING with AUTHORIZED attempts
    //   2) queries Stripe PaymentIntent status
    //   3) emits the appropriate event (CAPTURED / FAILED / CANCELED)
    // This avoids orders remaining indefinitely in PROCESSING.
    public void handleOrderReadyToCapture(OrderInboxEventHandler.OrderEventPayload payload) {
        paymentService.capturePaymentIntent(payload.orderId(), payload.paymentId(), payload.paymentAttemptId());
    }

    public void handleOrderDoNotCapture(OrderInboxEventHandler.OrderEventPayload payload) {
        UUID orderId = payload.orderId();
        UUID paymentId = payload.paymentId();
        UUID paymentAttemptId = payload.paymentAttemptId();
        int updatedRows = paymentService.cancelPaymentForDoNotCapture(orderId, paymentId, paymentAttemptId);

        if (updatedRows == 0) {
            log.warn("Skip payment->CANCELED on ORDER_DO_NOT_CAPTURE: payment not in PENDING state. orderId={} paymentId={} paymentAttemptId={}", orderId, paymentId, paymentAttemptId);
        }
    }

    public void handleOrderCanceled(OrderInboxEventHandler.OrderEventPayload payload) {
        UUID orderId = payload.orderId();
        int updatedRows = paymentService.cancelPayment(orderId);

        if (updatedRows == 0) {
            log.warn("Skip payment->CANCELED on ORDER_CANCELED: payment not in PENDING state. orderId={}", orderId);
        }
    }

}
