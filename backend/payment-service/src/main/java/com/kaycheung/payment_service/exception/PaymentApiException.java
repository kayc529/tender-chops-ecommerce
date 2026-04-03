package com.kaycheung.payment_service.exception;

import com.kaycheung.payment_service.exception.code.PaymentErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentApiException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final PaymentErrorCode errorCode;

    public PaymentApiException(HttpStatus httpStatus, PaymentErrorCode errorCode, String debugMessage) {
        super(debugMessage);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public PaymentApiException(HttpStatus httpStatus, PaymentErrorCode errorCode, String debugMessage, Throwable cause) {
        super(debugMessage, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
