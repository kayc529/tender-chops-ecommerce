package com.kaycheung.order_service.client.payment;

import com.kaycheung.order_service.exception.client.PaymentClientException;
import com.kaycheung.order_service.exception.code.CommonErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Service
public class PaymentClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private final WebClient paymentWebClient;

    public PaymentClient(@Qualifier("paymentWebClient") WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request, UUID userId) {
        return execute(
                paymentWebClient
                        .post()
                        .uri("/internal/payments")
                        .header("X-User-Id", userId.toString())
                        .bodyValue(request),
                CreatePaymentResponse.class,
                "createPayment"
        );
    }

    /**
     * Executes a WebClient request with unified error handling and retries.
     * - Uses PaymentErrorResponse as error body
     * - Combines 4xx and 5xx handling into one onStatus
     * - Forwards 4xx errors as-is into PaymentClientException
     * - Collapses 5xx errors into 503 + COMMON_SERVICE_UNAVAILABLE
     * - Retries ONLY when resulting exception is PaymentClientException with status == 503
     * - Maps request/decode/codec errors to 503 + COMMON_SERVICE_UNAVAILABLE
     * - Uses blockOptional().orElseThrow(...) for empty body
     */
    private <T> T execute(
            WebClient.RequestHeadersSpec<?> request,
            Class<T> responseType,
            String context
    ) {
        return request
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(PaymentErrorResponse.class)
                                .defaultIfEmpty(new PaymentErrorResponse(
                                        response.statusCode().value(),
                                        "PAYMENT_SERVICE_ERROR",
                                        "Payment service error",
                                        "payment-service returned error without body (" + context + "), httpStatus="
                                                + response.statusCode().value()
                                ))
                                .map(err -> {
                                    // Upstream 5xx -> collapse to 503
                                    if (err.status() >= 500) {
                                        return new PaymentClientException(
                                                503,
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                                                "Payment Service Error(" + err.status() + "): " + err.debugMessage()
                                        );
                                    }
                                    // Upstream 4xx -> forward as-is
                                    return new PaymentClientException(
                                            err.status(),
                                            err.errorCode(),
                                            err.userMessage(),
                                            "Payment Service Error(" + err.status() + "): " + err.debugMessage()
                                    );
                                })
                )
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, ex -> new PaymentClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Payment service request failure (" + context + ")",
                        ex
                ))
                .onErrorMap(DecodingException.class, ex -> new PaymentClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Payment service decode error (" + context + ")",
                        ex
                ))
                .onErrorMap(CodecException.class, ex -> new PaymentClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Payment service codec error (" + context + ")",
                        ex
                ))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .filter(ex -> ex instanceof PaymentClientException pce && pce.getStatus() == 503)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying payment-service call ({}) attempt {}",
                                        context,
                                        rs.totalRetries() + 1
                                ))
                )
                .blockOptional()
                .orElseThrow(() -> new PaymentClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Payment service returned empty response body (" + context + ")"
                ));
    }

}
