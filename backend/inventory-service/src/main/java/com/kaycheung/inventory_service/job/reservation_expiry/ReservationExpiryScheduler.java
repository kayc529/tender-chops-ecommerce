package com.kaycheung.inventory_service.job.reservation_expiry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {
    private final ReservationExpiryWorker reservationExpiryWorker;

    /**
     * Runs periodically to expire inventory reservations whose expiresAt has passed.
     */
    @Scheduled(fixedDelayString = "${jobs.reservation-expiry.fixed-delay-ms:5000}")
    public void run() {
        try {
            reservationExpiryWorker.releaseExpiredReservations();
        } catch (Exception ex) {
            // Never crash the scheduler thread; log and continue.
            log.warn("ReservationExpiryScheduler failed", ex);
        }
    }
}
