package com.kaycheung.cart_service.client.product;

import com.kaycheung.cart_service.exception.client.ProductClientException;
import com.kaycheung.cart_service.exception.code.CommonErrorCode;
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
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private final WebClient productWebClient;

    public ProductClient(@Qualifier("productWebClient") WebClient webClient) {
        this.productWebClient = webClient;
    }

    public ProductResponseDTO getProductById(UUID productId) {
        return execute(
                productWebClient.get().uri("/products/{id}", productId),
                ProductResponseDTO.class,
                "getProductById"
        );
    }

    public ProductBasePriceResponseDTO getProductBasePrices(ProductBasePriceRequestDTO request) {
        return execute(
                productWebClient
                        .post()
                        .uri("/internal/products")
                        .bodyValue(request),
                ProductBasePriceResponseDTO.class,
                "getProductBasePrices"
        );
    }

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
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .filter(ex -> ex instanceof WebClientRequestException
                                        || (ex instanceof ProductClientException pce && pce.getStatus() == 503))
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying product-service call ({}) attempt {}",
                                        context,
                                        rs.totalRetries() + 1
                                ))
                )
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
                .blockOptional()
                .orElseThrow(() -> new ProductClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Product service returned empty response body (" + context + ")"
                ));
    }

}
