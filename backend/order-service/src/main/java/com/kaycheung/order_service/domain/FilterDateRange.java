package com.kaycheung.order_service.domain;

import java.time.Instant;

public record FilterDateRange(Instant startInclusive, Instant endExclusive) {
    public static FilterDateRange getUnboundedDateRange() {
        return new FilterDateRange(null, null);
    }

    public boolean isUnbounded() {
        return startInclusive == null && endExclusive == null;
    }
}
