package com.kaycheung.payment_service.controller;

import com.kaycheung.payment_service.dto.*;
import com.kaycheung.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    //  polling to see the payment results
    @GetMapping("/{payment_id}")
    public GetPaymentResponse getPayment(@PathVariable(name = "payment_id") UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    //  retry payment from payment results page when previous payment fails and user clicks "Pay again"
    @PostMapping("/{payment_id}/attempts")
    public CreatePaymentResponse retryPayment(
            @PathVariable(name = "payment_id") UUID paymentId) {
        return paymentService.retryPayment(paymentId);
    }

    //  retry payment from order page when the order status shows PAYMENT_FAILED
    //  and user clicks "Pay again" (no paymentId, orderId only)
    @PostMapping("/by-order/{order_id}/attempts")
    public CreatePaymentResponse retryPaymentByOrderId(@PathVariable(name = "order_id") UUID orderId) {
        return paymentService.retryPaymentByOrderId(orderId);
    }
}
