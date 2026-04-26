package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.dto.CartDto;
import com.ecommerce.cartservice.dto.CartItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Redis-backed cart repository.
 *
 * Storage layout:
 *   Key:   "cart:{userId}"           (STRING → serialised CartDto JSON)
 *   TTL:   7 days (reset on every write, i.e. sliding expiry)
 *
 * Why a dedicated repository instead of Spring Data @RedisHash?
 *   - CartDto embeds a List<CartItemDto> with BigDecimal/LocalDateTime fields
 *     that need JavaTimeModule.  The GenericJackson2JsonRedisSerializer on our
 *     RedisTemplate handles this cleanly without extra @RedisHash annotations.
 *   - We keep the JPA Cart entity intact so the DB schema continues to be
 *     used for audit / analytics queries without code churn.
 *
 * Migration note: Existing Postgres carts are NOT migrated automatically.
 * On first access, CartService will find an empty cart in Redis and rebuild
 * it from the HTTP request (add-item flow).  If you want a warm migration,
 * run a one-off batch job that reads all Postgres carts and writes them here.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CartRedisRepository {

    private static final String KEY_PREFIX = "cart:";
    private static final Duration CART_TTL  = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    /** Save cart; resets sliding TTL. */
    public void save(CartDto cart) {
        String key = key(cart.getUserId());
        redisTemplate.opsForValue().set(key, cart, CART_TTL);
        log.debug("Saved cart for userId={} to Redis (TTL={})", cart.getUserId(), CART_TTL);
    }

    /** Load the cart for a user, or empty Optional if not present. */
    public Optional<CartDto> findByUserId(Long userId) {
        Object raw = redisTemplate.opsForValue().get(key(userId));
        if (raw == null) return Optional.empty();
        return Optional.of((CartDto) raw);
    }

    /** Remove a user's cart entirely (e.g. after checkout). */
    public void deleteByUserId(Long userId) {
        redisTemplate.delete(key(userId));
        log.debug("Deleted cart for userId={} from Redis", userId);
    }

    // -------------------------------------------------------------------------

    /** Build a new empty CartDto for a user who has no cart yet. */
    public static CartDto emptyCart(Long userId) {
        return CartDto.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .itemCount(0)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /** Recalculate totalPrice and itemCount from items. */
    public static CartDto recalculate(CartDto cart) {
        List<CartItemDto> items = cart.getItems();
        BigDecimal total = items.stream()
                .map(CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .totalPrice(total)
                .itemCount(items.size())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
