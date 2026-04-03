package com.kaycheung.order_service.client.cart;

import com.kaycheung.order_service.exception.client.CartClientException;
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
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Service
public class CartClient {
    private static final Logger log = LoggerFactory.getLogger(CartClient.class);
    private final WebClient cartWebClient;

    public CartClient(@Qualifier("cartWebClient") WebClient cartWebClient) {
        this.cartWebClient = cartWebClient;
    }

    public void emptyCart(UUID userId) {
        cartWebClient
                .post()
                .uri("/internal/cart/clear")
                .header("X-User-Id", userId.toString())
                .retrieve()
                // Combine 4xx/5xx handling: 4xx -> log and stop; 5xx -> throw to trigger retry
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(CartErrorResponse.class)
                                .defaultIfEmpty(new CartErrorResponse(response.statusCode().value(), "CART_SERVICE_ERROR", "Cart service error", "cart-service returned error without body (emptyCart), httpStatus="
                                        + response.statusCode().value()))
                                .flatMap(body -> {
                                    int status = response.statusCode().value();

                                    // Upstream 5xx -> collapse to 503
                                    if (status >= 500) {
                                        log.warn(
                                                "Cart service error during emptyCart (5xx). status={}, userMessage={}, debugMessage={}, userId={} ",
                                                status,
                                                body.userMessage(),
                                                body.debugMessage(),
                                                userId
                                        );
                                        return Mono.error(new CartClientException(
                                                503,
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                                                "Cart Service Error(" + status + "): " + body.debugMessage()
                                        ));
                                    }

                                    // Upstream 4xx: best-effort -> log and do NOT retry; swallow later via onErrorResume
                                    log.warn(
                                            "Cart service rejected emptyCart (4xx). status={}, userMessage={}, debugMessage={}, userId={}",
                                            status,
                                            body.userMessage(),
                                            body.debugMessage(),
                                            userId
                                    );
                                    return Mono.error(new CartClientException(
                                            status,
                                            body.errorCode(),
                                            body.userMessage(),
                                            "Cart Service Error(" + status + "): " + body.debugMessage()
                                    ));
                                }))
                .bodyToMono(Void.class)
                // Network/codec/decode failures -> treat as 503-like and retry
                .onErrorMap(WebClientRequestException.class, ex -> new CartClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Cart service request failure (emptyCart)",
                        ex
                ))
                .onErrorMap(DecodingException.class, ex -> new CartClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Cart service decode error (emptyCart)",
                        ex
                ))
                .onErrorMap(CodecException.class, ex -> new CartClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Cart service codec error (emptyCart)",
                        ex
                ))
                // Retry only for service-unavailable style failures
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .maxBackoff(Duration.ofSeconds(2))
                                .filter(ex -> ex instanceof CartClientException cce && cce.getStatus() == 503)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying emptyCart (attempt {} of 3), cause={}",
                                        rs.totalRetries() + 1,
                                        rs.failure().toString()
                                ))
                )
                .doOnError(ex -> log.warn("Failed to empty cart after retries. userId={}", userId, ex))
                // Best-effort: swallow all errors
                .onErrorResume(ex -> Mono.empty())
                .block();
    }

    public CartResponseDTO getCart(UUID userId) {
        return execute(cartWebClient
                        .get()
                        .uri("/internal/cart")
                        .header("X-User-Id", userId.toString()),
                CartResponseDTO.class,
                "getCart");
    }

    /**
     * Executes a WebClient request with unified error handling and retries.
     * - Uses CartErrorResponse as error body
     * - Combines 4xx and 5xx handling into one onStatus
     * - Forwards 4xx errors as-is into CartClientException
     * - Collapses 5xx errors into 503 + COMMON_SERVICE_UNAVAILABLE
     * - Retries ONLY when resulting exception is CartClientException with status == 503
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
                        response.bodyToMono(CartErrorResponse.class)
                                .defaultIfEmpty(new CartErrorResponse(
                                        response.statusCode().value(),
                                        "CART_SERVICE_ERROR",
                                        "Cart service error",
                                        "cart-service returned error without body (" + context + "), httpStatus="
                                                + response.statusCode().value()
                                ))
                                .map(err -> {
                                    // Upstream 5xx -> collapse to 503
                                    if (err.status() >= 500) {
                                        return new CartClientException(
                                                503,
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                                                "Cart Service Error(" + err.status() + "): " + err.debugMessage()
                                        );
                                    }
                                    // Upstream 4xx -> forward as-is
                                    return new CartClientException(
                                            err.status(),
                                            err.errorCode(),
                                            err.userMessage(),
                                            "Cart Service Error(" + err.status() + "): " + err.debugMessage()
                                    );
                                })
                )
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, ex -> new CartClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Cart service request failure (" + context + ")",
                        ex
                ))
                .onErrorMap(DecodingException.class, ex -> new CartClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Cart service decode error (" + context + ")",
                        ex
                ))
                .onErrorMap(CodecException.class, ex -> new CartClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Cart service codec error (" + context + ")",
                        ex
                ))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .filter(ex -> ex instanceof CartClientException cce && cce.getStatus() == 503)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying cart-service call ({}) attempt {}",
                                        context,
                                        rs.totalRetries() + 1
                                ))
                )
                .blockOptional()
                .orElseThrow(() -> new CartClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Cart service returned empty response body (" + context + ")"
                ));
    }
}
