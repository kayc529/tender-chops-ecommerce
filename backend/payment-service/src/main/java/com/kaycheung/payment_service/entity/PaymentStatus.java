package com.kaycheung.payment_service.entity;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING(false),
    CAPTURED(true),
    CANCELED(true);

    private final boolean terminal;

    PaymentStatus(boolean terminal)
    {
        this.terminal = terminal;
    }


}
