package com.kaycheung.order_service.client.inventory;

import com.kaycheung.order_service.exception.client.InventoryClientException;
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
public class InventoryClient {
    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private final WebClient inventoryWebClient;
    private final WebClient inventoryInternalWebClient;

    public InventoryClient(@Qualifier("inventoryWebClient") WebClient inventoryWebClient, @Qualifier("inventoryInternalWebClient") WebClient inventoryInternalWebClient) {
        this.inventoryWebClient = inventoryWebClient;
        this.inventoryInternalWebClient = inventoryInternalWebClient;
    }

    public InventoryResponseDTO checkStockAndCreateReservations(InventoryRequestDTO request) {
        return execute(
                inventoryWebClient
                        .post()
                        .uri("/internal/inventory/reservations")
                        .bodyValue(request),
                InventoryResponseDTO.class,
                "checkStockAndCreateReservations"
        );
    }

    public InventoryConfirmReservationResponse checkReservationForPayment(InventoryConfirmReservationRequest request) {
        return execute(
                inventoryInternalWebClient
                        .post()
                        .uri("/internal/inventory/reservations/confirm-for-payment")
                        .bodyValue(request),
                InventoryConfirmReservationResponse.class,
                "checkReservationForOrderPayment"
        );
    }

    public void releaseReservationsForCompensation(InventoryReleaseReservationRequestDTO request) {
        try {
            inventoryWebClient
                    .post()
                    .uri("/internal/inventory/reservations/release-for-compensation")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.error(
                    "Failed to release inventory reservations for compensation. quoteId={}",
                    request.quoteId(),
                    ex
            );
        }
    }

    public void releaseReservationsForCancellation(InventoryReleaseReservationRequestDTO request) {
        try {
            inventoryWebClient
                    .post()
                    .uri("/internal/inventory/reservations/release-for-cancellation")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            // Best-effort: order is already cancelled; this is cleanup.
            log.error(
                    "Failed to release inventory reservations for cancellation. quoteId={}",
                    request.quoteId(),
                    ex
            );
        }
    }


    /**
     * Executes a WebClient request with unified error handling and retries.
     * - Uses InventoryErrorResponse as error body
     * - Combines 4xx and 5xx handling into one onStatus
     * - Forwards 4xx errors as-is into InventoryClientException
     * - Collapses 5xx errors into 503 + COMMON_SERVICE_UNAVAILABLE
     * - Retries ONLY when resulting exception is InventoryClientException with status == 503
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
                        response.bodyToMono(InventoryErrorResponse.class)
                                .defaultIfEmpty(new InventoryErrorResponse(
                                        response.statusCode().value(),
                                        "INVENTORY_SERVICE_ERROR",
                                        "Inventory service error",
                                        "inventory-service returned error without body (" + context + "), httpStatus="
                                                + response.statusCode().value()
                                ))
                                .map(err -> {
                                    // Upstream 5xx -> collapse to 503
                                    if (err.status() >= 500) {
                                        return new InventoryClientException(
                                                503,
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                                                "Inventory Service Error(" + err.status() + "): " + err.debugMessage()
                                        );
                                    }
                                    // Upstream 4xx -> forward as-is
                                    return new InventoryClientException(
                                            err.status(),
                                            err.errorCode(),
                                            err.userMessage(),
                                            "Inventory Service Error(" + err.status() + "): " + err.debugMessage()
                                    );
                                })
                )
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service request failure (" + context + ")",
                        ex
                ))
                .onErrorMap(DecodingException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service decode error (" + context + ")",
                        ex
                ))
                .onErrorMap(CodecException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service codec error (" + context + ")",
                        ex
                ))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .filter(ex -> ex instanceof InventoryClientException ice && ice.getStatus() == 503)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying inventory-service call ({}) attempt {}",
                                        context,
                                        rs.totalRetries() + 1
                                ))
                )
                .blockOptional()
                .orElseThrow(() -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service returned empty response body (" + context + ")"
                ));
    }
}
