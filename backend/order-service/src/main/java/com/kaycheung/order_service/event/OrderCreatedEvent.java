package com.kaycheung.order_service.event;

import java.util.UUID;

public record OrderCreatedEvent(UUID userId) {
}
