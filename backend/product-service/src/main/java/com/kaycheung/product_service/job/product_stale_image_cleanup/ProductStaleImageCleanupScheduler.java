package com.kaycheung.product_service.job.product_stale_image_cleanup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStaleImageCleanupScheduler {
    private final ProductStaleImageCleanupWorker productStaleImageCleanupWorker;

    /**
     * Runs periodically to expire inventory reservations whose expiresAt has passed.
     */
    @Scheduled(fixedDelayString = "${product-stale-image-cleanup.fixed-delay-ms:5000}")
    public void run() {
        try {
            productStaleImageCleanupWorker.deleteImages();
        } catch (Exception ex) {
            // Never crash the scheduler thread; log and continue.
            log.warn("ReservationExpiryScheduler failed", ex);
        }
    }
}
