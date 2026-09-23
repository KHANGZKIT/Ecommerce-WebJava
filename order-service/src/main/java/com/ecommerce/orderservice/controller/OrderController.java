package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.CheckoutRequest;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderDto;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.security.OrderPrincipal;
import com.ecommerce.orderservice.security.OrderSecurityUtils;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.orderservice.model.OrderIdempotency;
import com.ecommerce.orderservice.service.OrderIdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderIdempotencyService orderIdempotencyService;

    public OrderController(OrderService orderService) {
        this(orderService, null);
    }

    @Autowired
    public OrderController(OrderService orderService, @Autowired(required = false) OrderIdempotencyService orderIdempotencyService) {
        this.orderService = orderService;
        this.orderIdempotencyService = orderIdempotencyService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal OrderPrincipal principal) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required and must not be blank");
        }
        if (idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key header must not exceed 128 characters");
        }

        long authenticatedUserId = principal != null ? principal.userId() : OrderSecurityUtils.getAuthenticatedUserId();

        OrderIdempotency idempotencyRecord = null;
        if (orderIdempotencyService != null) {
            idempotencyRecord = orderIdempotencyService.getOrCreate(authenticatedUserId, idempotencyKey, request);
        }

        Order order = null;
        if (orderService != null) {
            order = orderService.checkout(authenticatedUserId, idempotencyKey, request, authHeader);
            if (orderIdempotencyService != null) {
                idempotencyRecord = orderIdempotencyService.find(authenticatedUserId, idempotencyKey).orElse(idempotencyRecord);
            }
        } else if (idempotencyRecord != null && idempotencyRecord.getOrder() != null) {
            order = idempotencyRecord.getOrder();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticatedUserId", authenticatedUserId);
        response.put("idempotencyKey", idempotencyKey);
        if (order != null) {
            response.put("orderId", order.getId());
            response.put("status", order.getStatus().name());
            response.put("totalAmount", order.getTotalAmount());
        } else {
            response.put("status", "CHECKOUT_BOUNDARY_ESTABLISHED");
        }

        if (idempotencyRecord != null) {
            response.put("requestFingerprint", idempotencyRecord.getRequestFingerprint());
            if (idempotencyRecord.getCartClearStatus() != null) {
                response.put("cartClearStatus", idempotencyRecord.getCartClearStatus().name());
            }
        }

        if (order != null && order.getStatus() == OrderStatus.CONFIRMED
                && idempotencyRecord != null && idempotencyRecord.getCartClearStatus() == com.ecommerce.orderservice.model.CartClearStatus.PENDING) {
            response.put("status", "CONFIRMED_CART_CLEAR_PENDING");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderPrincipal principal = OrderSecurityUtils.findAuthenticatedPrincipal();
        if (principal != null && !principal.isAdmin()) {
            request.setUserId(principal.userId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        OrderPrincipal principal = OrderSecurityUtils.getAuthenticatedPrincipal();
        OrderDto order = orderService.getOrderById(id);
        if (!principal.isAdmin() && !Long.valueOf(principal.userId()).equals(order.getUserId())) {
            throw new ApiException("Access denied to order " + id, HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderDto>> getOrdersByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        OrderPrincipal principal = OrderSecurityUtils.getAuthenticatedPrincipal();
        if (!principal.isAdmin() && !Long.valueOf(principal.userId()).equals(userId)) {
            throw new ApiException("Access denied to user orders", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long id) {
        OrderPrincipal principal = OrderSecurityUtils.getAuthenticatedPrincipal();
        if (!principal.isAdmin()) {
            OrderDto existing = orderService.getOrderById(id);
            if (!Long.valueOf(principal.userId()).equals(existing.getUserId())) {
                throw new ApiException("Access denied to cancel order " + id, HttpStatus.FORBIDDEN);
            }
        }
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        OrderPrincipal principal = OrderSecurityUtils.getAuthenticatedPrincipal();
        if (!principal.isAdmin()) {
            throw new ApiException("Admin access required to update order status", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}
