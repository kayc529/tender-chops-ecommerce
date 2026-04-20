package com.kaycheung.order_service.exception.domain.order;

import com.kaycheung.order_service.exception.code.OrderErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class OrderException extends RuntimeException {
    private final OrderErrorCode errorCode;
    private final HttpStatus httpStatus;

    public OrderException(HttpStatus httpStatus, OrderErrorCode errorCode, String message)
    {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
