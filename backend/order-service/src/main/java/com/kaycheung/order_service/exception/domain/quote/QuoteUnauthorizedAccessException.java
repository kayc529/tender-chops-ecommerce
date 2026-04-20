package com.kaycheung.order_service.exception.domain.quote;

import com.kaycheung.order_service.exception.code.QuoteErrorCode;
import org.springframework.http.HttpStatus;

public class QuoteUnauthorizedAccessException extends QuoteException {
    public QuoteUnauthorizedAccessException() {
        super(HttpStatus.FORBIDDEN, QuoteErrorCode.QUOTE_UNAUTHORIZED_ACCESS, "Access denied: caller is not authorized to access this quote");
    }
}
