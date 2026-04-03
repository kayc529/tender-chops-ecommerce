package com.kaycheung.product_service.client;

import com.kaycheung.product_service.exception.client.InventoryClientException;
import com.kaycheung.product_service.exception.code.CommonErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Service
public class InventoryClient {

    private final WebClient inventoryWebClient;

    public InventoryClient(@Qualifier("inventoryWebClient") WebClient inventoryWebClient) {
        this.inventoryWebClient = inventoryWebClient;
    }

    public InventoryResponseDTO createInventoryForNewProduct(InventoryCreateRequestDTO request) {
        UUID productId = request.productId();

        return inventoryWebClient
                .post()
                .uri("/internal/inventory")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(InventoryErrorResponse.class)
                                .defaultIfEmpty(new InventoryErrorResponse(
                                        response.statusCode().value(),
                                        "INVENTORY_SERVICE_ERROR",
                                        "Inventory service error",
                                        "inventory-service returned error without body (createInventoryForNewProduct), httpStatus="
                                                + response.statusCode().value() + ", productId=" + productId
                                ))
                                .map(err -> {
                                    // 5xx from upstream -> collapse to 503
                                    if (err.status() >= 500) {
                                        return new InventoryClientException(
                                                503,
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                                                CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                                                "Inventory Service Error(" + err.status() + "): " + err.debugMessage()
                                        );
                                    }
                                    // 4xx from upstream -> forward as-is
                                    return new InventoryClientException(
                                            err.status(),
                                            err.errorCode(),
                                            err.userMessage(),
                                            "Inventory Service Error(" + err.status() + "): " + err.debugMessage()
                                    );
                                })
                )
                .bodyToMono(InventoryResponseDTO.class)
                // Network / timeout / decode errors (no usable upstream error response) -> collapse to 503
                .onErrorMap(WebClientRequestException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service request failure (createInventoryForNewProduct), productId=" + productId, ex
                ))
                .onErrorMap(TimeoutException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service timeout (createInventoryForNewProduct), productId=" + productId, ex
                ))
                .onErrorMap(DecodingException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service decode error (createInventoryForNewProduct), productId=" + productId,
                        ex
                ))
                .onErrorMap(CodecException.class, ex -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service codec error (createInventoryForNewProduct), productId=" + productId,
                        ex
                ))
                .blockOptional()
                .orElseThrow(() -> new InventoryClientException(
                        503,
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getCode(),
                        CommonErrorCode.COMMON_SERVICE_UNAVAILABLE.getMessage(),
                        "Inventory service returned empty response body (createInventoryForNewProduct), productId=" + productId
                ));
    }
}