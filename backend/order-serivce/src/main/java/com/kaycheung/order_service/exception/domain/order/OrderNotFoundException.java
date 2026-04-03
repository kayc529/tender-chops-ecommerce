package com.kaycheung.order_service.exception.domain.order;

import com.kaycheung.order_service.exception.code.OrderErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotFoundException extends OrderException {

    public OrderNotFoundException(UUID orderId) {
        super(HttpStatus.NOT_FOUND, OrderErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId);
    }
}
