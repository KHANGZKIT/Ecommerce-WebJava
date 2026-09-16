package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.AddItemRequest;
import com.ecommerce.cartservice.dto.CartDto;
import com.ecommerce.cartservice.dto.UpdateQuantityRequest;
import com.ecommerce.cartservice.service.CartService;
import jakarta.validation.Valid;
import com.ecommerce.cartservice.security.CartPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** Every operation uses only the identity verified by Cart authentication. */
    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @AuthenticationPrincipal CartPrincipal principal) {
        return ResponseEntity.ok(cartService.getCartByUserId(principal.userId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @AuthenticationPrincipal CartPrincipal principal,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.userId(), request));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @AuthenticationPrincipal CartPrincipal principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(principal.userId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal CartPrincipal principal,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(principal.userId(), itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal CartPrincipal principal) {
        cartService.clearCart(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
