package com.kaycheung.order_service.exception.domain.quote;

import com.kaycheung.order_service.exception.code.QuoteErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class QuoteNotFoundException extends QuoteException {
    public QuoteNotFoundException(UUID quoteId) {
        super(HttpStatus.NOT_FOUND, QuoteErrorCode.QUOTE_NOT_FOUND, "Quote not found: " + quoteId);
    }
}
