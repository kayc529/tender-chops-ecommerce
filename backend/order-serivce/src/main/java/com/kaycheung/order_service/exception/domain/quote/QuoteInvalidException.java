package com.kaycheung.order_service.exception.domain.quote;

import com.kaycheung.order_service.exception.code.QuoteErrorCode;
import org.springframework.http.HttpStatus;

public class QuoteInvalidException extends QuoteException {
    public QuoteInvalidException(String debugMessage) {
        super(HttpStatus.BAD_REQUEST, QuoteErrorCode.QUOTE_INVALID, debugMessage);
    }
}
