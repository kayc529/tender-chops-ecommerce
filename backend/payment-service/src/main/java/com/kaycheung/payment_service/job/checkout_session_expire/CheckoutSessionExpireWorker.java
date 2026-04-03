package com.kaycheung.payment_service.job.checkout_session_expire;


import com.kaycheung.payment_service.config.properties.CheckoutSessionExpireProperties;
import com.kaycheung.payment_service.entity.CheckoutSessionExpireTask;
import com.kaycheung.payment_service.repository.CheckoutSessionExpireTaskRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CheckoutSessionExpireWorker {
    private static final Logger log = LoggerFactory.getLogger(CheckoutSessionExpireWorker.class);
    private final CheckoutSessionExpireService checkoutSessionExpireService;
    private final CheckoutSessionExpireTaskRepository checkoutSessionExpireTaskRepository;
    private final CheckoutSessionExpireProperties checkoutSessionExpireProperties;

    public void expireDueCheckoutSessions() {
        //  fetch N due task
        Instant now = Instant.now();
        int batchSize = checkoutSessionExpireProperties.getBatchSize();

        List<CheckoutSessionExpireTask> tasks = checkoutSessionExpireTaskRepository.findDueTasks(
                now,
                PageRequest.of(0, batchSize)
        );

        if (tasks.isEmpty()) {
            return;
        }


        for (CheckoutSessionExpireTask task : tasks) {
            try {
                handleOneTask(task, now);
            } catch (Exception ex) {
                // Never let one task crash the whole batch
                log.warn("CheckoutSessionExpireWorker unexpected error: taskId={} paymentId={} paymentAttemptId={}",
                        task.getId(), task.getPaymentId(), task.getPaymentAttemptId(), ex);

                try {
                    checkoutSessionExpireService.rescheduleTask(task.getId(), "unexpected_error: " + ex.getClass().getSimpleName());
                } catch (Exception ignored) {
                    // best-effort
                }
            }
        }
    }

    private void handleOneTask(CheckoutSessionExpireTask task, Instant now) throws StripeException {
        if (task.getCheckoutSessionId() == null || task.getCheckoutSessionId().isBlank()) {
            log.warn("CheckoutSessionExpireWorker taskId={} has blank checkoutSessionId; completing task", task.getId());
            checkoutSessionExpireService.completeTask(task.getId(), now, "blank_checkout_session_id");
            return;
        }

        try {
            // Expire Checkout Session (Stripe will cancel underlying PaymentIntent when expiring the session)
            Session expired = checkoutSessionExpireService.expireCheckoutSession(task.getCheckoutSessionId());

            // Even if Stripe returns something odd, treat a successful call as completion
            String status = expired != null ? expired.getStatus() : null;
            log.info("CheckoutSessionExpireWorker expired session: taskId={} sessionId={} status={}",
                    task.getId(), task.getCheckoutSessionId(), status);

            checkoutSessionExpireService.completeTask(task.getId(), now, null);
        } catch (StripeException ex) {
            if (isAlreadyExpiredOrGone(ex)) {
                // Stripe says there's nothing to do; treat as completed
                log.info("CheckoutSessionExpireWorker session already expired/gone: taskId={} sessionId={} message={}",
                        task.getId(), task.getCheckoutSessionId(), safeStripeMessage(ex));
                checkoutSessionExpireService.completeTask(task.getId(), now, safeStripeMessage(ex));
                return;
            }


            String message = safeStripeMessage(ex);
            log.warn("CheckoutSessionExpireWorker failed to expire session: taskId={} sessionId={} message={}",
                    task.getId(), task.getCheckoutSessionId(), message);

            checkoutSessionExpireService.rescheduleTask(task.getId(), message);
        }
    }


    private boolean isAlreadyExpiredOrGone(StripeException ex) {
        if (ex == null) return false;

        // Prefer Stripe error codes when available
        if (ex.getStripeError() != null) {
            String code = ex.getStripeError().getCode();
            String type = ex.getStripeError().getType();
            if (code != null) {
                // Common cases we can safely treat as idempotent completion
                if ("resource_missing".equals(code)
                        || "checkout_session_expired".equals(code)
                        || "checkout_session_already_expired".equals(code)) {
                    return true;
                }
            }
            // Some Stripe errors may not set code; type can still hint
            if ("invalid_request_error".equals(type) && ex.getStatusCode() != null && ex.getStatusCode() == 404) {
                return true;
            }
        }

        // Fallback on message text (last resort)
        String msg = safeStripeMessage(ex);
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("already expired")
                || lower.contains("has expired")
                || lower.contains("no such checkout session")
                || lower.contains("resource_missing");
    }

    private String safeStripeMessage(StripeException ex) {
        if (ex == null) return null;
        if (ex.getStripeError() != null && ex.getStripeError().getMessage() != null) {
            return ex.getStripeError().getMessage();
        }
        return ex.getMessage();
    }


}
