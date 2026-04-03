package com.kaycheung.payment_service.job.checkout_session_expire;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutSessionExpireScheduler {

    private final CheckoutSessionExpireWorker checkoutSessionExpireWorker;

    /**
     * Runs periodically to expire Stripe Checkout Sessions for non-terminal payment attempts.
     */
    @Scheduled(fixedDelayString = "${jobs.checkout-session-expire.fixed-delay-ms:5000}")
    public void run() {
        try {
            checkoutSessionExpireWorker.expireDueCheckoutSessions();
        } catch (Exception ex) {
            // Never crash the scheduler thread; log and continue.
            log.warn("CheckoutSessionExpireScheduler failed", ex);
        }
    }
}
