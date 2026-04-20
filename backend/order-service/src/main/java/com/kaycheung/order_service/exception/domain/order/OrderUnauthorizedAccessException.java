package com.kaycheung.order_service.exception.domain.order;

import com.kaycheung.order_service.exception.code.OrderErrorCode;
import org.springframework.http.HttpStatus;

public class OrderUnauthorizedAccessException extends OrderException {
    public OrderUnauthorizedAccessException() {
        super(HttpStatus.FORBIDDEN, OrderErrorCode.ORDER_UNAUTHORIZED_ACCESS, "Access denied: caller is not authorized to access this order");
    }
}
