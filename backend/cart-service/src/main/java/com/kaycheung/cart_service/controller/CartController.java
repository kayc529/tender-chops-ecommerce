package com.kaycheung.cart_service.controller;

import com.kaycheung.cart_service.dto.*;
import com.kaycheung.cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponseDTO getCart() {
        return cartService.getCart();
    }

    public RefreshCartResponseDTO refreshCart() {
        return cartService.refreshCart();
    }

    @PostMapping
    public CartResponseDTO mergeGuestCart(@Valid @RequestBody MergeGuestCartRequestDTO request) {
        return cartService.mergeGuestCart(request);
    }

    @PostMapping("/items")
    public CartResponseDTO addItem(@RequestBody @Valid AddCartItemRequestDTO request) {
        return cartService.addItem(request);
    }

    @PutMapping("/items/{cart_item_id}")
    public CartResponseDTO updateItemQuantity(@PathVariable("cart_item_id") UUID cartItemId, @Valid @RequestBody UpdateCartItemRequestDTO request) {
        return cartService.updateItemQuantity(cartItemId, request);
    }

    @DeleteMapping("/items/{cart_item_id}")
    public CartResponseDTO removeCartItem(@PathVariable("cart_item_id") UUID cartItemId) {
        return cartService.removeCartItem(cartItemId);
    }
}
