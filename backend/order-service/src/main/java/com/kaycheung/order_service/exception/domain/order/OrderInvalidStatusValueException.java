package com.kaycheung.order_service.exception.domain.order;

import com.kaycheung.order_service.exception.code.OrderErrorCode;
import org.springframework.http.HttpStatus;

public class OrderInvalidStatusValueException extends OrderException {
    public OrderInvalidStatusValueException(String invalidStatus) {
        super(HttpStatus.BAD_REQUEST, OrderErrorCode.ORDER_INVALID_STATUS_VALUE, "Invalid order status value: " + invalidStatus);
    }
}
