package com.kaycheung.order_service.exception.domain.quote;

import com.kaycheung.order_service.exception.code.QuoteErrorCode;
import org.springframework.http.HttpStatus;

public class QuoteConflictException extends QuoteException {
    public QuoteConflictException(String debugMessage) {
        super(HttpStatus.CONFLICT, QuoteErrorCode.QUOTE_CONFLICT, debugMessage);
    }
}
