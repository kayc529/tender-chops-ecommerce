package com.kaycheung.cart_service.service;

import com.kaycheung.cart_service.domain.PersistedCartAndCartItems;
import com.kaycheung.cart_service.entity.Cart;
import com.kaycheung.cart_service.entity.CartItem;
import com.kaycheung.cart_service.repository.CartItemRepository;
import com.kaycheung.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersistCartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public PersistedCartAndCartItems persistCartAndCartItems(Cart cart, List<CartItem> cartItems) {
        Long currentCartVersion = cart.getCartVersion();
        cart.setCartVersion(currentCartVersion + 1L);
        Cart savedCart = cartRepository.save(cart);
        List<CartItem> savedCartItems = cartItemRepository.saveAll(cartItems);
        return new PersistedCartAndCartItems(savedCart, savedCartItems);
    }
}
