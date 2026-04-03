package com.kaycheung.payment_service.service;

import com.kaycheung.payment_service.dto.CreatePaymentResponse;
import com.kaycheung.payment_service.entity.CheckoutSessionExpireTask;
import com.kaycheung.payment_service.entity.Payment;
import com.kaycheung.payment_service.entity.PaymentAttempt;
import com.kaycheung.payment_service.entity.PaymentAttemptStatus;
import com.kaycheung.payment_service.exception.PaymentApiException;
import com.kaycheung.payment_service.exception.code.PaymentErrorCode;
import com.kaycheung.payment_service.repository.CheckoutSessionExpireTaskRepository;
import com.kaycheung.payment_service.repository.PaymentAttemptRepository;
import com.kaycheung.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetryPaymentPersistService {
    private static final Logger log = LoggerFactory.getLogger(RetryPaymentPersistService.class);
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final CheckoutSessionExpireTaskRepository checkoutSessionExpireTaskRepository;

    @Transactional
    public RetryTx1Result createNextPaymentAttempt(Payment payment, int nextAttemptNo, String key){
        PaymentAttempt newAttempt = PaymentAttempt.builder()
                .paymentId(payment.getId())
                .attemptNo(nextAttemptNo)
                .paymentAttemptStatus(PaymentAttemptStatus.AUTH_PENDING)
                .idempotencyKey(key)
                .build();

        try {
            newAttempt = paymentAttemptRepository.save(newAttempt);
        } catch (DataIntegrityViolationException ex) {
            // Another request created this attemptNo already; return the existing one
            PaymentAttempt existing = paymentAttemptRepository.findByPaymentIdAndAttemptNo(payment.getId(), nextAttemptNo)
                    .orElseThrow(() -> new PaymentApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                            "Retry attempt unique constraint hit but cannot refetch: paymentId=" + payment.getId() + ", attemptNo=" + nextAttemptNo,
                            ex
                    ));

            // Point currentAttemptId to the already-created retry attempt
            paymentRepository.updateCurrentAttemptId(payment.getId(), existing.getId());

            // Best-effort: kill other live attempts even if this retry hit a duplicate-attempt race.
            List<PaymentAttemptStatus> terminalStatuses = PaymentAttemptStatus.terminalStatuses();
            List<PaymentAttempt> nonTerminalAttempts = paymentAttemptRepository.getAllNonTerminalPaymentAttemptsExcept(
                    payment.getId(),
                    existing.getId(),
                    terminalStatuses
            );

            Instant now = Instant.now();
            for (PaymentAttempt pa : nonTerminalAttempts) {
                if (pa.getCheckoutSessionId() == null || pa.getCheckoutSessionId().isBlank()) {
                    log.warn("retryPayment paymentId={} attemptId={} is non-terminal but has no checkoutSessionId; skipping expire task", payment.getId(), pa.getId());
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
                } catch (DataIntegrityViolationException ignored) {
                    log.warn("retryPayment paymentId={} attemptId={} already has checkout_session_expire_task", payment.getId(), pa.getId());
                }
            }

            CreatePaymentResponse immediateResponse = new CreatePaymentResponse(
                    payment.getId(),
                    existing.getId(),
                    payment.getOrderId(),
                    existing.getAttemptNo(),
                    existing.getRedirectUrl(),
                    existing.getCheckoutSessionId()
            );

            return new RetryTx1Result(payment, existing, true, immediateResponse);
        }

        // Point payment's currentAttemptId to new attempt
        paymentRepository.updateCurrentAttemptId(payment.getId(), newAttempt.getId());

        // Kill previous live attempts on retry (enqueue expire tasks for other non-terminal attempts)
        List<PaymentAttemptStatus> terminalStatuses = PaymentAttemptStatus.terminalStatuses();
        List<PaymentAttempt> nonTerminalAttempts = paymentAttemptRepository.getAllNonTerminalPaymentAttemptsExcept(
                payment.getId(),
                newAttempt.getId(),
                terminalStatuses
        );

        Instant now = Instant.now();
        for (PaymentAttempt pa : nonTerminalAttempts) {
            if (pa.getCheckoutSessionId() == null || pa.getCheckoutSessionId().isBlank()) {
                log.warn("retryPayment paymentId={} attemptId={} is non-terminal but has no checkoutSessionId; skipping expire task", payment.getId(), pa.getId());
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
                // idempotency: payment_attempt_id is UNIQUE on the task table
                log.warn("retryPayment paymentId={} attemptId={} already has checkout_session_expire_task", payment.getId(), pa.getId());
            }
        }

        Payment refreshedPayment = paymentRepository.findById(payment.getId()).orElseThrow(()->new PaymentApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                "Cannot refetch Payment: paymentId=" + payment.getId()));

        return new RetryTx1Result(refreshedPayment, newAttempt, false, null);
    }

    @Transactional
    public void markAttemptFailed(UUID paymentAttemptId) {
        PaymentAttempt paymentAttempt = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);
        if (paymentAttempt == null) {
            throw new PaymentApiException(
                    HttpStatus.NOT_FOUND,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_FOUND,
                    "PaymentAttempt not found: paymentAttemptId=" + paymentAttemptId
            );
        }

        // idempotent: if already FAILED, update returns 0 and that's fine
        paymentAttemptRepository.setStatusUnless(paymentAttemptId, PaymentAttemptStatus.FAILED, PaymentAttemptStatus.FAILED);
    }


    @Transactional
    public void updateCheckoutSessionAndRedirectUrl(UUID paymentAttemptId, String checkoutSessionId, String redirectUrl, String paymentIntentId)
    {
        PaymentAttempt paymentAttempt = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);

        if (paymentAttempt == null) {
            throw new PaymentApiException(
                    HttpStatus.NOT_FOUND,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_FOUND,
                    "PaymentAttempt not found: paymentAttemptId=" + paymentAttemptId
            );
        }

        paymentAttempt.setCheckoutSessionId(checkoutSessionId);
        paymentAttempt.setRedirectUrl(redirectUrl);

        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            paymentAttempt.setPaymentIntentId(paymentIntentId);
        }
        paymentAttemptRepository.save(paymentAttempt);
    }

    public record RetryTx1Result(
            Payment payment,
            PaymentAttempt paymentAttempt,
            boolean shouldReturnImmediately,
            CreatePaymentResponse immediateResponse
    ) {

    }
}
