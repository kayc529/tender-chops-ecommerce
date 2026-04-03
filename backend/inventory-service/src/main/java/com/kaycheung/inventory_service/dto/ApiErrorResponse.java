package com.kaycheung.inventory_service.dto;

import java.time.Instant;

public record ApiErrorResponse(
        int status,
        String errorCode,
        String userMessage,
        String debugMessage,
        String path,
        Instant timestamp) {

}