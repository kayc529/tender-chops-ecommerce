package com.kaycheung.order_service.exception.domain.order;

import com.kaycheung.order_service.exception.code.OrderErrorCode;
import org.springframework.http.HttpStatus;

public class OrderChangeStatusException extends OrderException {
    public OrderChangeStatusException(String debugMessage) {
        super(HttpStatus.CONFLICT, OrderErrorCode.ORDER_INVALID_STATUS, debugMessage);
    }
}
