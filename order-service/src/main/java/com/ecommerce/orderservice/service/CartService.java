package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.exception.CartItemNotFoundException;
import com.ecommerce.orderservice.exception.CartNotFoundException;
import com.ecommerce.orderservice.repository.CartRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cart service in order-service — backed entirely by Redis.
 *
 * Carts are stored in Redis under the key "cart:{userId}".
 * TTL = 7 days sliding (reset on every write).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRedisRepository cartRedisRepository;
    private final ProductClient productClient;

    public static final int MAX_QUANTITY = 9999;

    /** Monotonic ID generator for new cart items within a request. */
    private static final AtomicLong ITEM_ID_SEQ = new AtomicLong(System.currentTimeMillis());

    // =========================================================================
    // Read
    // =========================================================================

    public CartDto getCartByUserId(Long userId) {
        return cartRedisRepository.findByUserId(userId)
                .orElse(CartRedisRepository.emptyCart(userId));
    }

    // =========================================================================
    // Write
    // =========================================================================

    public CartDto addItem(Long userId, AddItemRequest request) {
        // Fetch live product data (price snapshot into the cart)
        ProductResponse product = productClient.getProductById(request.getProductId());

        CartDto cart = cartRedisRepository.findByUserId(userId)
                .orElse(CartRedisRepository.emptyCart(userId));

        List<CartItemDto> items = new ArrayList<>(cart.getItems());

        // If the product is already in the cart -> increment quantity
        Optional<CartItemDto> existing = items.stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            CartItemDto item = existing.get();
            long newQtyLong = (long) item.getQuantity() + request.getQuantity();
            if (newQtyLong > MAX_QUANTITY) {
                throw new ApiException("Total item quantity cannot exceed " + MAX_QUANTITY, HttpStatus.BAD_REQUEST);
            }
            int newQty = (int) newQtyLong;
            item.setQuantity(newQty);
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setUnitPrice(product.getPrice());
            item.setImageUrl(product.getImageUrl());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(newQty)));
            log.debug("Incremented qty for product {} in cart for userId={}", request.getProductId(), userId);
        } else {
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            CartItemDto newItem = CartItemDto.builder()
                    .id(ITEM_ID_SEQ.incrementAndGet())
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .unitPrice(product.getPrice())
                    .quantity(request.getQuantity())
                    .subtotal(subtotal)
                    .imageUrl(product.getImageUrl())
                    .build();
            items.add(newItem);
            log.debug("Added new item productId={} to cart for userId={}", product.getId(), userId);
        }

        CartDto updated = CartRedisRepository.recalculate(
                CartDto.builder().id(cart.getId()).userId(userId).items(items).build());
        cartRedisRepository.save(updated);
        return updated;
    }

    public CartDto updateItemQuantity(Long userId, Long itemId, UpdateQuantityRequest request) {
        CartDto cart = requireCart(userId);
        List<CartItemDto> items = new ArrayList<>(cart.getItems());

        CartItemDto item = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));

        if (request.getQuantity() == 0) {
            items.remove(item);
            log.info("Removed item {} from cart for userId={}", itemId, userId);
        } else {
            if (request.getQuantity() > MAX_QUANTITY) {
                throw new ApiException("Item quantity cannot exceed " + MAX_QUANTITY, HttpStatus.BAD_REQUEST);
            }
            item.setQuantity(request.getQuantity());
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        }

        CartDto updated = CartRedisRepository.recalculate(
                CartDto.builder().id(cart.getId()).userId(userId).items(items).build());
        cartRedisRepository.save(updated);
        return updated;
    }

    public CartDto removeItem(Long userId, Long itemId) {
        CartDto cart = requireCart(userId);
        List<CartItemDto> items = new ArrayList<>(cart.getItems());

        boolean removed = items.removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new CartItemNotFoundException("Cart item not found: " + itemId);
        }

        log.info("Removed item {} from cart for userId={}", itemId, userId);
        CartDto updated = CartRedisRepository.recalculate(
                CartDto.builder().id(cart.getId()).userId(userId).items(items).build());
        cartRedisRepository.save(updated);
        return updated;
    }

    public void clearCart(Long userId) {
        cartRedisRepository.deleteByUserId(userId);
        log.info("Cleared cart for userId={}", userId);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private CartDto requireCart(Long userId) {
        return cartRedisRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("No cart found for user: " + userId));
    }
}
