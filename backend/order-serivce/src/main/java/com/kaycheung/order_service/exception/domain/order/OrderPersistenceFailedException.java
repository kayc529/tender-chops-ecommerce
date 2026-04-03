package com.kaycheung.order_service.exception.domain.order;

import com.kaycheung.order_service.exception.code.OrderErrorCode;
import org.springframework.http.HttpStatus;

public class OrderPersistenceFailedException extends OrderException {
    public OrderPersistenceFailedException(String debugMessage) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, OrderErrorCode.ORDER_PERSISTENCE_FAILED, debugMessage);
    }
}
