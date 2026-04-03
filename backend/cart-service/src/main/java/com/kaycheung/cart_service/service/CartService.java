package com.kaycheung.cart_service.service;

import com.kaycheung.cart_service.client.inventory.InventoryAvailability;
import com.kaycheung.cart_service.client.inventory.InventoryAvailabilityRequestDTO;
import com.kaycheung.cart_service.client.inventory.InventoryAvailabilityResponseDTO;
import com.kaycheung.cart_service.client.inventory.InventoryClient;
import com.kaycheung.cart_service.client.product.*;
import com.kaycheung.cart_service.domain.PersistedCartAndCartItems;
import com.kaycheung.cart_service.dto.*;
import com.kaycheung.cart_service.entity.Cart;
import com.kaycheung.cart_service.entity.CartItem;
import com.kaycheung.cart_service.exception.domain.CartItemNotFoundException;
import com.kaycheung.cart_service.exception.domain.CartNotFoundException;
import com.kaycheung.cart_service.exception.domain.UnauthorizedCartAccessException;
import com.kaycheung.cart_service.mapper.CartItemMapper;
import com.kaycheung.cart_service.mapper.RefreshCartItemMapper;
import com.kaycheung.cart_service.repository.CartItemRepository;
import com.kaycheung.cart_service.repository.CartRepository;
import com.kaycheung.cart_service.util.PriceFormatUtils;
import com.kaycheung.cart_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final CartItemMapper cartItemMapper;
    private final RefreshCartItemMapper refreshCartItemMapper;

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    private final PersistCartService persistCartService;

    public CartResponseDTO getCart() {
        //  get current userId
        UUID userId = SecurityUtils.getCurrentUserIdUUID();

        //  find cart with userId, if not found -> create new cart in db
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createNewCart(userId));

        return buildCartResponse(cart);
    }

    //  v1 - only one cart for each user, in later versions, refresh the active cart only
    public RefreshCartResponseDTO refreshCart() {
        UUID userId = SecurityUtils.getCurrentUserIdUUID();
        Cart cart = cartRepository.findByUserId(userId).orElse(null);

        if (cart == null) {
            cart = createNewCart(userId);
            return new RefreshCartResponseDTO(cart.getId(), List.of(), 0, 0L, "0", cart.getCreatedAt(), cart.getUpdatedAt(), cart.getCartVersion());
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            return new RefreshCartResponseDTO(cart.getId(), List.of(), 0, 0L, "0", cart.getCreatedAt(), cart.getUpdatedAt(), cart.getCartVersion());
        }

        Map<UUID, CartItem> cartItemsByProductId = cartItems.stream().collect(Collectors.toMap(CartItem::getProductId, item -> item, (a, b) -> a));

        //  get product base prices in batch
        ProductBasePriceRequestDTO productRequest = new ProductBasePriceRequestDTO(cartItemsByProductId.keySet().stream().toList());
        ProductBasePriceResponseDTO productResponse = productClient.getProductBasePrices(productRequest);
        Map<UUID, Long> productBasePricesByProductId = productResponse.products().stream().collect(Collectors.toMap(ProductBasePriceDTO::productId, ProductBasePriceDTO::basePrice));

        //  get inventory availabilities with available productIds only
        InventoryAvailabilityRequestDTO inventoryRequest = new InventoryAvailabilityRequestDTO(productBasePricesByProductId.keySet().stream().toList());
        InventoryAvailabilityResponseDTO inventoryResponse = inventoryClient.getAvailabilityBatch(inventoryRequest);
        Map<UUID, Integer> availabilityByProductId = inventoryResponse.inventoryAvailabilities().stream().collect(Collectors.toMap(InventoryAvailability::productId, InventoryAvailability::availableQuantity));


        List<CartItem> cartItemsToUpdate = new ArrayList<>();
        List<RefreshCartItemResponseDTO> refreshCartItems = new ArrayList<>();
        int totalQuantity = 0;
        long totalPrice = 0L;

        for (CartItem item : cartItemsByProductId.values()) {
            UUID productId = item.getProductId();
            RefreshCartItemResponseDTO refreshCartItem = refreshCartItemMapper.toDto(item);
            int availableQuantity = availabilityByProductId.getOrDefault(productId, 0);

            refreshCartItem.setAvailableQuantity(availableQuantity);

            //  if product is unavailable
            if (!productBasePricesByProductId.containsKey(productId)) {
                //  item is unavailable and no need to check stock
                refreshCartItem.setAvailable(false);
            } else {
                Long mostUpdatedBasePrice = productBasePricesByProductId.get(productId);

                //  if price has changed
                if (!item.getPriceSnapshot().equals(mostUpdatedBasePrice)) {
                    Long oldBasePrice = item.getPriceSnapshot();

                    item.setPriceSnapshot(mostUpdatedBasePrice);

                    refreshCartItem.setPriceChanged(true);
                    refreshCartItem.setPriceSnapshot(mostUpdatedBasePrice);
                    refreshCartItem.setDisplayPriceSnapshot(PriceFormatUtils.formatPriceInCentsToDisplayPrice(mostUpdatedBasePrice));
                    refreshCartItem.setOldDisplayPrice(PriceFormatUtils.formatPriceInCentsToDisplayPrice(oldBasePrice));

                    cartItemsToUpdate.add(item);
                }

                //  if not enough stock
                if (availableQuantity < item.getQuantity()) {
                    refreshCartItem.setAvailable(false);
                }
            }

            refreshCartItems.add(refreshCartItem);

            //  calculate total quantity and total price for available items only
            if (refreshCartItem.isAvailable()) {
                totalQuantity += refreshCartItem.getQuantity();
                totalPrice += refreshCartItem.getQuantity() * refreshCartItem.getPriceSnapshot();
            }
        }

        //  update cart (cartVersion) and cartItems
        if (!cartItemsToUpdate.isEmpty()) {
            PersistedCartAndCartItems persisted = persistCartService.persistCartAndCartItems(cart, cartItemsToUpdate);
            cart = persisted.cart();
        }


        return new RefreshCartResponseDTO(cart.getId(), refreshCartItems, totalQuantity, totalPrice, PriceFormatUtils.formatPriceInCentsToDisplayPrice(totalPrice), cart.getCreatedAt(), cart.getUpdatedAt(), cart.getCartVersion());
    }

    @Transactional
    public CartResponseDTO mergeGuestCart(MergeGuestCartRequestDTO request) {
        //  get current user
        UUID userId = SecurityUtils.getCurrentUserIdUUID();

        Cart cart;

        //  check if the user has an existing cart, if not->create new cart, if yes->merge cart
        Cart existingCart = cartRepository.findByUserId(userId).orElse(null);
        boolean isGuestCartEmpty = request.cartItems() == null || request.cartItems().isEmpty();

        if (existingCart == null) {
            //  user does not have existing cart
            cart = createNewCart(userId);
        } else {
            //  user has existing cart
            cart = existingCart;
        }

        //  if the cart in the request is empty + previous cart exists -> ignore request + just getCart()
        if (isGuestCartEmpty) {
            return buildCartResponse(cart);
        }

        //  group guest items by unique productId
        Map<UUID, Integer> grouped = new HashMap<>();
        for (MergeGuestCartItemRequestDTO item : request.cartItems()) {
            grouped.merge(item.productId(), item.quantity(), Integer::sum);
        }

        //  cache product
        Map<UUID, ProductResponseDTO> productCache = new HashMap<>();
        for (UUID productId : grouped.keySet()) {
            ProductResponseDTO product = productClient.getProductById(productId);
            productCache.put(productId, product);
        }

        for (UUID productId : grouped.keySet()) {
            int quantity = grouped.get(productId);
            ProductResponseDTO product = productCache.get(productId);

            //  check if cart item exists in cart
            CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);

            if (existingItem == null) {
                //  cart item does not exist -> create new cart item
                CartItem newCartItem = new CartItem();
                newCartItem.setCartId(cart.getId());
                newCartItem.setProductId(productId);
                newCartItem.setProductTitleSnapshot(product.title());
                newCartItem.setProductDescriptionSnapshot(product.description());
                newCartItem.setPriceSnapshot(product.basePrice());
                newCartItem.setQuantity(quantity);
                cartItemRepository.save(newCartItem);
            } else {
                //  update qty and product details in the existing cart item
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                existingItem.setProductTitleSnapshot(product.title());
                existingItem.setProductDescriptionSnapshot(product.description());
                existingItem.setPriceSnapshot(product.basePrice());
                cartItemRepository.save(existingItem);
            }
        }

        increaseCartVersion(cart);

        //  return cart response
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponseDTO addItem(AddCartItemRequestDTO request) {
        //  get current userId
        UUID userId = SecurityUtils.getCurrentUserIdUUID();

        //  get or create user's cart
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createNewCart(userId));

        //  fetch product info from product-service
        ProductResponseDTO product = productClient.getProductById(request.productId());

        //  check if current cart contains the same product (guardrail)
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.id()).orElse(null);

        if (cartItem == null) {
            //  cart item does not exist, create new cart item
            CartItem newCartItem = new CartItem();
            newCartItem.setCartId(cart.getId());
            newCartItem.setProductId(product.id());
            newCartItem.setQuantity(request.quantity());
            newCartItem.setProductTitleSnapshot(product.title());
            newCartItem.setProductDescriptionSnapshot(product.description());
            newCartItem.setPriceSnapshot(product.basePrice());
            cartItemRepository.save(newCartItem);
        } else {
            //  cart item exists, update quantity per request
            cartItem.setQuantity(cartItem.getQuantity() + request.quantity());
            cartItem.setProductTitleSnapshot(product.title());
            cartItem.setProductDescriptionSnapshot(product.description());
            cartItem.setPriceSnapshot(product.basePrice());
            cartItemRepository.save(cartItem);
        }

        increaseCartVersion(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponseDTO updateItemQuantity(UUID cartItemId, UpdateCartItemRequestDTO request) {
        // find cart item -> 404 return CartItemNotFoundException
        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(() -> new CartItemNotFoundException(cartItemId));

        // get cart with cartId
        Cart cart = cartRepository.findById(cartItem.getCartId()).orElseThrow(CartNotFoundException::new);

        //  check if current userId equals to the cart's userId
        UUID userId = SecurityUtils.getCurrentUserIdUUID();
        if (!userId.equals(cart.getUserId())) {
            throw new UnauthorizedCartAccessException();
        }

        //  update cart item quantity
        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);

        increaseCartVersion(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponseDTO removeCartItem(UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(() -> new CartItemNotFoundException(cartItemId));
        Cart cart = cartRepository.findById(cartItem.getCartId()).orElseThrow(CartNotFoundException::new);

        UUID userId = SecurityUtils.getCurrentUserIdUUID();
        if (!userId.equals(cart.getUserId())) {
            throw new UnauthorizedCartAccessException();
        }

        cartItemRepository.delete(cartItem);

        increaseCartVersion(cart);

        return buildCartResponse(cart);
    }

    private Cart createNewCart(UUID userId) {
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        newCart.setCartVersion(1L);
        return cartRepository.save(newCart);
    }

    private CartResponseDTO buildCartResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponseDTO> cartItemDtos = new ArrayList<>();
        int totalQuantity = 0;
        long totalPrice = 0L;

        for (CartItem item : cartItems) {
            totalPrice += item.getPriceSnapshot() * item.getQuantity();
            totalQuantity += item.getQuantity();
            cartItemDtos.add(cartItemMapper.toResponseDto(item));
        }

        return new CartResponseDTO(cart.getId(), cartItemDtos, totalQuantity, totalPrice, PriceFormatUtils.formatPriceInCentsToDisplayPrice(totalPrice), cart.getCreatedAt(), cart.getUpdatedAt(), cart.getCartVersion());
    }


//    private void refreshCartTimestamp(Cart cart) {
//        cart.setUpdatedAt(Instant.now());
//    }

    private void increaseCartVersion(Cart cart) {
        long currentCartVersion = cart.getCartVersion() == null ? 0L : cart.getCartVersion();
        cart.setCartVersion(currentCartVersion + 1);
        //  JPA dirty checking
//        cartRepository.save(cart);
    }

}
