package com.kaycheung.order_service.messaging.outbox.payload;

import java.util.UUID;

public record OrderCreatedPayload (
        UUID quoteId,
        UUID orderId
){
}
