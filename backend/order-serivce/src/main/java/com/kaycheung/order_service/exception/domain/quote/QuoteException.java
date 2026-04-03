package com.kaycheung.order_service.exception.domain.quote;

import com.kaycheung.order_service.exception.code.QuoteErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class QuoteException extends RuntimeException {
    private final QuoteErrorCode errorCode;
    private final HttpStatus httpStatus;

    public QuoteException(HttpStatus httpStatus, QuoteErrorCode errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
