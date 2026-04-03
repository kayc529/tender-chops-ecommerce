package com.kaycheung.cart_service.client.inventory;

import com.kaycheung.cart_service.exception.client.InventoryClientException;
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
import java.util.concurrent.TimeoutException;

@Service
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private final WebClient inventoryWebClient;

    public InventoryClient(@Qualifier("inventoryWebClient") WebClient inventoryWebClient) {
        this.inventoryWebClient = inventoryWebClient;
    }

    public InventoryAvailabilityResponseDTO getAvailabilityBatch(InventoryAvailabilityRequestDTO request) {
        return inventoryWebClient
                .post()
                .uri("/api/v1/internal/inventory/batch")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(InventoryErrorResponse.class)
                                .defaultIfEmpty(new InventoryErrorResponse(
                                        response.statusCode().value(),
                                        "INVENTORY_SERVICE_ERROR",
                                        "Inventory service error",
                                        "inventory-service returned error without body (getAvailabilityBatch), httpStatus="
                                                + response.statusCode().value()
                                ))
                                .map(err -> {
                                    // Upstream 5xx -> collapse to 503
                                    if (err.status() >= 500) {
                                        log.error(
                                                "Inventory-service 5xx on getAvailabilityBatch. upstreamStatus={}, errorCode={}, userMessage={}",
                                                err.status(), err.errorCode(), err.userMessage()
                                        );
                                        return new InventoryClientException(
                                                503,
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                                                "Inventory Service Error(" + err.status() + "): " + err.debugMessage()
                                        );
                                    }

                                    // Upstream 4xx -> forward as-is
                                    log.warn(
                                            "Inventory-service 4xx on getAvailabilityBatch. upstreamStatus={}, errorCode={}, userMessage={}",
                                            err.status(), err.errorCode(), err.userMessage()
                                    );
                                    return new InventoryClientException(
                                            err.status(),
                                            err.errorCode(),
                                            err.userMessage(),
                                            "Inventory Service Error(" + err.status() + "): " + err.debugMessage()
                                    );
                                })
                )
                .bodyToMono(InventoryAvailabilityResponseDTO.class)
                // Network / timeout / decode / codec -> collapse to 503
                .onErrorMap(WebClientRequestException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service request failure (getAvailabilityBatch)",
                        ex
                ))
                .onErrorMap(TimeoutException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service timeout (getAvailabilityBatch)",
                        ex
                ))
                .onErrorMap(DecodingException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service decode error (getAvailabilityBatch)",
                        ex
                ))
                .onErrorMap(CodecException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service codec error (getAvailabilityBatch)",
                        ex
                ))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .filter(ex -> ex instanceof WebClientRequestException
                                        || (ex instanceof InventoryClientException ice && ice.getStatus() == 503))
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying getAvailabilityBatch (attempt {} of 3), cause={}",
                                        rs.totalRetries() + 1,
                                        rs.failure().toString()
                                ))
                )
                .doOnError(ex ->
                        log.error(
                                "getAvailabilityBatch failed after retries. request={}",
                                request,
                                ex
                        )
                )
                .blockOptional()
                .orElseThrow(() -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service returned empty response body for availability batch"
                ));
    }
}
