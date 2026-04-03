package com.kaycheung.payment_service.job.checkout_session_expire;

import com.kaycheung.payment_service.entity.CheckoutSessionExpireTask;
import com.kaycheung.payment_service.repository.CheckoutSessionExpireTaskRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutSessionExpireService {

    private static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

    private final CheckoutSessionExpireTaskRepository checkoutSessionExpireTaskRepository;

    /**
     * Stripe network call to expire a Checkout Session.
     * Expiring the Checkout Session will also cancel the underlying PaymentIntent (Stripe behavior).
     */
    public Session expireCheckoutSession(String checkoutSessionId) throws StripeException {
        // Retrieve is required to obtain the Session object, then expire it.
        Session session = Session.retrieve(checkoutSessionId);
        return session.expire();
    }

    /**
     * Mark a task as completed (idempotent). We keep lastError as an audit note if provided.
     */
    @Transactional
    public void completeTask(UUID taskId, Instant now, String note) {
        CheckoutSessionExpireTask task = checkoutSessionExpireTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            // If task is gone, treat as idempotent completion.
            return;
        }

        if (task.getCompletedAt() != null) {
            return;
        }

        task.setCompletedAt(now);
        task.setLastError(note); // keep as note for debugging/audit; can be null
        checkoutSessionExpireTaskRepository.save(task);
    }

    /**
     * Reschedule a task with exponential backoff (idempotent-ish).
     * attemptCount increments only on reschedule.
     */
    @Transactional
    public void rescheduleTask(UUID taskId, String lastError) {
        CheckoutSessionExpireTask task = checkoutSessionExpireTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }

        if (task.getCompletedAt() != null) {
            return;
        }

        int currentAttempts = task.getAttemptCount();
        int nextAttempts = currentAttempts + 1;
        task.setAttemptCount(nextAttempts);
        task.setLastError(lastError);

        Instant now = Instant.now();
        Duration backoff = computeBackoff(nextAttempts);
        task.setNextAttemptAt(now.plus(backoff));

        checkoutSessionExpireTaskRepository.save(task);
    }

    private Duration computeBackoff(int attemptCount) {
        // Exponential backoff: BASE * 2^(attemptCount-1), capped.
        // attemptCount starts at 1 on first retry.
        long multiplier;
        if (attemptCount <= 1) {
            multiplier = 1L;
        } else {
            // prevent overflow / ridiculous values
            int exp = Math.min(attemptCount - 1, 10);
            multiplier = 1L << exp;
        }

        Duration raw = BASE_BACKOFF.multipliedBy(multiplier);
        return raw.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : raw;
    }

}
