package com.kaycheung.order_service.exception.domain.quote;

import com.kaycheung.order_service.exception.code.QuoteErrorCode;
import org.springframework.http.HttpStatus;

public class QuoteExpiredException extends QuoteException {

    public QuoteExpiredException(String debugMessage) {
        super(HttpStatus.GONE, QuoteErrorCode.QUOTE_EXPIRED, debugMessage);
    }
}
