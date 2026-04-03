package com.kaycheung.order_service.controller;

import com.kaycheung.order_service.dto.OrderPageResponseDTO;
import com.kaycheung.order_service.dto.PublicOrderCreateRequestDTO;
import com.kaycheung.order_service.dto.PublicOrderCreateResponseDTO;
import com.kaycheung.order_service.dto.PublicOrderResponseDTO;
import com.kaycheung.order_service.service.PublicOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class PublicOrderController {

    private final PublicOrderService orderService;

    @GetMapping("/{orderId}")
    public PublicOrderResponseDTO getOrder(@PathVariable("orderId") UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping
    public OrderPageResponseDTO getOrders(
            @RequestParam(value = "timeFilter", required = false) String timeFilter,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.getOrders(timeFilter, start, end, pageable);
    }

    //  POST create order
    @PostMapping
    public PublicOrderCreateResponseDTO createOrder(@Valid @RequestBody PublicOrderCreateRequestDTO request) {
        return orderService.createOrder(request);
    }

    //  POST cancel order (when order is still PENDING_PAYMENT or PAID)
    @PostMapping("/{orderId}/cancel")
    public PublicOrderResponseDTO cancelOrder(@PathVariable("orderId")UUID orderId)
    {
        return orderService.cancelOrder(orderId);
    }
}
