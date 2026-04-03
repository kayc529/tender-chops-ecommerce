package com.kaycheung.cart_service.controller;

import com.kaycheung.cart_service.dto.InternalCartResponseDTO;
import com.kaycheung.cart_service.service.InternalCartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/cart")
@RequiredArgsConstructor
public class InternalCartController {

    private static final Logger log = LoggerFactory.getLogger(InternalCartController.class);
    private final InternalCartService internalCartService;

    @GetMapping
    public InternalCartResponseDTO getCart(@RequestHeader(value = "X-User-Id") UUID userId) {
        return internalCartService.getCart(userId);
    }

    @PostMapping("/clear")
    public void emptyCart(@RequestHeader(value = "X-User-Id") UUID userId) {
        log.info("emptyCart");
        internalCartService.emptyCart(userId);
    }
}
