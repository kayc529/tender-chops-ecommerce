package com.kaycheung.payment_service.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum PaymentAttemptStatus {
    AUTH_PENDING(false),
    AUTHORIZED(false),
//    CAPTURE_PENDING(false),

    CAPTURED(true),
    FAILED(true),
    CANCELED(true);

    private static final List<PaymentAttemptStatus> TERMINAL =
            Arrays.stream(values()).filter(PaymentAttemptStatus::isTerminal).toList();
    private final boolean terminal;

    PaymentAttemptStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public static List<PaymentAttemptStatus> terminalStatuses() {
        return TERMINAL;
    }
}
