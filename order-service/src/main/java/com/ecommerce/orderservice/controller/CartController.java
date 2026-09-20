package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.AddItemRequest;
import com.ecommerce.orderservice.dto.CartDto;
import com.ecommerce.orderservice.dto.UpdateQuantityRequest;
import com.ecommerce.orderservice.security.OrderPrincipal;
import com.ecommerce.orderservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** Every operation uses only the identity verified by Order authentication. */
    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @AuthenticationPrincipal OrderPrincipal principal) {
        return ResponseEntity.ok(cartService.getCartByUserId(principal.userId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @AuthenticationPrincipal OrderPrincipal principal,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.userId(), request));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @AuthenticationPrincipal OrderPrincipal principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(principal.userId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal OrderPrincipal principal,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(principal.userId(), itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal OrderPrincipal principal) {
        cartService.clearCart(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
