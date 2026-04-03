package com.kaycheung.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaycheung.payment_service.domain.Providers;
import com.kaycheung.payment_service.entity.*;
import com.kaycheung.payment_service.exception.BadWebhookPayloadException;
import com.kaycheung.payment_service.messaging.outbox.OutboxEvent;
import com.kaycheung.payment_service.messaging.outbox.OutboxEventRepository;
import com.kaycheung.payment_service.messaging.outbox.OutboxEventType;
import com.kaycheung.payment_service.repository.CheckoutSessionExpireTaskRepository;
import com.kaycheung.payment_service.repository.PaymentAttemptRepository;
import com.kaycheung.payment_service.repository.PaymentRepository;
import com.kaycheung.payment_service.repository.ProcessedProviderEventRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WebhooksService {
    private static final Logger log = LoggerFactory.getLogger(WebhooksService.class);

    private final ProcessedProviderEventRepository processedProviderEventRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CheckoutSessionExpireTaskRepository checkoutSessionExpireTaskRepository;
    private final PaymentService paymentService;

    private final ObjectMapper objectMapper;

    //  TODO hash rawPayload
    @Transactional
    public void handleStripeWebhookEvent(Event event, String rawPayload) {
        String type = event.getType();
        Optional<StripeObject> maybeObj = event.getDataObjectDeserializer().getObject();

        if (maybeObj.isEmpty()) {
            throw new BadWebhookPayloadException("Bad payload");
        }

        ProcessedProviderEvent processedProviderEvent = new ProcessedProviderEvent();
        processedProviderEvent.setProviderEventId(event.getId());
        processedProviderEvent.setProvider(Providers.STRIPE.name());
        processedProviderEvent.setReceivedAt(Instant.now());

        try {
            processedProviderEventRepository.save(processedProviderEvent);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Event eventId={} already processed", event.getId());
            return;
        }

        Object obj = maybeObj.get();
        String eventId = event.getId();

        switch (type) {
            //  user completed the checkout process on the Stripe-hosted page
            case "checkout.session.completed" -> {
                if (!(obj instanceof Session session)) {
                    log.warn("checkout.session.completed eventId={}: unexpected data.object type {}", eventId, obj.getClass().getName());
                    return;
                }

                String sessionId = session.getId();
                String paymentIntentId = session.getPaymentIntent();

                if (sessionId == null) {
                    log.warn("checkout.session.completed eventId={}: sessionId missing", eventId);
                    return;
                }

                if (paymentIntentId == null) {
                    log.warn("checkout.session.completed eventId={} sessionId={}: paymentIntentId missing", eventId, sessionId);
                    return;
                }

                PaymentAttempt paymentAttempt = paymentAttemptRepository.findByCheckoutSessionId(sessionId).orElse(null);

                if (paymentAttempt == null) {
                    log.warn("checkout.session.completed eventId={} sessionId={}: payment attempt not found", eventId, sessionId);
                    return;
                }

                if (paymentAttempt.getPaymentIntentId() == null) {
                    //  TODO only update paymentIntentId
                    paymentService.updatePaymentAttemptPaymentIntentId(paymentAttempt.getId(), paymentIntentId);
                } else if (!paymentAttempt.getPaymentIntentId().equals(paymentIntentId)) {
                    log.warn("checkout.session.completed eventId={} sessionId={}: paymentIntentId mismatch - paymentAttempt.paymentIntentId={}, incoming paymentIntentId={}", eventId, sessionId, paymentAttempt.getPaymentIntentId(), paymentIntentId);
                }
            }

            //  payment has been authorized but not yet captured
            case "payment_intent.amount_capturable_updated" -> {
                if (!(obj instanceof PaymentIntent paymentIntent)) {
                    log.warn("payment_intent.amount_capturable_updated eventId={}: unexpected data.object type {}", eventId, obj.getClass().getName());
                    return;
                }

                String paymentIntentId = paymentIntent.getId();

                //  guardrail - non-manual payment intent
                if (paymentIntent.getCaptureMethod() == null || !"manual".equals(paymentIntent.getCaptureMethod())) {
                    log.warn("payment_intent.amount_capturable_updated eventId={} paymentIntentId={}: this is not a manual capture payment intent", eventId, paymentIntentId);
                    return;
                }

                //  guardrail - zero amount payment intent
                if (paymentIntent.getAmountCapturable() == null || paymentIntent.getAmountCapturable() <= 0) {
                    log.warn("payment_intent.amount_capturable_updated eventId={} paymentIntentId={}: invalid capturable amount({})", eventId, paymentIntentId, paymentIntent.getAmountCapturable());
                    return;
                }

                PaymentAttempt paymentAttempt = resolvePaymentAttemptFromPaymentIntent("payment_intent.amount_capturable_updated", eventId, paymentIntent);

                if (paymentAttempt == null) {
                    return;
                }

                int updatedRows = paymentAttemptRepository.transitionStatus(paymentAttempt.getId(), PaymentAttemptStatus.AUTH_PENDING, PaymentAttemptStatus.AUTHORIZED);

                if (updatedRows == 0) {
                    PaymentAttempt fresh = paymentAttemptRepository.findById(paymentAttempt.getId()).orElse(null);
                    if (fresh == null) {
                        log.warn("payment_intent.amount_capturable_updated eventId={} paymentIntentId={}: paymentAttempt disappeared - id={}",
                                eventId, paymentIntentId, paymentAttempt.getId());
                        return;
                    }

                    if (fresh.getPaymentAttemptStatus() != PaymentAttemptStatus.AUTHORIZED) {
                        return;
                    }

                    paymentAttempt = fresh;
                }


                Payment payment = paymentRepository.findById(paymentAttempt.getPaymentId()).orElse(null);
                if (payment == null) {
                    log.warn("payment_intent.amount_capturable_updated eventId={} paymentIntentId={}: payment not found - paymentId={}", eventId, paymentIntentId, paymentAttempt.getPaymentId());
                    return;
                }

                //  *** ONLY publish PaymentAuthorized event if: ***
                //  1) paymentAttempt.paymentAttemptStatus is updated (from AUTH_PENDING to AUTHORIZED)
                //  2) paymentAttempt.id == payment.currentAttemptId
                //  3) payment.paymentStatus is NOT terminal (CAPTURED/CANCELED/FAILED)
                if (payment.getCurrentAttemptId() == null) {
                    log.warn("payment_intent.amount_capturable_updated eventId={} paymentIntentId={}: payment.currentAttemptId is null - paymentId={}", eventId, paymentIntentId, payment.getId());
                    return;
                }

                // publish Payment Authorized event to outbox
                if (paymentAttempt.getId().equals(payment.getCurrentAttemptId()) && !payment.getPaymentStatus().isTerminal()) {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("paymentId", payment.getId().toString());
                    node.put("paymentAttemptId", paymentAttempt.getId().toString());
                    node.put("orderId", payment.getOrderId().toString());

                    String key = "payment:" + payment.getId() + ":attempt:" + paymentAttempt.getId() + ":authorized";
                    Instant now = Instant.now();

                    OutboxEvent outboxEvent = OutboxEvent.builder()
                            .idempotencyKey(key)
                            .eventType(OutboxEventType.PAYMENT_ATTEMPT_AUTHORIZED.name())
                            .payload(toJson(node))
                            .occurredAt(now)
                            .nextAttemptAt(now)
                            .attemptCount(0)
                            .build();

                    try {
                        outboxEventRepository.save(outboxEvent);
                    } catch (DataIntegrityViolationException ex) {
                        log.warn("payment_intent.amount_capturable_updated Outbox event (PAYMENT_ATTEMPT_AUTHORIZED) already exists: idempotencyKey={}", key);
                    }
                }
            }

            //  captured
            //  v1 - capture payment as long as it's not in CAPTURED status
            //  TODO: v2 - do not capture payment if payment is CANCELED, issue a refund, update payment to REFUNDED and publish PaymentRefunded event
            case "payment_intent.succeeded" -> {
                if (!(obj instanceof PaymentIntent paymentIntent)) {
                    log.warn("payment_intent.succeeded eventId={}: unexpected data.object type {}", eventId, obj.getClass().getName());
                    return;
                }

                String paymentIntentId = paymentIntent.getId();

                PaymentAttempt paymentAttempt = resolvePaymentAttemptFromPaymentIntent("payment_intent.succeeded", eventId, paymentIntent);

                if (paymentAttempt == null) {
                    return;
                }

                UUID paymentAttemptId = paymentAttempt.getId();

                paymentAttemptRepository.setStatusUnless(paymentAttemptId, PaymentAttemptStatus.CAPTURED, PaymentAttemptStatus.CAPTURED);

                //  *** even though updatedRows can be 0, we must ensure the payment is updated to CAPTURED (idempotency) and all the other live attempts must be killed to avoid user being charged multiple times ***

                Payment payment = paymentRepository.findById(paymentAttempt.getPaymentId()).orElse(null);

                if (payment == null) {
                    log.warn("payment_intent.succeeded eventId={} paymentIntentId={}: payment not found - paymentId={}", eventId, paymentIntentId, paymentAttempt.getPaymentId());
                    return;
                }

                //  force update payment status to CAPTURED
                int updatedPaymentRows = paymentRepository.setStatusUnless(payment.getId(), PaymentStatus.CAPTURED, PaymentStatus.CAPTURED);

                ObjectNode node = objectMapper.createObjectNode();
                node.put("paymentId", payment.getId().toString());
                node.put("paymentAttemptId", paymentAttemptId.toString());
                node.put("orderId", payment.getOrderId().toString());

                String key = "payment:" + payment.getId() + ":captured";
                Instant now = Instant.now();

                OutboxEvent outboxEvent = OutboxEvent.builder()
                        .idempotencyKey(key)
                        .eventType(OutboxEventType.PAYMENT_CAPTURED.name())
                        .payload(toJson(node))
                        .attemptCount(0)
                        .occurredAt(now)
                        .nextAttemptAt(now)
                        .build();
                try {
                    outboxEventRepository.save(outboxEvent);
                } catch (DataIntegrityViolationException ex) {
                    log.warn("payment_intent.succeeded Outbox event (PAYMENT_CAPTURED) already exists: idempotencyKey={}", key);
                }


                //  point payment's current attempt to the captured attempt if they are not the same
                if (payment.getCurrentAttemptId() == null || !paymentAttemptId.equals(payment.getCurrentAttemptId())) {
                    paymentRepository.updateCurrentAttemptId(payment.getId(), paymentAttemptId);
                }

                //  get all the attempts of the payment, cancel the payment intents of any non-terminal attempts
                List<PaymentAttemptStatus> terminalStatuses = PaymentAttemptStatus.terminalStatuses();

                List<PaymentAttempt> nonTerminalPaymentAttempts = paymentAttemptRepository.getAllNonTerminalPaymentAttemptsExcept(
                        payment.getId(), paymentAttemptId, terminalStatuses);

                for (PaymentAttempt pa : nonTerminalPaymentAttempts) {
                    if (pa.getCheckoutSessionId() == null) {
                        log.warn("payment_intent.succeeded eventId={} paymentId={} attemptId={} has no checkoutSessionId",
                                eventId, payment.getId(), pa.getId());
                        continue;
                    }

                    CheckoutSessionExpireTask task = CheckoutSessionExpireTask.builder()
                            .paymentId(pa.getPaymentId())
                            .paymentAttemptId(pa.getId())
                            .checkoutSessionId(pa.getCheckoutSessionId())
                            .attemptCount(0)
                            .nextAttemptAt(now)
                            .build();

                    try {
                        checkoutSessionExpireTaskRepository.save(task);
                    } catch (DataIntegrityViolationException ex) {
                        log.debug("payment_intent.succeeded eventId={} Payment attempt id={} has already been saved in checkout_session_expire_tasks", eventId, pa.getId());
                    }
                }
            }
            //  failed
            case "payment_intent.payment_failed" -> {
                if (!(obj instanceof PaymentIntent paymentIntent)) {
                    log.warn("payment_intent.payment_failed eventId={}: unexpected data.object type {}", eventId, obj.getClass().getName());
                    return;
                }

                String paymentIntentId = paymentIntent.getId();

                PaymentAttempt paymentAttempt = resolvePaymentAttemptFromPaymentIntent("payment_intent.payment_failed", eventId, paymentIntent);

                if (paymentAttempt == null) {
                    return;
                }

                UUID paymentAttemptId = paymentAttempt.getId();

                int updatedRows = paymentAttemptRepository.failNonTerminalPaymentAttempt(paymentAttemptId, PaymentAttemptStatus.FAILED, PaymentAttemptStatus.terminalStatuses());

                //  if no updated rows, re-fetch to know the real DB status
                if (updatedRows == 0) {
                    PaymentAttempt fresh = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);
                    if (fresh == null) {
                        log.warn("payment_intent.payment_failed eventId={} paymentIntentId={}: paymentAttempt disappeared - id={}",
                                eventId, paymentIntentId, paymentAttemptId);
                        return;
                    }

                    // Not failed => terminal but NOT failed (CANCELED/CAPTURED), so don't publish failed event.
                    if (fresh.getPaymentAttemptStatus() != PaymentAttemptStatus.FAILED) {
                        return;
                    }

                    paymentAttempt = fresh;
                }

                Payment payment = paymentRepository.findById(paymentAttempt.getPaymentId()).orElse(null);

                if (payment == null) {
                    log.warn("payment_intent.payment_failed eventId={} paymentIntentId={}: payment not found - paymentId={}", eventId, paymentIntentId, paymentAttempt.getPaymentId());
                    return;
                }

                if (payment.getCurrentAttemptId() == null) {
                    log.warn("payment_intent.payment_failed eventId={} paymentIntentId={}: payment.currentAttemptId is null - paymentId={}", eventId, paymentIntentId, payment.getId());
                    return;
                }

                //  *** ONLY publish PaymentFailed event if: ***
                //  1) paymentAttempt.paymentAttemptStatus is updated (from non-terminal status to FAILED)
                //  2) paymentAttempt.id == payment.currentAttemptId
                //  3) payment.paymentStatus is NOT terminal (CAPTURED/CANCELED/FAILED)
                if (paymentAttemptId.equals(payment.getCurrentAttemptId()) && !payment.getPaymentStatus().isTerminal()) {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("paymentId", payment.getId().toString());
                    node.put("paymentAttemptId", paymentAttemptId.toString());
                    node.put("orderId", payment.getOrderId().toString());

                    String key = "payment:" + payment.getId() + ":attempt:" + paymentAttemptId + ":failed";
                    Instant now = Instant.now();

                    OutboxEvent outboxEvent = OutboxEvent.builder()
                            .idempotencyKey(key)
                            .eventType(OutboxEventType.PAYMENT_ATTEMPT_FAILED.name())
                            .payload(toJson(node))
                            .occurredAt(now)
                            .nextAttemptAt(now)
                            .attemptCount(0)
                            .build();

                    try {
                        outboxEventRepository.save(outboxEvent);
                    } catch (DataIntegrityViolationException ex) {
                        log.warn("payment_intent.payment_failed Outbox event (PAYMENT_ATTEMPT_FAILED) already exists: idempotencyKey={}", key);
                    }
                }
            }
            //  cancel
            //  TODO: update needed if new PaymentAttemptStatus are added in later versions
            case "payment_intent.canceled" -> {
                if (!(obj instanceof PaymentIntent paymentIntent)) {
                    log.warn("payment_intent.canceled eventId={}: unexpected data.object type {}", eventId, obj.getClass().getName());
                    return;
                }

                String paymentIntentId = paymentIntent.getId();

                PaymentAttempt paymentAttempt = resolvePaymentAttemptFromPaymentIntent("payment_intent.canceled", eventId, paymentIntent);

                if (paymentAttempt == null) {
                    return;
                }

                UUID paymentAttemptId = paymentAttempt.getId();

                int updatedRows = paymentAttemptRepository.cancelNonTerminalPaymentAttempt(paymentAttemptId, PaymentAttemptStatus.CANCELED, PaymentAttemptStatus.terminalStatuses());

                //  if no updated rows, re-fetch to know the real DB status
                if (updatedRows == 0) {
                    PaymentAttempt fresh = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);
                    if (fresh == null) {
                        log.warn("payment_intent.canceled eventId={} paymentIntentId={}: paymentAttempt disappeared - id={}",
                                eventId, paymentIntentId, paymentAttemptId);
                        return;
                    }

                    // Not canceled => terminal but NOT canceled (FAILED/CAPTURED), so don't publish canceled event.
                    if (fresh.getPaymentAttemptStatus() != PaymentAttemptStatus.CANCELED) {
                        return;
                    }
                    paymentAttempt = fresh;
                }

                Payment payment = paymentRepository.findById(paymentAttempt.getPaymentId()).orElse(null);

                if (payment == null) {
                    log.warn("payment_intent.canceled eventId={} paymentIntentId={}: payment not found - paymentId={}", eventId, paymentIntentId, paymentAttempt.getPaymentId());
                    return;
                }

                if (payment.getCurrentAttemptId() == null) {
                    log.warn("payment_intent.canceled eventId={} paymentIntentId={}: payment.currentAttemptId is null - paymentId={}", eventId, paymentIntentId, payment.getId());
                    return;
                }

                //  *** ONLY publish PaymentCanceled event if: ***
                //  1) paymentAttempt.id == payment.currentAttemptId
                //  2) if paymentStatus is still PENDING (payable)

                if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
                    return;
                }

                if (paymentAttemptId.equals(payment.getCurrentAttemptId())) {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("paymentId", payment.getId().toString());
                    node.put("paymentAttemptId", paymentAttemptId.toString());
                    node.put("orderId", payment.getOrderId().toString());

                    String key = "payment:" + payment.getId() + ":attempt:" + paymentAttemptId + ":canceled";
                    Instant now = Instant.now();

                    OutboxEvent outboxEvent = OutboxEvent.builder()
                            .idempotencyKey(key)
                            .eventType(OutboxEventType.PAYMENT_ATTEMPT_CANCELED.name())
                            .payload(toJson(node))
                            .occurredAt(now)
                            .nextAttemptAt(now)
                            .attemptCount(0)
                            .build();

                    try {
                        outboxEventRepository.save(outboxEvent);
                    } catch (DataIntegrityViolationException ex) {
                        log.warn("payment_intent.canceled Outbox event (PAYMENT_ATTEMPT_CANCELED) already exists: idempotencyKey={}", key);
                    }
                }
            }
            //  checkout session expired
            case "checkout.session.expired" -> {
                if (!(obj instanceof Session session)) {
                    log.warn("checkout.session.expired eventId={}: unexpected data.object type {}", eventId, obj.getClass().getName());
                    return;
                }

                String sessionId = session.getId();

                if (sessionId == null) {
                    log.warn("checkout.session.expired eventId={}: sessionId missing", eventId);
                    return;
                }

                PaymentAttempt paymentAttempt = paymentAttemptRepository.findByCheckoutSessionId(sessionId).orElse(null);

                if (paymentAttempt == null) {
                    log.warn("checkout.session.expired eventId={} sessionId={}: payment attempt not found", eventId, sessionId);
                    return;
                }

                UUID paymentAttemptId = paymentAttempt.getId();

                int updatedRows = paymentAttemptRepository.cancelNonTerminalPaymentAttempt(paymentAttemptId, PaymentAttemptStatus.CANCELED, PaymentAttemptStatus.terminalStatuses());

                //  if no updated rows, re-fetch to know the real DB status
                if (updatedRows == 0) {
                    PaymentAttempt fresh = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);
                    if (fresh == null) {
                        log.warn("checkout.session.expired eventId={} sessionId={}: paymentAttempt disappeared - id={}",
                                eventId, sessionId, paymentAttemptId);
                        return;
                    }

                    // Not canceled => terminal but NOT canceled (FAILED/CAPTURED), so don't publish canceled event.
                    if (fresh.getPaymentAttemptStatus() != PaymentAttemptStatus.CANCELED) {
                        return;
                    }
                    paymentAttempt = fresh;
                }

                Payment payment = paymentRepository.findById(paymentAttempt.getPaymentId()).orElse(null);

                if (payment == null) {
                    log.warn("checkout.session.expired eventId={} sessionId={}: payment not found - paymentId={}", eventId, sessionId, paymentAttempt.getPaymentId());
                    return;
                }

                if (payment.getCurrentAttemptId() != null
                        && paymentAttemptId.equals(payment.getCurrentAttemptId())
                        && payment.getPaymentStatus() == PaymentStatus.PENDING) {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("paymentId", payment.getId().toString());
                    node.put("paymentAttemptId", paymentAttemptId.toString());
                    node.put("orderId", payment.getOrderId().toString());

                    String key = "payment:" + payment.getId() + ":attempt:" + paymentAttemptId + ":canceled";
                    Instant now = Instant.now();

                    OutboxEvent outboxEvent = OutboxEvent.builder()
                            .idempotencyKey(key)
                            .eventType(OutboxEventType.PAYMENT_ATTEMPT_CANCELED.name())
                            .payload(toJson(node))
                            .occurredAt(now)
                            .nextAttemptAt(now)
                            .attemptCount(0)
                            .build();

                    try {
                        outboxEventRepository.save(outboxEvent);
                    } catch (DataIntegrityViolationException ex) {
                        log.warn("checkout.session.expired Outbox event (PAYMENT_ATTEMPT_CANCELED) already exists: idempotencyKey={}", key);
                    }
                }

                //  update CheckoutSessionExpireTask
                CheckoutSessionExpireTask checkoutSessionExpireTask = checkoutSessionExpireTaskRepository.findByCheckoutSessionId(sessionId).orElse(null);

                if (checkoutSessionExpireTask == null) {
                    log.debug("checkout.session.expired No CheckoutSessionExpireTask for Checkout Session id={}", sessionId);
                    return;
                }

                int taskUpdatedRows = checkoutSessionExpireTaskRepository.updateTaskCompletedAt(checkoutSessionExpireTask.getId(), Instant.now());

                if (taskUpdatedRows == 0) {
                    log.debug("checkout.session.expired CheckoutSessionExpireTask already completed: id={} sessionId={}", checkoutSessionExpireTask.getId(), sessionId);
                }
            }

            //  ignore unneeded event type
            default -> {
                log.debug("Ignoring unsupported Stripe webhook event type={} eventId={}", type, eventId);
            }
        }
    }

    private String toJson(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new BadWebhookPayloadException("Failed to serialize outbox payload", e);
        }
    }

    private PaymentAttempt resolvePaymentAttemptFromPaymentIntent(String eventType, String eventId, PaymentIntent paymentIntent) {
        String paymentIntentId = paymentIntent.getId();

        //  attempt already correlated by payment_intent_id
        PaymentAttempt attempt = paymentAttemptRepository.findByPaymentIntentId(paymentIntentId).orElse(null);
        if (attempt != null) {
            return attempt;
        }

        String metaDataAttemptId = null;
        try {
            metaDataAttemptId = paymentIntent.getMetadata().get("paymentAttemptId");
        } catch (Exception ignore) {
            //  defensive: Stripe should not throw here, but we never want webhook processing to 500 because of metadata access
        }

        if (metaDataAttemptId == null || metaDataAttemptId.isBlank()) {
            log.warn("{} eventId={} paymentIntentId={}: cannot correlate (no metadata.paymentAttemptId)", eventType, eventId, paymentIntentId);
            return null;
        }

        UUID attemptId;
        try {
            attemptId = UUID.fromString(metaDataAttemptId);
        } catch (IllegalArgumentException ex) {
            log.warn("{} eventId={} paymentIntentId={}: invalid metadata.paymentAttemptId='{}'", eventType, eventId, paymentIntentId, metaDataAttemptId);
            return null;
        }

        attempt = paymentAttemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) {
            log.warn("{} eventId={} paymentIntentId={}: payment attempt not found by metadata.paymentAttemptId={}", eventType, eventId, paymentIntentId, attemptId);
            return null;
        }

        //  backfill payment_intent_id on the attempt, but never overwrite a different existing value
        String existingPaymentIntentId = attempt.getPaymentIntentId();
        if (existingPaymentIntentId == null || existingPaymentIntentId.isBlank()) {
            //  TODO
            int updatedRow = paymentService.updatePaymentAttemptPaymentIntentId(attemptId, paymentIntentId);
            if (updatedRow == 1) {
                // Our guarded backfill succeeded.
                attempt.setPaymentIntentId(paymentIntentId);
                return attempt;
            }

            // Another thread/webhook may already have filled it. Re-fetch and inspect final DB state.
            PaymentAttempt fresh = paymentAttemptRepository.findById(attemptId).orElse(null);
            if (fresh == null) {
                log.warn("{} eventId={} paymentIntentId={}: paymentAttempt disappeared after guarded backfill - id={}",
                        eventType, eventId, paymentIntentId, attemptId);
                return null;
            }

            String freshPaymentIntentId = fresh.getPaymentIntentId();
            if (freshPaymentIntentId == null || freshPaymentIntentId.isBlank()) {
                log.warn("{} eventId={} paymentIntentId={}: guarded backfill updatedRows=0 but paymentAttempt still has null paymentIntentId - id={}",
                        eventType, eventId, paymentIntentId, attemptId);
                return null;
            }

            if (!freshPaymentIntentId.equals(paymentIntentId)) {
                log.warn("{} eventId={} paymentIntentId={}: metadata maps to attemptId={}, but DB now has different paymentIntentId={}",
                        eventType, eventId, paymentIntentId, attemptId, freshPaymentIntentId);
                return null;
            }

            return fresh;
        }

        if (!existingPaymentIntentId.equals(paymentIntentId)
        ) {
            log.warn("{} eventId={} paymentIntentId={}: metadata maps to attemptId={}, but attempt already has paymentIntentId={}", eventType, eventId, paymentIntentId, attemptId, existingPaymentIntentId);
            return null;
        }

        return attempt;
    }
}
