package com.kaycheung.product_service.repository.projection;

import java.util.UUID;

public interface ProductBasePriceProjection {
    UUID getId();
    Long getBasePrice();
}
