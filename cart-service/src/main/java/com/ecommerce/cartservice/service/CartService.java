package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.client.ProductClient;
import com.ecommerce.cartservice.dto.*;
import com.ecommerce.cartservice.exception.CartItemNotFoundException;
import com.ecommerce.cartservice.repository.CartRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cart service — backed entirely by Redis.
 *
 * Architecture decision:
 *   - Carts are ephemeral, high-frequency, and small (typically < 20 items).
 *   - Redis (in-memory, O(1) key lookup) is a perfect fit; Postgres is overkill
 *     and wastes connection pool slots on hot, transient data.
 *   - TTL = 7 days sliding (reset on every write).  Abandoned carts expire
 *     automatically without a manual cleanup job.
 *
 * The JPA Cart/CartItem entities are preserved in the schema so that a
 * write-behind job can periodically flush active carts to Postgres for
 * analytics or audit.  That concern is intentionally OUT of scope here.
 *
 * Item IDs:
 *   Each CartItemDto.id is a client-visible stable identifier used for
 *   update/remove calls.  Because we no longer use Postgres sequences, we
 *   assign a simple epoch-based ID on insertion (unique within a cart's
 *   lifetime; good enough for the UI).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRedisRepository cartRedisRepository;
    private final ProductClient productClient;

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

        // If the product is already in the cart → increment quantity
        Optional<CartItemDto> existing = items.stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            CartItemDto item = existing.get();
            int newQty = item.getQuantity() + request.getQuantity();
            item.setQuantity(newQty);
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(newQty)));
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
                .orElseThrow(() -> new CartItemNotFoundException("No cart found for user: " + userId));
    }
}
