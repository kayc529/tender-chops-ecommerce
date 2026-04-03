package com.kaycheung.order_service.repository.projection;

import java.util.UUID;

public interface OrderSourceQuoteIdProjection {
    UUID getId();
    UUID getSourceQuoteId();
}
