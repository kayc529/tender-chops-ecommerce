package com.kaycheung.inventory_service.job.reservation_expiry;

import com.kaycheung.inventory_service.config.properties.ScheduledJobProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationExpiryWorker {
    private final ReservationExpiryService reservationExpiryService;
    private final ScheduledJobProperties scheduledJobProperties;

    public void releaseExpiredReservations(){
        int batchSize = scheduledJobProperties.getReservationExpiry().getBatchSize();
        reservationExpiryService.releaseExpiredReservations(batchSize);
    }
}
