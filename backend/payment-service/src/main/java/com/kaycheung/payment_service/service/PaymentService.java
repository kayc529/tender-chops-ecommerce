package com.kaycheung.payment_service.service;

import com.kaycheung.payment_service.dto.CreatePaymentRequest;
import com.kaycheung.payment_service.dto.CreatePaymentResponse;
import com.kaycheung.payment_service.dto.GetPaymentResponse;
import com.kaycheung.payment_service.dto.PaymentRetryNotAllowedReason;
import com.kaycheung.payment_service.entity.Payment;
import com.kaycheung.payment_service.entity.PaymentAttempt;
import com.kaycheung.payment_service.entity.PaymentAttemptStatus;
import com.kaycheung.payment_service.entity.PaymentStatus;
import com.kaycheung.payment_service.exception.PaymentApiException;
import com.kaycheung.payment_service.exception.code.PaymentErrorCode;
import com.kaycheung.payment_service.repository.PaymentAttemptRepository;
import com.kaycheung.payment_service.repository.PaymentRepository;
import com.kaycheung.payment_service.util.SecurityUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final CreatePaymentPersistService createPaymentPersistService;
    private final RetryPaymentPersistService retryPaymentPersistService;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;

    public GetPaymentResponse getPayment(UUID paymentId) {
        UUID userId = SecurityUtil.getCurrentUserIdUUID();

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            throw new PaymentApiException(
                    HttpStatus.NOT_FOUND,
                    PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "Payment not found: paymentId=" + paymentId
            );
        }

        if (!userId.equals(payment.getUserId())) {
            throw new PaymentApiException(
                    HttpStatus.FORBIDDEN,
                    PaymentErrorCode.PAYMENT_UNAUTHORIZED,
                    "Access denied"
            );
        }

        UUID currentAttemptId = payment.getCurrentAttemptId();
        PaymentAttemptStatus currentAttemptStatus = null;

        if (currentAttemptId != null) {
            PaymentAttempt attempt = paymentAttemptRepository.findById(currentAttemptId).orElse(null);
            if (attempt == null) {
                throw new PaymentApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                        "Payment has currentAttemptId but attempt row is missing: paymentId=" + payment.getId() + ", currentAttemptId=" + currentAttemptId
                );
            }
            currentAttemptStatus = attempt.getPaymentAttemptStatus();
        }

        // Retry rules (v1):
        // - Never retry if payment already captured
        // - If payment is terminal (e.g., order canceled), do not retry
        // - Retry allowed only when current attempt is FAILED or CANCELED
        boolean canRetry;
        PaymentRetryNotAllowedReason reason;

        if (payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
            canRetry = false;
            reason = PaymentRetryNotAllowedReason.ALREADY_PAID;
        } else if (payment.getPaymentStatus().isTerminal()) {
            canRetry = false;
            // Keep this generic unless there is a more specific enum value
            reason = PaymentRetryNotAllowedReason.PAYMENT_CANCELED;
        } else {
            // Payment is non-terminal, decide based on current attempt
            if (currentAttemptStatus == null) {
                // Should not happen for normal payments; treat as inconsistent
                throw new PaymentApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                        "Payment missing current attempt while non-terminal: paymentId=" + payment.getId()
                );
            }

            if (currentAttemptStatus == PaymentAttemptStatus.FAILED || currentAttemptStatus == PaymentAttemptStatus.CANCELED) {
                canRetry = true;
                reason = null;
            } else {
                canRetry = false;
                reason = PaymentRetryNotAllowedReason.PAYMENT_PROCESSING;
            }
        }

        return new GetPaymentResponse(
                payment.getId(),
                payment.getCurrentAttemptId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentStatus(),
                currentAttemptStatus,
                canRetry,
                reason,
                payment.getUpdatedAt()
        );
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request, UUID userId) {
        CreatePaymentPersistService.PersistedPaymentAndPaymentAttempt persisted = createPaymentPersistService.createPaymentAndPaymentAttempt(request, userId);

        Payment payment = persisted.payment();
        PaymentAttempt paymentAttempt = persisted.paymentAttempt();

        if (persisted.alreadyExists()) {
            if (!userId.equals(payment.getUserId())) {
                throw new PaymentApiException(
                        HttpStatus.FORBIDDEN,
                        PaymentErrorCode.PAYMENT_UNAUTHORIZED,
                        "Access denied"
                );
            }
            log.debug("Existing payment={}", payment);
            return handleExistingPayment(payment, request);
        }

        if (paymentAttempt == null) {
            // Should never happen when alreadyExists == false
            String debugMessage = "paymentAttempt is null for newly-created payment: paymentId=" + payment.getId();
            throw new PaymentApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                    debugMessage
            );
        }

        //  create Checkout Session with Stripe SDK
        //  any Stripe error -> throw customized Payment error
        Session checkoutSession;
        try {
            SessionCreateParams.LineItem item = SessionCreateParams.LineItem.builder()
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(request.currency())
                                    .setUnitAmount(request.amount())
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName("Order " + request.orderId())
                                                    .setDescription("Complete order")
                                                    .build()
                                    )
                                    .build()
                    )
                    .setQuantity(1L)
                    .build();

            String successfulUrl = "https://localhost:3000/pending-payment/" + payment.getId();
            String cancelUrl = "https://localhost:3000/orders";

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(item)
                    .setSuccessUrl(successfulUrl)
                    .setCancelUrl(cancelUrl)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setCustomerEmail("kurok1132@gmail.com")
                    .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                            .setCaptureMethod(SessionCreateParams.PaymentIntentData.CaptureMethod.MANUAL)
                            //  METADATA
                            .putMetadata("paymentAttemptId", paymentAttempt.getId().toString())
                            .putMetadata("paymentId", payment.getId().toString())
                            .putMetadata("orderId", payment.getOrderId().toString())
                            .build())
                    //  METADATA
                    .putMetadata("paymentAttemptId", paymentAttempt.getId().toString())
                    .putMetadata("paymentId", payment.getId().toString())
                    .putMetadata("orderId", payment.getOrderId().toString())
                    .build();

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(paymentAttempt.getIdempotencyKey())
                    .build();

            checkoutSession = Session.create(params, requestOptions);

        } catch (StripeException ex) {
            String debugMessage = "Stripe error while creating checkout session for paymentId="
                    + payment.getId() + ", attemptId=" + paymentAttempt.getId();

            log.debug("Stripe Exception={}, error={}", debugMessage, ex.getMessage());

            throw new PaymentApiException(
                    HttpStatus.BAD_GATEWAY,
                    PaymentErrorCode.STRIPE_API_ERROR,
                    debugMessage,
                    ex
            );
        }

        //  save checkoutSessionId and redirectUrl of payment attempt
        String checkoutSessionId = checkoutSession.getId();
        String redirectUrl = checkoutSession.getUrl();

        if (redirectUrl == null || redirectUrl.isBlank()) {
            String debugMessage = "Stripe Checkout Session has no url: sessionId=" + checkoutSessionId
                    + ", paymentId=" + payment.getId()
                    + ", attemptId=" + paymentAttempt.getId();
            throw new PaymentApiException(
                    HttpStatus.BAD_GATEWAY,
                    PaymentErrorCode.STRIPE_SESSION_INVALID,
                    debugMessage
            );
        }

        String paymentIntentId = checkoutSession.getPaymentIntent();

        createPaymentPersistService.updateCheckoutSessionAndRedirectUrl(paymentAttempt.getId(), checkoutSessionId, redirectUrl, paymentIntentId);

        return new CreatePaymentResponse(payment.getId(), paymentAttempt.getId(), payment.getOrderId(), paymentAttempt.getAttemptNo(), redirectUrl, checkoutSessionId);
    }

    public CreatePaymentResponse retryPayment(UUID paymentId) {
        UUID userId = SecurityUtil.getCurrentUserIdUUID();

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            throw new PaymentApiException(
                    HttpStatus.NOT_FOUND,
                    PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "Payment not found: paymentId=" + paymentId
            );
        }

        if (!userId.equals(payment.getUserId())) {
            throw new PaymentApiException(
                    HttpStatus.FORBIDDEN,
                    PaymentErrorCode.PAYMENT_UNAUTHORIZED,
                    "Access denied"
            );
        }

        //  Payment-level guards
        if (payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_NOT_PAYABLE,
                    "Payment already captured: paymentId=" + payment.getId() + ", orderId=" + payment.getOrderId()
            );
        }

        if (payment.getPaymentStatus().isTerminal()) {
            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_NOT_PAYABLE,
                    "Payment not payable due to terminal status=" + payment.getPaymentStatus() + ": paymentId=" + payment.getId() + ", orderId=" + payment.getOrderId()
            );
        }

        UUID currentAttemptId = payment.getCurrentAttemptId();
        if (currentAttemptId == null) {
            throw new PaymentApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                    "Payment missing currentAttemptId: paymentId=" + payment.getId() + ", orderId=" + payment.getOrderId()
            );
        }

        PaymentAttempt currentAttempt = paymentAttemptRepository.findById(currentAttemptId).orElse(null);
        if (currentAttempt == null) {
            throw new PaymentApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                    "Payment has currentAttemptId but attempt row is missing: paymentId=" + payment.getId() + ", currentAttemptId=" + currentAttemptId
            );
        }

        //  Attempt-level guards (v1 - only check payment/attempt state)
        //  TODO check with order-service for latest order status
        if (!currentAttempt.getPaymentAttemptStatus().isTerminal()) {
            // AUTH_PENDING / AUTHORIZED -> processing; do not allow retry
            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_ALLOWED,
                    "Retry not allowed while current attempt is processing: paymentId=" + payment.getId()
                            + ", currentAttemptId=" + currentAttempt.getId()
                            + ", status=" + currentAttempt.getPaymentAttemptStatus()
            );
        }

        if (currentAttempt.getPaymentAttemptStatus() == PaymentAttemptStatus.CAPTURED) {
            //  Defensive - even though payment status should be CAPTURED too
            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_NOT_PAYABLE,
                    "Retry not allowed because current attempt is already CAPTURED: paymentId=" + payment.getId() + ", attemptId=" + currentAttempt.getId()
            );
        }

        //  Only allow retry when current attempt is FAILED or CANCELED
        if (!(currentAttempt.getPaymentAttemptStatus() == PaymentAttemptStatus.FAILED
                || currentAttempt.getPaymentAttemptStatus() == PaymentAttemptStatus.CANCELED)) {
            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_ALLOWED,
                    "Retry not allowed for current attempt status=" + currentAttempt.getPaymentAttemptStatus()
                            + ": paymentId=" + payment.getId() + ", attemptId=" + currentAttempt.getId()
            );
        }

        //  Create next attempt (TX1)
        int nextAttemptNo = currentAttempt.getAttemptNo() + 1;
        String idempotencyKey = "order:" + payment.getOrderId() + ":attempt:" + nextAttemptNo;

        RetryPaymentPersistService.RetryTx1Result retryTx1Result = retryPaymentPersistService.createNextPaymentAttempt(payment, nextAttemptNo, idempotencyKey);

        if (retryTx1Result.shouldReturnImmediately()) {
            return retryTx1Result.immediateResponse();
        }

        payment = retryTx1Result.payment();
        PaymentAttempt newAttempt = retryTx1Result.paymentAttempt();

        // Create Stripe Checkout Session (network call outside DB consistency assumptions)
        Session checkoutSession;
        try {
            SessionCreateParams.LineItem item = SessionCreateParams.LineItem.builder()
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(payment.getCurrency())
                                    .setUnitAmount(payment.getAmount())
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName("Order " + payment.getOrderId())
                                                    .setDescription("Complete order")
                                                    .build()
                                    )
                                    .build()
                    )
                    .setQuantity(1L)
                    .build();

            String successfulUrl = "https://localhost:3000/pending-payment/" + payment.getId();
            String cancelUrl = "https://localhost:3000/orders";

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(item)
                    .setSuccessUrl(successfulUrl)
                    .setCancelUrl(cancelUrl)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setCustomerEmail("kurok1132@gmail.com")
                    .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                            .setCaptureMethod(SessionCreateParams.PaymentIntentData.CaptureMethod.MANUAL)
                            //  METADATA
                            .putMetadata("paymentAttemptId", newAttempt.getId().toString())
                            .putMetadata("paymentId", payment.getId().toString())
                            .putMetadata("orderId", payment.getOrderId().toString())
                            .build())
                    //  METADATA
                    .putMetadata("paymentAttemptId", newAttempt.getId().toString())
                    .putMetadata("paymentId", payment.getId().toString())
                    .putMetadata("orderId", payment.getOrderId().toString())
                    .build();

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(newAttempt.getIdempotencyKey())
                    .build();

            checkoutSession = Session.create(params, requestOptions);
        } catch (StripeException ex) {
            // Mark this attempt FAILED so the user can retry again (TX2)
            try {
                retryPaymentPersistService.markAttemptFailed(newAttempt.getId());
            } catch (Exception ignored) {
                // best-effort; do not mask Stripe error
            }

            String debugMessage = "Stripe error while creating checkout session for retry: paymentId="
                    + payment.getId() + ", attemptId=" + newAttempt.getId();
            throw new PaymentApiException(
                    HttpStatus.BAD_GATEWAY,
                    PaymentErrorCode.STRIPE_API_ERROR,
                    debugMessage,
                    ex
            );
        }

        String checkoutSessionId = checkoutSession.getId();
        String redirectUrl = checkoutSession.getUrl();
        String paymentIntentId = checkoutSession.getPaymentIntent();

        if (redirectUrl == null || redirectUrl.isBlank()) {
            // Mark attempt FAILED so it is retryable
            try {
                retryPaymentPersistService.markAttemptFailed(newAttempt.getId());
            } catch (Exception ignored) {
            }

            String debugMessage = "Stripe Checkout Session has no url for retry: sessionId=" + checkoutSessionId
                    + ", paymentId=" + payment.getId()
                    + ", attemptId=" + newAttempt.getId();
            throw new PaymentApiException(
                    HttpStatus.BAD_GATEWAY,
                    PaymentErrorCode.STRIPE_SESSION_INVALID,
                    debugMessage
            );
        }

        retryPaymentPersistService.updateCheckoutSessionAndRedirectUrl(newAttempt.getId(), checkoutSessionId, redirectUrl, paymentIntentId);

        return new CreatePaymentResponse(
                payment.getId(),
                newAttempt.getId(),
                payment.getOrderId(),
                newAttempt.getAttemptNo(),
                redirectUrl,
                checkoutSessionId
        );
    }

    public CreatePaymentResponse retryPaymentByOrderId(UUID orderId) {
        UUID userId = SecurityUtil.getCurrentUserIdUUID();

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            throw new PaymentApiException(
                    HttpStatus.NOT_FOUND,
                    PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "Payment not found for orderId=" + orderId
            );
        }

        if (!userId.equals(payment.getUserId())) {
            throw new PaymentApiException(
                    HttpStatus.FORBIDDEN,
                    PaymentErrorCode.PAYMENT_UNAUTHORIZED,
                    "Access denied"
            );
        }

        return retryPayment(payment.getId());
    }

    @Transactional
    public int cancelPayment(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new PaymentApiException(HttpStatus.NOT_FOUND, PaymentErrorCode.PAYMENT_NOT_FOUND, "Payment not found for orderId=" + orderId));

        return paymentRepository.transitionStatus(payment.getId(), PaymentStatus.PENDING, PaymentStatus.CANCELED);
    }

    @Transactional
    public int cancelPaymentForDoNotCapture(UUID orderId, UUID paymentId, UUID paymentAttemptId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Skip payment->CANCELED on ORDER_DO_NOT_CAPTURE: payment not found. orderId={} paymentId={} paymentAttemptId={}",
                    orderId, paymentId, paymentAttemptId);
            return 0;
        }

        //  poison
        if (!orderId.equals(payment.getOrderId())) {
            throw new IllegalArgumentException(
                    "ORDER_DO_NOT_CAPTURE payload mismatch: paymentId=" + paymentId
                            + " belongs to orderId=" + payment.getOrderId()
                            + ", but payload.orderId=" + orderId
            );
        }

        PaymentAttempt paymentAttempt = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);
        if (paymentAttempt == null) {
            log.warn("Skip payment->CANCELED on ORDER_DO_NOT_CAPTURE: paymentAttempt not found. orderId={} paymentId={} paymentAttemptId={}",
                    orderId, paymentId, paymentAttemptId);
            return 0;
        }

        //  poison
        if (!payment.getId().equals(paymentAttempt.getPaymentId())) {
            throw new IllegalArgumentException(
                    "ORDER_DO_NOT_CAPTURE payload mismatch: paymentAttemptId=" + paymentAttemptId
                            + " belongs to paymentId=" + paymentAttempt.getPaymentId()
                            + ", but payload.paymentId=" + paymentId
            );
        }

        if (payment.getCurrentAttemptId() == null) {
            log.warn("Skip payment->CANCELED on ORDER_DO_NOT_CAPTURE: payment has null currentAttemptId. orderId={} paymentId={} paymentAttemptId={}",
                    orderId, paymentId, paymentAttemptId);
            return 0;
        }

        if (!paymentAttemptId.equals(payment.getCurrentAttemptId())) {
            log.info("Skip payment->CANCELED on ORDER_DO_NOT_CAPTURE: paymentAttempt is not current attempt. orderId={} paymentId={} paymentAttemptId={} currentAttemptId={}",
                    orderId, paymentId, paymentAttemptId, payment.getCurrentAttemptId());
            return 0;
        }

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            log.info("Skip payment->CANCELED on ORDER_DO_NOT_CAPTURE: payment not in PENDING state. orderId={} paymentId={} paymentAttemptId={} paymentStatus={}",
                    orderId, paymentId, paymentAttemptId, payment.getPaymentStatus());
            return 0;
        }

        if (paymentAttempt.getPaymentAttemptStatus() != PaymentAttemptStatus.AUTHORIZED) {
            log.info("Skip payment->CANCELED on ORDER_DO_NOT_CAPTURE: paymentAttempt not in AUTHORIZED state. orderId={} paymentId={} paymentAttemptId={} attemptStatus={}",
                    orderId, paymentId, paymentAttemptId, paymentAttempt.getPaymentAttemptStatus());
            return 0;
        }

        return paymentRepository.transitionStatus(payment.getId(), PaymentStatus.PENDING, PaymentStatus.CANCELED);
    }

    public void capturePaymentIntent(UUID orderId, UUID paymentId, UUID paymentAttemptId) {
        //  check payment + payment attempt's existence, status etc.
        //  make Stripe capture call with idempotency key
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Skip Stripe capture on ORDER_READY_TO_CAPTURE: payment not found. orderId={} paymentId={} paymentAttemptId={}",
                    orderId, paymentId, paymentAttemptId);
            return;
        }

        // poison: payload/payment mismatch
        if (!orderId.equals(payment.getOrderId())) {
            throw new IllegalArgumentException(
                    "ORDER_READY_TO_CAPTURE payload mismatch: paymentId=" + paymentId
                            + " belongs to orderId=" + payment.getOrderId()
                            + ", but payload.orderId=" + orderId
            );
        }

        PaymentAttempt paymentAttempt = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);
        if (paymentAttempt == null) {
            log.warn("Skip Stripe capture on ORDER_READY_TO_CAPTURE: paymentAttempt not found. orderId={} paymentId={} paymentAttemptId={}",
                    orderId, paymentId, paymentAttemptId);
            return;
        }

        // poison: payload/paymentAttempt mismatch
        if (!payment.getId().equals(paymentAttempt.getPaymentId())) {
            throw new IllegalArgumentException(
                    "ORDER_READY_TO_CAPTURE payload mismatch: paymentAttemptId=" + paymentAttemptId
                            + " belongs to paymentId=" + paymentAttempt.getPaymentId()
                            + ", but payload.paymentId=" + paymentId
            );
        }

        if (payment.getCurrentAttemptId() == null) {
            log.warn("Skip Stripe capture on ORDER_READY_TO_CAPTURE: payment has null currentAttemptId. orderId={} paymentId={} paymentAttemptId={}",
                    orderId, paymentId, paymentAttemptId);
            return;
        }

        if (!paymentAttemptId.equals(payment.getCurrentAttemptId())) {
            log.info("Skip Stripe capture on ORDER_READY_TO_CAPTURE: paymentAttempt is not current attempt. orderId={} paymentId={} paymentAttemptId={} currentAttemptId={}",
                    orderId, paymentId, paymentAttemptId, payment.getCurrentAttemptId());
            return;
        }

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            log.info("Skip Stripe capture on ORDER_READY_TO_CAPTURE: payment not in PENDING state. orderId={} paymentId={} paymentAttemptId={} paymentStatus={}",
                    orderId, paymentId, paymentAttemptId, payment.getPaymentStatus());
            return;
        }

        if (paymentAttempt.getPaymentAttemptStatus() != PaymentAttemptStatus.AUTHORIZED) {
            log.info("Skip Stripe capture on ORDER_READY_TO_CAPTURE: paymentAttempt not in AUTHORIZED state. orderId={} paymentId={} paymentAttemptId={} attemptStatus={}",
                    orderId, paymentId, paymentAttemptId, paymentAttempt.getPaymentAttemptStatus());
            return;
        }

        String paymentIntentId = paymentAttempt.getPaymentIntentId();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException(
                    "ORDER_READY_TO_CAPTURE inconsistent state: AUTHORIZED attempt missing paymentIntentId. orderId=" + orderId
                            + ", paymentId=" + paymentId
                            + ", paymentAttemptId=" + paymentAttemptId
            );
        }

        String stripeIdempotencyKey = "payment:" + paymentId + ":attempt:" + paymentAttemptId + ":capture";

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            //  check if the payment intent is capturable ->  prevents worker from retrying
            if (!"requires_capture".equals(paymentIntent.getStatus())) {
                log.info("Skip Stripe capture on ORDER_READY_TO_CAPTURE: PaymentIntent not capturable. orderId={} paymentId={} paymentAttemptId={} paymentIntentId={} status={}",
                        orderId, paymentId, paymentAttemptId, paymentIntentId, paymentIntent.getStatus());
                return;
            }

            PaymentIntentCaptureParams params = PaymentIntentCaptureParams.builder().build();
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(stripeIdempotencyKey)
                    .build();

            paymentIntent.capture(params, requestOptions);
        } catch (StripeException ex) {
            String debugMessage = "Stripe error while capturing paymentIntentId=" + paymentIntentId
                    + ", orderId=" + orderId
                    + ", paymentId=" + paymentId
                    + ", paymentAttemptId=" + paymentAttemptId;
            throw new RuntimeException(debugMessage, ex);
        }
    }

    @Transactional
    public int updatePaymentAttemptPaymentIntentId(UUID paymentAttemptId, String paymentIntentId){
        return paymentAttemptRepository.updatePaymentAttemptPaymentIntentIdIfNull(paymentAttemptId, paymentIntentId);
    }

    private CreatePaymentResponse handleExistingPayment(Payment existingPayment, CreatePaymentRequest request) {
        log.warn("handleExistingPayment");

        if (existingPayment.getPaymentStatus().isTerminal()) {
            String debugMessage = "Payment id=" + existingPayment.getId() + " has a terminal status (" + existingPayment.getPaymentStatus() + ")";
            throw new PaymentApiException(HttpStatus.CONFLICT, PaymentErrorCode.PAYMENT_NOT_PAYABLE, debugMessage);
        }

        if (!Objects.equals(existingPayment.getCurrency(), request.currency())
                || existingPayment.getAmount() != request.amount()) {

            String debugMessage = "Payment id=" + existingPayment.getId()
                    + " amount/currency mismatch. existingAmount=" + existingPayment.getAmount()
                    + ", requestAmount=" + request.amount()
                    + ", existingCurrency=" + existingPayment.getCurrency()
                    + ", requestCurrency=" + request.currency();

            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_AMOUNT_OR_CURRENCY_MISMATCH,
                    debugMessage
            );
        }

        if (existingPayment.getCurrentAttemptId() == null) {
            String debugMessage = "Payment id=" + existingPayment.getId() + " has null currentAttemptId";
            throw new PaymentApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                    debugMessage
            );
        }

        PaymentAttempt paymentAttempt = paymentAttemptRepository.findById(existingPayment.getCurrentAttemptId()).orElse(null);

        if (paymentAttempt == null) {
            String debugMessage = "Payment id=" + existingPayment.getId()
                    + " currentAttemptId=" + existingPayment.getCurrentAttemptId()
                    + " not found in DB";
            throw new PaymentApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_FOUND,
                    debugMessage
            );
        }

        if (paymentAttempt.getAttemptNo() != 1) {
            String debugMessage = "Payment id=" + existingPayment.getId()
                    + " has attemptNo=" + paymentAttempt.getAttemptNo()
                    + " but expected attemptNo=1 for initial createPayment";
            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_ALLOWED,
                    debugMessage
            );
        }

        if (paymentAttempt.getPaymentAttemptStatus().isTerminal()) {
            String debugMessage = "PaymentAttempt id=" + paymentAttempt.getId()
                    + " is terminal with status=" + paymentAttempt.getPaymentAttemptStatus();
            throw new PaymentApiException(
                    HttpStatus.CONFLICT,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_ALLOWED,
                    debugMessage
            );
        }

        if (paymentAttempt.getCheckoutSessionId() == null) {
            //  mark attempt as FAILED
            retryPaymentPersistService.markAttemptFailed(paymentAttempt.getId());

            String debugMessage = "PaymentAttempt id=" + paymentAttempt.getId()
                    + " has null checkoutSessionId";

            log.debug("handleExistingPayment: checkoutSessionId is null");

            throw new PaymentApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                    debugMessage
            );
        }

        if (paymentAttempt.getRedirectUrl() != null && !paymentAttempt.getRedirectUrl().isBlank()) {
            return new CreatePaymentResponse(existingPayment.getId(), paymentAttempt.getId(), existingPayment.getOrderId(), paymentAttempt.getAttemptNo(), paymentAttempt.getRedirectUrl(), paymentAttempt.getCheckoutSessionId());
        }

        Session existingSession;

        try {
            existingSession = Session.retrieve(paymentAttempt.getCheckoutSessionId());
        } catch (StripeException ex) {
            String debugMessage = "Stripe error while retrieving sessionId="
                    + paymentAttempt.getCheckoutSessionId()
                    + " for paymentAttemptId=" + paymentAttempt.getId();
            throw new PaymentApiException(
                    HttpStatus.BAD_GATEWAY,
                    PaymentErrorCode.STRIPE_API_ERROR,
                    debugMessage,
                    ex
            );
        }

        String redirectUrl = existingSession.getUrl();

        if (redirectUrl == null || redirectUrl.isBlank()) {
            //  session might be completed/expired; redirecting to Stripe checkout isn't useful
            return new CreatePaymentResponse(existingPayment.getId(), paymentAttempt.getId(), existingPayment.getOrderId(), paymentAttempt.getAttemptNo(), null, paymentAttempt.getCheckoutSessionId());
        }

        createPaymentPersistService.updateRedirectUrl(paymentAttempt.getId(), redirectUrl);

        return new CreatePaymentResponse(existingPayment.getId(), paymentAttempt.getId(), existingPayment.getOrderId(), paymentAttempt.getAttemptNo(), redirectUrl, paymentAttempt.getCheckoutSessionId());
    }
}
