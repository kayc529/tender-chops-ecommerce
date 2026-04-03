package com.kaycheung.cart_service.service;

import com.kaycheung.cart_service.dto.InternalCartItemDTO;
import com.kaycheung.cart_service.dto.InternalCartResponseDTO;
import com.kaycheung.cart_service.entity.Cart;
import com.kaycheung.cart_service.entity.CartItem;
import com.kaycheung.cart_service.exception.domain.CartNotFoundException;
import com.kaycheung.cart_service.exception.domain.UnauthorizedCartAccessException;
import com.kaycheung.cart_service.mapper.CartItemMapper;
import com.kaycheung.cart_service.repository.CartItemRepository;
import com.kaycheung.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalCartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemMapper cartItemMapper;

    public InternalCartResponseDTO getCart(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedCartAccessException();
        }

        Cart cart = cartRepository.findByUserId(userId).orElseThrow(CartNotFoundException::new);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        List<InternalCartItemDTO> internalCartItems = cartItemMapper.toInternalResponseDTOList(cartItems);

        return new InternalCartResponseDTO(cart.getId(), cart.getCartVersion(), internalCartItems);
    }

    //  transactional in case delete fails halfway -> rollback
    @Transactional
    public void emptyCart(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedCartAccessException();
        }

        Cart cart = cartRepository.findByUserId(userId).orElseThrow(CartNotFoundException::new);
        cartItemRepository.deleteByCartId(cart.getId());
        increaseCartVersion(cart);
    }


    private void increaseCartVersion(Cart cart) {
        long currentCartVersion = cart.getCartVersion() == null ? 0L : cart.getCartVersion();
        cart.setCartVersion(currentCartVersion + 1);
    }
}
