package com.kaycheung.payment_service.controller;

import com.kaycheung.payment_service.dto.CreatePaymentRequest;
import com.kaycheung.payment_service.dto.CreatePaymentResponse;
import com.kaycheung.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/payments")
public class InternalPaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public CreatePaymentResponse createPayment(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request, userId);
    }
}
