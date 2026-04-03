package com.kaycheung.order_service.controller;

import com.kaycheung.order_service.dto.internal.InternalUpdateOrderStatusDTO;
import com.kaycheung.order_service.service.InternalOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final InternalOrderService orderService;

    //  PATCH update order status
    @PatchMapping("/{orderId}/update-status")
    public void updateOrderStatus(@PathVariable("orderId") UUID orderId, @Valid @RequestBody InternalUpdateOrderStatusDTO request){
        orderService.updateOrderStatus(orderId, request);
    }
}
