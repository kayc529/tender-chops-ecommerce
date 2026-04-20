package com.kaycheung.order_service.controller;

import com.kaycheung.order_service.dto.QuoteRequestDTO;
import com.kaycheung.order_service.dto.QuoteResponseDTO;
import com.kaycheung.order_service.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    //  GET quote by quoteId (for refresh/reload)
    @GetMapping("/{quoteId}")
    public QuoteResponseDTO getQuote(@PathVariable("quoteId")UUID quoteId){
        return checkoutService.getQuote(quoteId);
    }

    //  POST create quote for checkout
    @PostMapping
    public QuoteResponseDTO createQuote() {
        return checkoutService.createQuote();
    }

}
