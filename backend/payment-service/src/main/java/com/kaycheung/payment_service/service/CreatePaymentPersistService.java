package com.kaycheung.payment_service.service;

import com.kaycheung.payment_service.domain.Providers;
import com.kaycheung.payment_service.dto.CreatePaymentRequest;
import com.kaycheung.payment_service.entity.Payment;
import com.kaycheung.payment_service.entity.PaymentAttempt;
import com.kaycheung.payment_service.entity.PaymentAttemptStatus;
import com.kaycheung.payment_service.entity.PaymentStatus;
import com.kaycheung.payment_service.exception.PaymentApiException;
import com.kaycheung.payment_service.exception.code.PaymentErrorCode;
import com.kaycheung.payment_service.repository.PaymentAttemptRepository;
import com.kaycheung.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePaymentPersistService {
    private static final Logger log = LoggerFactory.getLogger(CreatePaymentPersistService.class);
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;

    @Transactional
    public PersistedPaymentAndPaymentAttempt createPaymentAndPaymentAttempt(CreatePaymentRequest request, UUID userId) {

        UUID newPaymentId = UUID.randomUUID();

        UUID insertedPaymentId = paymentRepository.tryInsertPaymentReturningId(
                newPaymentId,
                request.orderId(),
                userId,
                PaymentStatus.PENDING.name(),
                request.amount(),
                request.currency(),
                Providers.STRIPE.name()
        );

        // Conflict → payment already exists
        if (insertedPaymentId == null) {
            Payment existing = paymentRepository.findByOrderId(request.orderId())
                    .orElseThrow(() -> new PaymentApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                            "Payment exists (conflict) but cannot refetch by orderId=" + request.orderId()
                    ));

            if (!userId.equals(existing.getUserId())) {
                throw new PaymentApiException(
                        HttpStatus.FORBIDDEN,
                        PaymentErrorCode.PAYMENT_UNAUTHORIZED,
                        "Access denied"
                );
            }

            return new PersistedPaymentAndPaymentAttempt(true, existing, null);
        }

        // Inserted → fetch the new payment row
        Payment payment = paymentRepository.findById(insertedPaymentId)
                .orElseThrow(() -> new PaymentApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                        "Inserted payment id not found immediately: paymentId=" + insertedPaymentId
                ));

        // Attempt #1
        UUID newAttemptId = UUID.randomUUID();
        String idempotencyKey = "order:" + request.orderId() + ":attempt:1";

        UUID insertedAttemptId = paymentAttemptRepository.tryInsertAttemptReturningId(
                newAttemptId,
                payment.getId(),
                1,
                PaymentAttemptStatus.AUTH_PENDING.name(),
                idempotencyKey
        );

        PaymentAttempt attempt;
        if (insertedAttemptId == null) {
            // Rare case: someone already inserted attempt #1 (shouldn’t happen if payment was new, but safe anyway)
            attempt = paymentAttemptRepository.findByPaymentIdAndAttemptNo(payment.getId(), 1)
                    .orElseThrow(() -> new PaymentApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                            "Attempt #1 exists (conflict) but cannot refetch: paymentId=" + payment.getId()
                    ));
        } else {
            attempt = paymentAttemptRepository.findById(insertedAttemptId)
                    .orElseThrow(() -> new PaymentApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            PaymentErrorCode.PAYMENT_INCONSISTENT_STATE,
                            "Inserted attempt id not found immediately: attemptId=" + insertedAttemptId
                    ));
        }

        // Point payment.currentAttemptId → attempt #1
        payment.setCurrentAttemptId(attempt.getId());
        paymentRepository.save(payment);

        return new PersistedPaymentAndPaymentAttempt(false, payment, attempt);
    }

    @Transactional
    public void updateCheckoutSessionAndRedirectUrl(UUID paymentAttemptId, String checkoutSessionId, String redirectUrl, String paymentIntentId) {
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

    @Transactional
    public void updateRedirectUrl(UUID paymentAttemptId, String redirectUrl) {
        PaymentAttempt paymentAttempt = paymentAttemptRepository.findById(paymentAttemptId).orElse(null);
        if (paymentAttempt == null) {
            throw new PaymentApiException(
                    HttpStatus.NOT_FOUND,
                    PaymentErrorCode.PAYMENT_ATTEMPT_NOT_FOUND,
                    "PaymentAttempt not found: paymentAttemptId=" + paymentAttemptId
            );
        }
        paymentAttempt.setRedirectUrl(redirectUrl);
        paymentAttemptRepository.save(paymentAttempt);
    }

    public record PersistedPaymentAndPaymentAttempt(
            boolean alreadyExists, Payment payment, PaymentAttempt paymentAttempt
    ) {
    }
}

