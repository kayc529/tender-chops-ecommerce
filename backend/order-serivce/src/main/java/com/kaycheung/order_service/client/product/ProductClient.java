package com.kaycheung.order_service.client.product;

import com.kaycheung.order_service.exception.client.ProductClientException;
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

@Service
public class ProductClient {
    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private final WebClient productWebClient;

    public ProductClient(@Qualifier("productWebClient") WebClient productWebClient) {
        this.productWebClient = productWebClient;
    }

    public ProductResponseDTO getProductsWithBasePrice(ProductRequestDTO request) {

        return execute(productWebClient
                        .post()
                        .uri("/internal/products")
                        .bodyValue(request),
                ProductResponseDTO.class,
                "getProductsWithBasePrice");
    }

    /**
     * Executes a WebClient request with unified error handling and retries.
     * - Uses ProductErrorResponse as error body
     * - Combines 4xx and 5xx handling into one onStatus
     * - Forwards 4xx errors as-is into ProductClientException
     * - Collapses 5xx errors into 503 + COMMON_SERVICE_UNAVAILABLE
     * - Retries ONLY when resulting exception is ProductClientException with status == 503
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
                        response.bodyToMono(ProductErrorResponse.class)
                                .defaultIfEmpty(new ProductErrorResponse(
                                        response.statusCode().value(),
                                        "PRODUCT_SERVICE_ERROR",
                                        "Product service error",
                                        "product-service returned error without body (" + context + "), httpStatus="
                                                + response.statusCode().value()
                                ))
                                .map(err -> {
                                    // Upstream 5xx -> collapse to 503
                                    if (err.status() >= 500) {
                                        return new ProductClientException(
                                                503,
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                                                "Product Service Error(" + err.status() + "): " + err.debugMessage()
                                        );
                                    }
                                    // Upstream 4xx -> forward as-is
                                    return new ProductClientException(
                                            err.status(),
                                            err.errorCode(),
                                            err.userMessage(),
                                            "Product Service Error(" + err.status() + "): " + err.debugMessage()
                                    );
                                })
                )
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, ex -> new ProductClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Product service request failure (" + context + ")",
                        ex
                ))
                .onErrorMap(DecodingException.class, ex -> new ProductClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Product service decode error (" + context + ")",
                        ex
                ))
                .onErrorMap(CodecException.class, ex -> new ProductClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Product service codec error (" + context + ")",
                        ex
                ))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .filter(ex -> ex instanceof ProductClientException pce && pce.getStatus() == 503)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying product-service call ({}) attempt {}",
                                        context,
                                        rs.totalRetries() + 1
                                ))
                )
                .blockOptional()
                .orElseThrow(() -> new ProductClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Product service returned empty response body (" + context + ")"
                ));
    }
}
